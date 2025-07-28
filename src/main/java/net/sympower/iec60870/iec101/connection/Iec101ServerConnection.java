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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Iec101ServerConnection extends IEC60870Connection {

    private static final Logger logger = LoggerFactory.getLogger(Iec101ServerConnection.class);
    
    public static final boolean SECONDARY_STATION = false;
    public static final boolean FCV_CLEAR = false;
    public static final boolean FCB_CLEAR = false;
    public static final boolean DFC_CLEAR = false;

    private static final int MAX_FRAME_SIZE = 255;

    private final int linkAddress;
    private volatile Runnable connectionCloseListener;
    
    private final AtomicBoolean linkLayerActive = new AtomicBoolean(false);
    private final Map<Integer, Boolean> lastConfirmedFcbPerLink = new ConcurrentHashMap<>();
    private final Queue<ASdu> pendingClass1Responses = new ConcurrentLinkedQueue<>();
    private final Queue<ASdu> pendingClass2Responses = new ConcurrentLinkedQueue<>();

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
        logger.debug("Received {} frame from link {}", frame.getFunctionCode(), frame.getLinkAddress());
        
        // Check for duplicate frame ONLY if FCV=1 and this is from primary station
        if (frame.getFcv() && frame.getPrm() && isDuplicateFixedFrame(frame)) {
            logger.info("Duplicate fixed frame detected - will not process");
            return;
        }
        
        switch (frame.getFunctionCode()) {
            case RESET_REMOTE_LINK:
                handleResetRemoteLink(frame);
                break;
            case TEST_FUNCTION_LINK:
                sendRespNackNoData();
                break;
            case REQUEST_CLASS_1_DATA:
                handleClass1DataRequest();
                break;
            case REQUEST_CLASS_2_DATA:
                handleClass2DataRequest();
                break;
            case REQUEST_LINK_STATUS:
                handleLinkStatusRequest();
                break;
        }
        
        if (frame.getFcv() && frame.getPrm()) {
            updateFcbAfterConfirmation(frame.getLinkAddress(), frame.getFcb());
        }
    }
    
    private boolean isDuplicateFixedFrame(Iec101FixedFrame frame) {
        int linkAddr = frame.getLinkAddress();
        Boolean lastConfirmedFcb = lastConfirmedFcbPerLink.get(linkAddr);
        
        if (lastConfirmedFcb != null && frame.getFcb() == lastConfirmedFcb) {
            logger.debug("Fixed frame has same FCB as last confirmed frame - may be retransmission");
            return true;
        }
        return false;
    }
    
    private void updateFcbAfterConfirmation(int linkAddress, boolean fcb) {
        lastConfirmedFcbPerLink.put(linkAddress, fcb);
        logger.debug("Updated FCB tracking for link {} to {} after sending confirmation", linkAddress, fcb);
    }

    private void handleVariableFrame(Iec101VariableFrame frame) {
        logger.debug("Received variable frame from link {}, ASDU type: {}", 
                    frame.getLinkAddress(), 
                    frame.getAsdu() != null ? frame.getAsdu().getTypeIdentification() : "null");
        
        // Check for duplicate frame ONLY if Frame count valid is set, and this is from primary station
        if (frame.getFcv() && frame.getPrm() && isDuplicateVariableFrame(frame)) {
            logger.info("Duplicate variable frame detected - will not process");
            return;
        }
        
        if (frame.getAsdu() != null) {
            sendSingleCharFrame(Iec101Frame.ACK);
            handleAsdu(frame.getAsdu());
        } else {
            logger.warn("Variable frame has no ASDU");
            sendSingleCharFrame(Iec101Frame.ACK);
        }
        
        if (frame.getFcv() && frame.getPrm()) {
            updateFcbAfterConfirmation(frame.getLinkAddress(), frame.getFcb());
        }
    }
    
    private boolean isDuplicateVariableFrame(Iec101VariableFrame frame) {
        int linkAddr = frame.getLinkAddress();
        Boolean lastConfirmedFcb = lastConfirmedFcbPerLink.get(linkAddr);
        
        if (lastConfirmedFcb != null && frame.getFcb() == lastConfirmedFcb) {
            logger.debug("Variable frame has same FCB as last confirmed frame - may be retransmission");
            return true;
        }
        return false;
    }
    

    private void handleAsdu(ASdu asdu) {
        if (eventListener != null) {
            eventListener.onAsduReceived(asdu);
        }
    }

    private void handleResetRemoteLink(Iec101FixedFrame frame) {
        int remoteLinkAddress = frame.getLinkAddress();
        lastConfirmedFcbPerLink.remove(remoteLinkAddress);
        sendSingleCharFrame(Iec101Frame.ACK);
        linkLayerActive.set(true);

        logger.info("Remote link reset processed for address {} - FCB tracking cleared", remoteLinkAddress);
        
        if (eventListener != null && !dataTransferStarted.get()) {
            dataTransferStarted.set(true);
            eventListener.onConnectionReady();
        }
    }

    private void handleLinkStatusRequest() {
            sendLinkStatus();
    }
    
    private void handleClass1DataRequest() {
        ASdu pendingResponse = pendingClass1Responses.poll();
        if (pendingResponse != null) {
            logger.debug("Sending Class 1 response: {} (COT: {})", 
                        pendingResponse.getTypeIdentification(),
                        pendingResponse.getCauseOfTransmission());
            try {
                send(pendingResponse);
            } catch (IOException e) {
                logger.error("Failed to send Class 1 response: {}", e.getMessage());
                sendRespNackNoData();
            }
        } else {
            sendRespNackNoData();
        }
    }
    
    private void handleClass2DataRequest() {
        ASdu pendingResponse = pendingClass2Responses.poll();
        if (pendingResponse != null) {
            logger.debug("Sending Class 2 response: {} (COT: {})", 
                        pendingResponse.getTypeIdentification(),
                        pendingResponse.getCauseOfTransmission());
            try {
                send(pendingResponse);
            } catch (IOException e) {
                logger.error("Failed to send Class 2 response: {}", e.getMessage());
                sendRespNackNoData();
            }
        } else {
            sendRespNackNoData();
        }
    }
    
    public void queueClass1Response(ASdu response) {
        pendingClass1Responses.offer(response);
        logger.debug("Queued Class 1 response: {} (COT: {})", 
                    response.getTypeIdentification(), 
                    response.getCauseOfTransmission());
    }
    
    public void queueClass2Response(ASdu response) {
        pendingClass2Responses.offer(response);
        logger.debug("Queued Class 2 response: {} (COT: {})", 
                    response.getTypeIdentification(), 
                    response.getCauseOfTransmission());
    }


    private Iec101VariableFrame createVariableFrame(ASdu asdu) {
        return new Iec101VariableFrame(
            linkAddress,
            FunctionCode.USER_DATA_RESPONSE, // Only unbalance mode is supported, server should not receive ACKs or NACKs
            SECONDARY_STATION,
            FCV_CLEAR, // FCV is not set on server frames
            FCB_CLEAR, // FCB is not set on server frames
            determineACDBit(),
            DFC_CLEAR, // Proper Data Flow Control handling not supported
            asdu
        );
    }

    private void sendVariableFrame(Iec101VariableFrame frame) {
        byte[] frameData = encodeVariableFrame(frame);
        logger.debug("Sending variable frame with ASDU: {}", frame.getAsdu());
        sendRawFrame(frameData);
    }
    
    private byte[] encodeVariableFrame(Iec101VariableFrame frame) {
        byte[] buffer = new byte[MAX_FRAME_SIZE];
        int length = frame.encode(buffer, settings);
        byte[] frameData = new byte[length];
        System.arraycopy(buffer, 0, frameData, 0, length);
        return frameData;
    }

    private void sendRawFrame(byte[] frameData) {
        try {
            synchronized (outputStream) {
                logger.debug("Sending raw frame as bytes: {}", BitUtils.bytesToHex(frameData));
                outputStream.write(frameData);
                outputStream.flush();

                applyInterFrameDelay();
            }
        } catch (IOException | InterruptedException e) {
            close();
            if (eventListener != null && e instanceof IOException) {
                eventListener.onConnectionLost((IOException) e);
            }
        }
    }

    private void sendSingleCharFrame(byte character) {
        logger.debug("Sending single character: {}", character);
            
        try {
            synchronized (outputStream) {
                outputStream.write(character);
                outputStream.flush();

                applyInterFrameDelay();
            }
        } catch (IOException | InterruptedException e) {
            close();
            if (eventListener != null) {
                eventListener.onConnectionLost((IOException) e);
            }
        }
    }

    private void sendLinkStatus() {
        boolean acdBit = determineACDBit();
        FunctionCode statusCode = acdBit ? FunctionCode.STATUS_LINK_ACCESS_DEMAND : FunctionCode.STATUS_LINK;
            
        Iec101FixedFrame statusFrame = new Iec101FixedFrame(
            linkAddress,
            statusCode,
            SECONDARY_STATION,
            FCV_CLEAR, // FCV is not set on server frames
            FCB_CLEAR, // FCB is not set on server frames
            acdBit,
            DFC_CLEAR // Proper Data Flow Control handling not supported
        );
        
        logger.debug("Sending link status with ACD={} (Class 1 data: {})", acdBit, acdBit ? "available" : "empty");
        sendFixedFrame(statusFrame);
    }

    private void sendRespNackNoData() {
        boolean acdBit = determineACDBit();
        
        Iec101FixedFrame statusFrame = new Iec101FixedFrame(
                linkAddress,
                FunctionCode.RESP_NACK_NO_DATA,
                SECONDARY_STATION,
                FCV_CLEAR, // FCV is not set on server frames
                FCB_CLEAR, // FCB is not set on server frames
                acdBit,
                DFC_CLEAR // Proper Data Flow Control handling not supported
        );
        
        logger.debug("Sending RESP_NACK_NO_DATA with ACD={} (Class 1 data: {})", acdBit, acdBit ? "available" : "empty");
        sendFixedFrame(statusFrame);
    }

    private void sendFixedFrame(Iec101FixedFrame frame) {
        byte[] frameData = encodeFixedFrame(frame);
        logger.debug("Sending fixed frame: {}", frame.getFunctionCode());
        sendRawFrame(frameData);
    }

    private void applyInterFrameDelay() throws InterruptedException {
        Thread.sleep(settings.getInterFrameDelayMs());
    }
    
    private byte[] encodeFixedFrame(Iec101FixedFrame frame) {
        byte[] buffer = new byte[MAX_FRAME_SIZE];
        int length = frame.encode(buffer, settings);
        byte[] frameData = new byte[length];
        System.arraycopy(buffer, 0, frameData, 0, length);
        return frameData;
    }
    
    private boolean determineACDBit() {
        return !pendingClass1Responses.isEmpty();
    }

}

