/*
 * Original work: Copyright 2014-20 Fraunhofer ISE (OpenMUC j60870)
 *
 * Modified work: Copyright 2025 Sympower
 *
 * This file is part of the enhanced IEC 60870 library.
 * Original project: https://github.com/openmuc/j60870
 * Enhanced version: https://github.com/josh-mills-sympower/IEC-60870-5
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.sympower.iec60870.iec104.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.common.IEC60870Protocol;
import net.sympower.iec60870.iec101.frame.BitUtils;
import net.sympower.iec60870.iec104.apdu.APdu;
import net.sympower.iec60870.internal.TimeoutManager;
import net.sympower.iec60870.internal.TimeoutTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Iec104ServerConnection extends IEC60870Connection {

    private static final Logger logger = LoggerFactory.getLogger(Iec104ServerConnection.class);
    
    private static final byte[] TESTFR_ACT = { 0x68, 0x04, 0x43, 0x00, 0x00, 0x00 };
    private static final byte[] TESTFR_CON = { 0x68, 0x04, (byte) 0x83, 0x00, 0x00, 0x00 };
    private static final byte[] STARTDT_CON = { 0x68, 0x04, 0x0b, 0x00, 0x00, 0x00 };
    private static final byte[] STOPDT_CON = { 0x68, 0x04, 0x23, 0x00, 0x00, 0x00 };

    private final Socket socket;
    private final TimeoutManager timeoutManager;
    private volatile Runnable connectionCloseListener;
    private final Iec104Settings iec104Settings;
    
    private final AtomicInteger sendSeqNum = new AtomicInteger(0);
    private final AtomicInteger receiveSeqNum = new AtomicInteger(0);
    private final ReentrantLock sendLock = new ReentrantLock();
    
    private volatile TimeoutTask testFrameTimeoutTask;
    private volatile TimeoutTask testFrameConfirmationTimeoutTask;
    private volatile boolean awaitingTestFrameConfirmation = false;
    
    private final AtomicInteger unacknowledgedIFrames = new AtomicInteger(0);
    private final AtomicInteger unacknowledgedReceivedIFrames = new AtomicInteger(0);
    private volatile TimeoutTask t1TimeoutTask;
    private volatile TimeoutTask t2TimeoutTask;
    
    private volatile long lastMessageTime = System.currentTimeMillis();

    public Iec104ServerConnection(Socket socket, IEC60870Settings settings) throws IOException {
        super(
            new DataInputStream(socket.getInputStream()),
            new DataOutputStream(socket.getOutputStream()),
            settings
        );
        this.socket = socket;
        this.timeoutManager = new TimeoutManager();
        
        if (settings instanceof Iec104ServerSettings) {
            this.iec104Settings = ((Iec104ServerSettings) settings).getIec104Settings();
        } else {
            this.iec104Settings = new Iec104Settings();
        }
        
        Thread timeoutThread = new Thread(timeoutManager, "IEC104-Server-TimeoutManager");
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    public void setConnectionCloseListener(Runnable listener) {
        this.connectionCloseListener = listener;
    }

    @Override
    public void startDataTransfer(IEC60870EventListener listener) throws IOException {
        if (closed.get()) {
            throw new IOException("Connection is closed");
        }
        
        this.eventListener = listener;
        
        executor.submit(this::readerTask);
        
        if (listener != null) {
            listener.onConnectionReady();
        }
    }

    @Override
    public void stopDataTransfer() {
        if (closed.get()) {
            return;
        }
        
        dataTransferStarted.set(false);
        cancelTestFrameTimeout();
        cancelTestFrameConfirmationTimeout();
    }

    @Override
    public void send(ASdu asdu) throws IOException {
        if (closed.get()) {
            throw new IOException("Connection is closed");
        }
        
        if (!dataTransferStarted.get()) {
            throw new IOException("Data transfer not started - waiting for STARTDT_ACT from client");
        }
        
        if (unacknowledgedIFrames.get() >= iec104Settings.getMaxNumOfOutstandingIPdus()) {
            throw new IOException("Too many unacknowledged I-frames (k=" + 
                iec104Settings.getMaxNumOfOutstandingIPdus() + ")");
        }
        
        sendLock.lock();
        try {
            int currentSendSeq = sendSeqNum.get();
            int currentReceiveSeq = receiveSeqNum.get();
            
            APdu apdu = new APdu(currentSendSeq, currentReceiveSeq, APdu.ApciType.I_FORMAT, asdu);
            byte[] buffer = new byte[255];
            int length = apdu.encode(buffer, settings);
            
            synchronized (outputStream) {
                outputStream.write(buffer, 0, length);
                outputStream.flush();
            }
            
            sendSeqNum.set((currentSendSeq + 1) % 32768);
            unacknowledgedIFrames.incrementAndGet();
            scheduleAcknowledgmentTimeout();
            updateLastMessageTime();
            
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public void sendConfirmation(ASdu originalAsdu) throws IOException {
        if (closed.get()) {
            throw new IOException("Connection is closed");
        }
        
        ASdu confirmationAsdu = IEC60870Protocol.createConfirmation(originalAsdu, 0);
        send(confirmationAsdu);
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        
        dataTransferStarted.set(false);
        cancelAllTimeouts();
        timeoutManager.cancel();
        executor.shutdown();
        
        try {
            performClose();
        } catch (IOException e) {
            // Ignore close exceptions
        }
        
        if (eventListener != null) {
            eventListener.onConnectionLost(null);
        }
        
        if (connectionCloseListener != null) {
            connectionCloseListener.run();
        }
    }

    @Override
    protected void performClose() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    protected void readerTask() {
        try {
            while (!closed.get()) {
                // APdu.decode will block until data is available
                APdu apdu = APdu.decode(inputStream, settings);
                if (apdu != null) {
                    handleApdu(apdu);
                }
            }
        } catch (IOException e) {
            if (!closed.get()) {
                close();
                if (eventListener != null) {
                    eventListener.onConnectionLost(e);
                }
            }
        } catch (Exception e) {
            if (!closed.get()) {
                close();
                if (eventListener != null) {
                    eventListener.onConnectionLost(new IOException("Connection error", e));
                }
            }
        }
    }
    
    private void handleApdu(APdu apdu) {
        updateLastMessageTime();
        cancelTestFrameTimeout();
        
        switch (apdu.getApciType()) {
            case I_FORMAT:
                handleIFormat(apdu);
                break;
            case S_FORMAT:
                handleSFormat(apdu);
                break;
            case TESTFR_ACT:
            case TESTFR_CON:
            case STARTDT_ACT:
            case STARTDT_CON:
            case STOPDT_ACT:
            case STOPDT_CON:
                handleUFormat(apdu);
                break;
        }
        
        if (dataTransferStarted.get() && !awaitingTestFrameConfirmation) {
            scheduleTestFrameTimeout();
        }
    }
    
    private void handleIFormat(APdu apdu) {
        if (!dataTransferStarted.get()) {
            return;
        }
        
        int receivedSeq = apdu.getSendSeqNumber();
        receiveSeqNum.set((receivedSeq + 1) % 32768);
        
        int ackSeq = apdu.getReceiveSeqNumber();
        updateAcknowledgment(ackSeq);
        
        unacknowledgedReceivedIFrames.incrementAndGet();
        
        if (unacknowledgedReceivedIFrames.get() >= iec104Settings.getMaxUnconfirmedIPdusReceived()) {
            sendSupervisoryFrameImmediately();
        } else {
            scheduleSupervisoryFrame();
        }
        
        ASdu asdu = apdu.getASdu();
        if (asdu != null && eventListener != null) {
            eventListener.onAsduReceived(asdu);
        }
    }
    
    private void handleSFormat(APdu apdu) {
        if (!dataTransferStarted.get()) {
            return;
        }
        
        int ackSeq = apdu.getReceiveSeqNumber();
        updateAcknowledgment(ackSeq);
    }
    
    private void handleUFormat(APdu apdu) {
        try {
            switch (apdu.getApciType()) {
                case TESTFR_ACT:
                    synchronized (outputStream) {
                        outputStream.write(TESTFR_CON);
                        outputStream.flush();
                    }
                    break;
                    
                case TESTFR_CON:
                    awaitingTestFrameConfirmation = false;
                    cancelTestFrameConfirmationTimeout();
                    break;
                    
                case STARTDT_ACT:
                    synchronized (outputStream) {
                        outputStream.write(STARTDT_CON);
                        outputStream.flush();
                    }
                    
                    if (!dataTransferStarted.get()) {
                        dataTransferStarted.set(true);
                        sendSeqNum.set(0);
                        receiveSeqNum.set(0);
                        scheduleTestFrameTimeout();
                        unacknowledgedIFrames.set(0);
                        unacknowledgedReceivedIFrames.set(0);
                    }
                    break;
                    
                case STARTDT_CON:
                case STOPDT_CON:
                    break;
                    
                case STOPDT_ACT:
                    synchronized (outputStream) {
                        outputStream.write(STOPDT_CON);
                        outputStream.flush();
                    }
                    dataTransferStarted.set(false);
                    cancelTestFrameTimeout();
                    cancelTestFrameConfirmationTimeout();
                    break;
            }
        } catch (IOException e) {
            close();
        }
    }


    private void scheduleAcknowledgmentTimeout() {
        if (t1TimeoutTask == null) {
            TimeoutTask task = new TimeoutTask(iec104Settings.getMaxTimeNoAckReceived()) {
                @Override
                protected void execute() {
                    handleAcknowledgmentTimeout();
                }
            };
            t1TimeoutTask = task;
            timeoutManager.addTimerTask(task);
        }
    }
    
    private void handleAcknowledgmentTimeout() {
        close();
    }
    
    private void scheduleSupervisoryFrame() {
        if (t2TimeoutTask == null) {
            TimeoutTask task = new TimeoutTask(iec104Settings.getMaxTimeNoAckSent()) {
                @Override
                protected void execute() {
                    sendSupervisoryFrame();
                }
            };
            t2TimeoutTask = task;
            timeoutManager.addTimerTask(task);
        }
    }
    
    private void sendSupervisoryFrame() {
        if (closed.get() || !dataTransferStarted.get()) {
            return;
        }
        
        try {
            APdu sFormatApdu = new APdu(0, receiveSeqNum.get(), APdu.ApciType.S_FORMAT, null);
            byte[] buffer = new byte[255];
            int length = sFormatApdu.encode(buffer, settings);
            
            synchronized (outputStream) {
                outputStream.write(buffer, 0, length);
                outputStream.flush();
            }
            
            unacknowledgedReceivedIFrames.set(0);
            t2TimeoutTask = null;
            updateLastMessageTime();
            
        } catch (IOException e) {
            close();
        }
    }
    
    private void sendSupervisoryFrameImmediately() {
        if (t2TimeoutTask != null) {
            t2TimeoutTask.cancel();
            t2TimeoutTask = null;
        }
        sendSupervisoryFrame();
    }
    
    private void scheduleTestFrameTimeout() {
        if (testFrameTimeoutTask != null) {
            testFrameTimeoutTask.cancel();
        }
        
        testFrameTimeoutTask = new TimeoutTask(iec104Settings.getMaxIdleTime()) {
            @Override
            protected void execute() {
                sendTestFrame();
            }
        };
        timeoutManager.addTimerTask(testFrameTimeoutTask);
    }
    
    private void sendTestFrame() {
        if (closed.get() || !dataTransferStarted.get()) {
            return;
        }
        
        try {
            synchronized (outputStream) {
                outputStream.write(TESTFR_ACT);
                outputStream.flush();
            }
            
            awaitingTestFrameConfirmation = true;
            
            testFrameConfirmationTimeoutTask = new TimeoutTask(iec104Settings.getMaxTimeNoAckReceived()) {
                @Override
                protected void execute() {
                    handleTestFrameConfirmationTimeout();
                }
            };
            timeoutManager.addTimerTask(testFrameConfirmationTimeoutTask);
            updateLastMessageTime();
            
        } catch (IOException e) {
            close();
        }
    }
    
    private void handleTestFrameConfirmationTimeout() {
        close();
    }
    
    private void updateAcknowledgment(int ackSeq) {
        int currentUnacknowledged = unacknowledgedIFrames.get();
        int acknowledgedFrames = (ackSeq - (sendSeqNum.get() - currentUnacknowledged) + 32768) % 32768;
        
        if (acknowledgedFrames > 0 && acknowledgedFrames <= currentUnacknowledged) {
            int newUnacknowledged = unacknowledgedIFrames.addAndGet(-acknowledgedFrames);
            
            if (newUnacknowledged == 0 && t1TimeoutTask != null) {
                t1TimeoutTask.cancel();
                t1TimeoutTask = null;
            }
        }
    }
    
    private void cancelAcknowledgmentTimeouts() {
        if (t1TimeoutTask != null) {
            t1TimeoutTask.cancel();
            t1TimeoutTask = null;
        }
        if (t2TimeoutTask != null) {
            t2TimeoutTask.cancel();
            t2TimeoutTask = null;
        }
    }
    
    private void cancelTestFrameTimeout() {
        if (testFrameTimeoutTask != null) {
            testFrameTimeoutTask.cancel();
            testFrameTimeoutTask = null;
        }
    }
    
    private void cancelTestFrameConfirmationTimeout() {
        if (testFrameConfirmationTimeoutTask != null) {
            testFrameConfirmationTimeoutTask.cancel();
            testFrameConfirmationTimeoutTask = null;
        }
    }
    
    private void updateLastMessageTime() {
        lastMessageTime = System.currentTimeMillis();
    }
    
    private void cancelAllTimeouts() {
        cancelTestFrameTimeout();
        cancelTestFrameConfirmationTimeout();
        cancelAcknowledgmentTimeouts();
    }
    
}
