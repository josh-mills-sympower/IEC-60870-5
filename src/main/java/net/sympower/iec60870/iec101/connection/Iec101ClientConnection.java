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
package net.sympower.iec60870.iec101.connection;

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.iec101.frame.BitUtils;
import net.sympower.iec60870.iec101.frame.Iec101FixedFrame;
import net.sympower.iec60870.iec101.frame.Iec101Frame;
import net.sympower.iec60870.iec101.frame.Iec101Frame.FunctionCode;
import net.sympower.iec60870.iec101.frame.Iec101SingleCharFrame;
import net.sympower.iec60870.iec101.frame.Iec101VariableFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

public class Iec101ClientConnection extends IEC60870Connection {

    private static final Logger logger = LoggerFactory.getLogger(Iec101ClientConnection.class);
    
    public static final boolean PRIMARY_STATION = true;
    public static final boolean FCV_CLEAR = false;
    public static final boolean FCB_CLEAR = false;
    public static final boolean ACD_CLEAR = false;
    public static final boolean DFC_CLEAR = false;

    private static final int MAX_FRAME_SIZE = 255;
    private static final int BROADCAST_ADDRESS = 255;

    private final int linkAddress;
    private final Iec101ClientSettings clientSettings;
    private final ScheduledExecutorService pollingExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean pollClass1Next = new AtomicBoolean(true);
    private final AtomicBoolean acdDetected = new AtomicBoolean(false); // ACD (Access Demand) bit state
    
    private final AtomicBoolean linkLayerActive = new AtomicBoolean(false);
    private final Map<Integer, Boolean> fcbPerLink = new ConcurrentHashMap<>();
    private final Map<Integer, FrameWithAckState> pendingFrames = new ConcurrentHashMap<>();
    private final ReentrantLock sendLock = new ReentrantLock();
    
    private volatile boolean linkStatusReceived = false;
    private volatile boolean resetConfirmationReceived = false;
    private final Object handshakeLock = new Object();
    
    private static class FrameWithAckState {
        final byte[] frameData;
        final CountDownLatch ackLatch;
        final boolean fcbValue;
        volatile boolean acknowledged = false;
        final AtomicInteger retryCount = new AtomicInteger(0);
        
        FrameWithAckState(byte[] frameData, boolean fcbValue) {
            this.frameData = frameData;
            this.fcbValue = fcbValue;
            this.ackLatch = new CountDownLatch(1);
        }
    }


    public Iec101ClientConnection(DataInputStream inputStream, DataOutputStream outputStream,
        IEC60870Settings settings, int linkAddress, Iec101ClientSettings clientSettings) {
        super(inputStream, outputStream, settings);
        this.linkAddress = linkAddress;
        this.clientSettings = clientSettings;
    }

    @Override
    public void startDataTransfer(IEC60870EventListener listener) throws IOException {
        if (closed.get()) {
            throw new IOException("Connection is closed");
        }
        
        this.eventListener = listener;
        
        logger.info("Starting IEC-101 client connection initialization");
        executor.submit(this::readerTask);
        performInitialization();
    }

    @Override
    public void stopDataTransfer() throws IOException {
        if (closed.get()) {
            return;
        }
        
        sendResetRemoteLink();
        linkLayerActive.set(false);
        dataTransferStarted.set(false);
    }

    @Override
    public void send(ASdu asdu) throws IOException {
        if (closed.get()) {
            throw new IOException("Connection is closed");
        }
        
        if (!dataTransferStarted.get()) {
            throw new IOException("Data transfer not started");
        }
        
        if (!linkLayerActive.get()) {
            throw new IOException("Link layer not active");
        }
        
        sendLock.lock();
        try {
            boolean currentFcb = fcbPerLink.getOrDefault(linkAddress, false);
            boolean isBroadcast = isBroadcastAddress(linkAddress);
            
            Iec101VariableFrame frame = createVariableFrame(asdu, currentFcb, isBroadcast);
            
            if (frame.getFunctionCode() == FunctionCode.USER_DATA_CONFIRMED) {
                if (sendVariableFrameWithRetries(frame, currentFcb)) {
                    fcbPerLink.put(linkAddress, !currentFcb);
                } else {
                    throw new IOException("Failed to send frame after " + clientSettings.getMaxRetries() + " retries");
                }
            } else {
                sendVariableFrame(frame);
            }
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        
        logger.info("Closing IEC-101 client connection");
        dataTransferStarted.set(false);
        linkLayerActive.set(false);
        
        releasePendingFrames();
        executor.shutdown();
        
        pollingExecutor.shutdown();
        try {
            if (!pollingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                pollingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            pollingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        try {
            performClose();
        } catch (IOException e) {
            // Ignore close exceptions
        }
        
        if (eventListener != null) {
            eventListener.onConnectionLost(null);
        }
    }

    @Override
    protected void performClose() throws IOException {
        // Protocol-specific cleanup if needed
    }

    private void performInitialization() throws IOException {
        try {
            sendLinkStatusRequest();
            
            if (!waitForLinkStatusResponse()) {
                throw new IOException("Link status response not received within timeout");
            }
            
            sendResetRemoteLink();
            
            if (!waitForResetConfirmation()) {
                throw new IOException("Link reset confirmation not received within timeout");
            }
            
            linkLayerActive.set(true);
            dataTransferStarted.set(true);
            
            if (eventListener != null) {
                eventListener.onConnectionReady();
            }
            
            startPeriodicPolling();
            
            logger.info("IEC-101 client connection established successfully");
            
        } catch (Exception e) {
            close();
            throw new IOException("IEC-101 initialization failed: " + e.getMessage(), e);
        }
    }

    private void sendLinkStatusRequest() throws IOException {
        Iec101FixedFrame statusRequest = new Iec101FixedFrame(
            linkAddress,
            FunctionCode.REQUEST_LINK_STATUS,
            PRIMARY_STATION,
            FCV_CLEAR, // FCV is not relevant for connection initialization frames
            FCB_CLEAR, // FCB is not relevant for connection initialization frames
            ACD_CLEAR, // ACD is not set on client frames
            DFC_CLEAR // DFC is not set on client frames
        );
        
        sendFixedFrame(statusRequest);
    }
    
    private void sendResetRemoteLink() throws IOException {
        fcbPerLink.put(linkAddress, false);
        
        Iec101FixedFrame resetFrame = new Iec101FixedFrame(
            linkAddress,
            FunctionCode.RESET_REMOTE_LINK,
            PRIMARY_STATION,
            FCV_CLEAR, // FCV is not relevant for connection initialization frames
            FCB_CLEAR, // FCB is not relevant for connection initialization frames
            ACD_CLEAR, // ACD is not set on client frames
            DFC_CLEAR // DFC is not set on client frames
        );
        
        sendFixedFrame(resetFrame);
    }

    private boolean waitForLinkStatusResponse() {
        return waitForHandshakeEvent(() -> linkStatusReceived, () -> linkStatusReceived = false);
    }
    
    private boolean waitForResetConfirmation() {
        return waitForHandshakeEvent(() -> resetConfirmationReceived, () -> resetConfirmationReceived = false);
    }

    private boolean waitForHandshakeEvent(BooleanSupplier condition, Runnable resetAction) {
        synchronized (handshakeLock) {
            if (!condition.getAsBoolean()) {
                resetAction.run();
                try {
                    long startTime = System.currentTimeMillis();
                    while (!condition.getAsBoolean() && !closed.get()) {
                        handshakeLock.wait(clientSettings.getHandshakePollIntervalMs());
                        if (System.currentTimeMillis() - startTime > clientSettings.getInitializationTimeoutMs()) {
                            return false;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return condition.getAsBoolean();
        }
    }

    @Override
    protected void readerTask() {
        while (!closed.get()) {
            try {
                if (inputStream.available() > 0) {
                    Iec101Frame frame = Iec101Frame.decode(inputStream, settings);
                    handleFrame(frame);
                } else {
                    // Small sleep to avoid busy waiting
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                if (!closed.get()) {
                    logger.info("IEC-101 client connection lost: {}", e.getMessage());
                    close();
                    if (eventListener != null) {
                        eventListener.onConnectionLost(e instanceof IOException ? (IOException) e : new IOException(e));
                    }
                }
                break;
            }
        }
    }

    private void handleFrame(Iec101Frame frame) {
        logger.debug("Received {} frame", frame.getFrameType());

        switch (frame.getFrameType()) {
            case SINGLE_CHARACTER:
                handleSingleCharFrame((Iec101SingleCharFrame) frame);
                break;
            case FIXED_LENGTH:
                handleFixedFrame((Iec101FixedFrame) frame);
                break;
            case VARIABLE_LENGTH:
                handleVariableFrame((Iec101VariableFrame) frame);
                break;
        }
    }

    private void handleSingleCharFrame(Iec101SingleCharFrame frame) {
        logger.debug("Received single character: {}", frame.isAck() ? Iec101Frame.ACK : Iec101Frame.NACK);
            
        if (frame.isAck()) {
            handleAcknowledgment(true);
        } else if (frame.isNack()) {
            handleAcknowledgment(false);
        }
    }

    private void handleFixedFrame(Iec101FixedFrame frame) {
        boolean acdBit = frame.getAcd();
        acdDetected.set(acdBit);
        
        if (acdBit) {
            logger.debug("ACD bit set: Class 1 data available - triggering immediate polling");
            triggerClass1PriorityPolling();
        }
        
        switch (frame.getFunctionCode()) {
            case STATUS_LINK:
            case STATUS_LINK_ACCESS_DEMAND:
            case RESP_NACK_NO_DATA:
                notifyHandshakeEvent(() -> linkStatusReceived = true);
                break;
            case RESET_REMOTE_LINK:
                if (!frame.getPrm()) {
                    notifyHandshakeEvent(() -> resetConfirmationReceived = true);
                }
                break;
        }
    }

    private void handleVariableFrame(Iec101VariableFrame frame) {
        if (frame.getAsdu() != null && eventListener != null) {
            eventListener.onAsduReceived(frame.getAsdu());
        }
    }

    private void handleAcknowledgment(boolean positive) {
        if (positive && !linkLayerActive.get()) {
            notifyHandshakeEvent(() -> resetConfirmationReceived = true);
        }
        
        FrameWithAckState frameState = pendingFrames.get(linkAddress);
        if (frameState != null) {
            frameState.acknowledged = positive;
            frameState.ackLatch.countDown();
        }
    }


    private Iec101VariableFrame createVariableFrame(ASdu asdu, boolean currentFcb, boolean isBroadcast) {
        FunctionCode functionCode = getDataFunctionCode(!isBroadcast);
        
        return new Iec101VariableFrame(
            linkAddress, 
            functionCode,
            PRIMARY_STATION,
            !isBroadcast,
            currentFcb, 
            ACD_CLEAR, // ACD is not set on client frames
            DFC_CLEAR, // DFC is not set on client frames
            asdu
        );
    }

    private byte[] encodeVariableFrame(Iec101VariableFrame frame) {
        byte[] buffer = new byte[MAX_FRAME_SIZE];
        int length = frame.encode(buffer, settings);
        byte[] frameData = new byte[length];
        System.arraycopy(buffer, 0, frameData, 0, length);
        return frameData;
    }
    
    private byte[] encodeFixedFrame(Iec101FixedFrame frame) {
        byte[] buffer = new byte[MAX_FRAME_SIZE];
        int length = frame.encode(buffer, settings);
        byte[] frameData = new byte[length];
        System.arraycopy(buffer, 0, frameData, 0, length);
        return frameData;
    }

    private static boolean isBroadcastAddress(int linkAddress) {
        return linkAddress == BROADCAST_ADDRESS;
    }

    private static FunctionCode getDataFunctionCode(boolean requiresAck) {
        return requiresAck ? 
            FunctionCode.USER_DATA_CONFIRMED : 
            FunctionCode.USER_DATA_NO_REPLY;
    }

    private void sendRawFrame(byte[] frameData) throws IOException {
        synchronized (outputStream) {
            outputStream.write(frameData);
            outputStream.flush();
            
            try {
                applyInterFrameDelay();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Inter-frame delay interrupted", e);
            }
        }
    }

    private void applyInterFrameDelay() throws InterruptedException {
        Thread.sleep(settings.getInterFrameDelayMs());
    }

    private boolean sendVariableFrameWithRetries(Iec101VariableFrame frame, boolean fcbValue) throws IOException {
        byte[] frameData = encodeVariableFrame(frame);
        return sendRawFrameWithRetries(frameData, fcbValue);
    }
    
    private boolean sendRawFrameWithRetries(byte[] frameData, boolean fcbValue) throws IOException {
        FrameWithAckState frameState = new FrameWithAckState(frameData, fcbValue);
        pendingFrames.put(linkAddress, frameState);
        
        try {
            while (frameState.retryCount.get() <= clientSettings.getMaxRetries()) {
                sendRawFrame(frameState.frameData);
                
                try {
                    if (frameState.ackLatch.await(clientSettings.getAckTimeoutMs(), TimeUnit.MILLISECONDS)) {
                        return frameState.acknowledged;
                    }
                    frameState.retryCount.incrementAndGet();
                    logger.info("No ACK received for frame, retrying... (attempt {})", frameState.retryCount.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for ACK", e);
                }
            }
            return false;
        } finally {
            pendingFrames.remove(linkAddress);
        }
    }

    public void sendFixedFrame(Iec101FixedFrame frame) throws IOException {
        byte[] frameData = encodeFixedFrame(frame);
        logger.debug("Sending fixed frame: {}", frame.getFunctionCode());
        logger.debug("Fixed frame encoded as: {}", BitUtils.bytesToHex(frameData));
        sendRawFrame(frameData);
    }
    
    public void sendRequestClass1Data() throws IOException {
        Iec101FixedFrame requestFrame = new Iec101FixedFrame(
            linkAddress,
            FunctionCode.REQUEST_CLASS_1_DATA,
            PRIMARY_STATION,
            FCV_CLEAR, // FCV is not relevant for data request frames
            FCB_CLEAR, // FCB is not relevant for data request frames
            ACD_CLEAR, // ACD is not set on client frames
            DFC_CLEAR  // DFC is not set on client frames
        );
        sendFixedFrame(requestFrame);
    }
    
    public void sendRequestClass2Data() throws IOException {
        Iec101FixedFrame requestFrame = new Iec101FixedFrame(
            linkAddress,
            FunctionCode.REQUEST_CLASS_2_DATA,
            PRIMARY_STATION,
            FCV_CLEAR, // FCV is not relevant for data request frames
            FCB_CLEAR, // FCB is not relevant for data request frames
            ACD_CLEAR, // ACD is not set on client frames
            DFC_CLEAR  // DFC is not set on client frames
        );
        sendFixedFrame(requestFrame);
    }
    
    private void sendVariableFrame(Iec101VariableFrame frame) throws IOException {
        byte[] frameData = encodeVariableFrame(frame);
        logger.debug("Sending variable frame with ASDU: {}", frame.getAsdu());
        logger.debug("Variable frame encoded as: {}", BitUtils.bytesToHex(frameData));
        sendRawFrame(frameData);
    }

    private void releasePendingFrames() {
        for (FrameWithAckState frameState : pendingFrames.values()) {
            frameState.ackLatch.countDown();
        }
        pendingFrames.clear();
    }

    private void notifyHandshakeEvent(Runnable action) {
        synchronized (handshakeLock) {
            action.run();
            handshakeLock.notifyAll();
        }
    }
    
    private void startPeriodicPolling() {
        logger.debug("Starting alternating periodic polling every {}ms", clientSettings.getPollingIntervalMs());
        
        pollingExecutor.scheduleWithFixedDelay(() -> {
            if (!dataTransferStarted.get() || closed.get()) {
                return;
            }
            
            try {
                // If ACD bit is set, prioritize Class 1 data polling
                if (acdDetected.get()) {
                    logger.debug("ACD priority polling: requesting Class 1 data (ACD=true)");
                    sendRequestClass1Data();
                } else {
                    boolean requestClass1 = pollClass1Next.getAndSet(!pollClass1Next.get());
                    
                    if (requestClass1) {
                        logger.debug("Normal periodic polling: requesting Class 1 data");
                        sendRequestClass1Data();
                    } else {
                        logger.debug("Normal periodic polling: requesting Class 2 data");
                        sendRequestClass2Data();
                    }
                }
            } catch (Exception e) {
                logger.debug("Error during periodic polling: {}", e.getMessage());
            }
        }, 
        clientSettings.getPollingIntervalMs(),
        clientSettings.getPollingIntervalMs(),
        TimeUnit.MILLISECONDS);
    }
    
    private void triggerClass1PriorityPolling() {
        if (!dataTransferStarted.get() || closed.get()) {
            return;
        }
        
        pollingExecutor.submit(() -> {
            try {
                logger.debug("ACD immediate polling: requesting Class 1 data");
                sendRequestClass1Data();
            } catch (Exception e) {
                logger.debug("Error during ACD priority polling: {}", e.getMessage());
            }
        });
    }
}
