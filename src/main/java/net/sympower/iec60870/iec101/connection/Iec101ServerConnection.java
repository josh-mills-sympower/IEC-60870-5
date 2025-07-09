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
import net.sympower.iec60870.iec101.frame.Iec101VariableFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Iec101ServerConnection extends IEC60870Connection {

    private static final Logger logger = LoggerFactory.getLogger(Iec101ServerConnection.class);
    
    public static final boolean SECONDARY_STATION = false;
    public static final boolean FCV_CLEAR = false;
    public static final boolean FCB_CLEAR = false;
    public static final boolean ACD_CLEAR = false;
    public static final boolean DFC_CLEAR = false;

    private static final int MAX_FRAME_SIZE = 255;

    private final int linkAddress;
    private volatile Runnable connectionCloseListener;
    
    private final AtomicBoolean linkLayerActive = new AtomicBoolean(false);
    private final Map<Integer, Boolean> expectedFcbPerLink = new ConcurrentHashMap<>();

    public Iec101ServerConnection(
        DataInputStream inputStream, DataOutputStream outputStream,
        IEC60870Settings settings, int linkAddress
    ) {
        super(inputStream, outputStream, settings);
        this.linkAddress = linkAddress;
    }

    @Override
    public void startDataTransfer(IEC60870EventListener listener) throws IOException {
        if (closed.get()) {
            throw new IOException("Connection is closed");
        }
        
        this.eventListener = listener;
        
        executor.submit(this::readerTask);
        linkLayerActive.set(true);
        dataTransferStarted.set(true);
        
        if (listener != null) {
            listener.onConnectionReady();
        }
        
        logger.info("IEC-101 server connection ready for data transfer");
    }

    @Override
    public void stopDataTransfer() {
        if (closed.get()) {
            return;
        }
        
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
        
        Iec101VariableFrame frame = createVariableFrame(asdu);
        sendVariableFrame(frame);
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        
        logger.info("Closing IEC-101 server connection");
        dataTransferStarted.set(false);
        linkLayerActive.set(false);
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

    public void setConnectionCloseListener(Runnable listener) {
        this.connectionCloseListener = listener;
    }

    @Override
    protected void performClose() throws IOException {
        // Protocol-specific cleanup if needed
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
            }
            catch (Exception e) {
                if (!closed.get()) {
                    logger.info("IEC-101 server connection lost: {}", e.getMessage());
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
            
        try {
            switch (frame.getFrameType()) {
                case SINGLE_CHARACTER: // Only unbalance mode is supported, server should not receive ACKs or NACKs
                    break;
                case FIXED_LENGTH:
                    handleFixedFrame((Iec101FixedFrame) frame);
                    break;
                case VARIABLE_LENGTH:
                    handleVariableFrame((Iec101VariableFrame) frame);
                    break;
            }
        } catch (Exception e) {
            sendSingleCharFrame(Iec101Frame.NACK);
        }
    }

    private void handleFixedFrame(Iec101FixedFrame frame) {
        switch (frame.getFunctionCode()) {
            case RESET_REMOTE_LINK:
                handleResetRemoteLink(frame);
                break;
            case TEST_FUNCTION_LINK:
            case REQUEST_CLASS_1_DATA: // Proper REQUEST_CLASS_DATA handling not supported.
            case REQUEST_CLASS_2_DATA:
                sendSingleCharFrame(Iec101Frame.ACK);
                break;
            case REQUEST_LINK_STATUS:
                handleLinkStatusRequest();
                break;
        }
    }

    private void handleVariableFrame(Iec101VariableFrame frame) {
        if (frame.getFcv() && isDuplicateFrame(frame)) {
            sendSingleCharFrame(Iec101Frame.ACK);
            return;
        }
        
        if (frame.getAsdu() != null) {
            sendSingleCharFrame(Iec101Frame.ACK);
            handleAsdu(frame.getAsdu());
        }
    }

    private boolean isDuplicateFrame(Iec101VariableFrame frame) {
        int remoteLinkAddress = frame.getLinkAddress();
        Boolean lastReceivedFcb = expectedFcbPerLink.get(remoteLinkAddress);
        
        if (lastReceivedFcb != null && frame.getFcb() == lastReceivedFcb) {
            return true;
        }
        
        expectedFcbPerLink.put(remoteLinkAddress, frame.getFcb());
        return false;
    }

    private void handleAsdu(ASdu asdu) {
        logger.debug("Received ASDU: {}", asdu);
        if (eventListener != null) {
            eventListener.onAsduReceived(asdu);
        }
    }

    private void handleResetRemoteLink(Iec101FixedFrame frame) {
        int remoteLinkAddress = frame.getLinkAddress();
        expectedFcbPerLink.remove(remoteLinkAddress);
        sendSingleCharFrame(Iec101Frame.ACK);
        linkLayerActive.set(true);
        
        logger.info("Remote link reset processed for address {}", remoteLinkAddress);
        
        if (eventListener != null && !dataTransferStarted.get()) {
            dataTransferStarted.set(true);
            eventListener.onConnectionReady();
        }
    }

    private void handleLinkStatusRequest() {
        try {
            sendLinkStatus();
        } catch (IOException e) {
            close();
            if (eventListener != null) {
                eventListener.onConnectionLost(e);
            }
        }
    }


    private Iec101VariableFrame createVariableFrame(ASdu asdu) {
        return new Iec101VariableFrame(
            linkAddress,
            FunctionCode.USER_DATA_NO_REPLY, // Only unbalance mode is supported, server should not receive ACKs or NACKs
            SECONDARY_STATION,
            FCV_CLEAR, // FCV is not set on server frames
            FCB_CLEAR, // FCB is not set on server frames
            ACD_CLEAR, // Proper Access Demand handling not supported
            DFC_CLEAR, // Proper Data Flow Control handling not supported
            asdu
        );
    }

    private void sendVariableFrame(Iec101VariableFrame frame) throws IOException {
        byte[] frameData = encodeVariableFrame(frame);
        logger.debug("Sending variable frame with ASDU: {}", frame.getAsdu());
        logger.debug("Variable frame encoded as: {}", BitUtils.bytesToHex(frameData));
        sendRawFrame(frameData);
    }
    
    private byte[] encodeVariableFrame(Iec101VariableFrame frame) {
        byte[] buffer = new byte[MAX_FRAME_SIZE];
        int length = frame.encode(buffer, settings);
        byte[] frameData = new byte[length];
        System.arraycopy(buffer, 0, frameData, 0, length);
        return frameData;
    }

    private void sendRawFrame(byte[] frameData) throws IOException {
        synchronized (outputStream) {
            outputStream.write(frameData);
            outputStream.flush();
        }
    }

    private void sendSingleCharFrame(byte character) {
        logger.debug("Sending single character: {}", character);
            
        try {
            synchronized (outputStream) {
                outputStream.write(character);
                outputStream.flush();
            }
        } catch (IOException e) {
            close();
            if (eventListener != null) {
                eventListener.onConnectionLost(e);
            }
        }
    }

    private void sendLinkStatus() throws IOException {
        FunctionCode statusCode =
            ACD_CLEAR ? FunctionCode.STATUS_LINK_ACCESS_DEMAND : FunctionCode.STATUS_LINK_NO_DATA; // Proper Access Demand not supported
            
        Iec101FixedFrame statusFrame = new Iec101FixedFrame(
            linkAddress,
            statusCode,
            SECONDARY_STATION,
            FCV_CLEAR, // FCV is not set on server frames
            FCB_CLEAR, // FCB is not set on server frames
            ACD_CLEAR, // Proper Access Demand handling not supported
            DFC_CLEAR // Proper Data Flow Control handling not supported
        );
        
        sendFixedFrame(statusFrame);
    }

    private void sendFixedFrame(Iec101FixedFrame frame) throws IOException {
        byte[] frameData = encodeFixedFrame(frame);
        logger.debug("Sending fixed frame: {}", frame.getFunctionCode());
        logger.debug("Fixed frame encoded as: {}", BitUtils.bytesToHex(frameData));
        sendRawFrame(frameData);
    }
    
    private byte[] encodeFixedFrame(Iec101FixedFrame frame) {
        byte[] buffer = new byte[MAX_FRAME_SIZE];
        int length = frame.encode(buffer, settings);
        byte[] frameData = new byte[length];
        System.arraycopy(buffer, 0, frameData, 0, length);
        return frameData;
    }

}

