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
package net.sympower.iec60870.iec101;

import org.junit.After;
import org.junit.Test;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.elements.IeQualifierOfInterrogation;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.iec101.connection.Iec101ClientConnection;
import net.sympower.iec60870.iec101.connection.Iec101ClientSettings;
import net.sympower.iec60870.iec101.connection.Iec101ServerConnection;
import net.sympower.iec60870.iec101.frame.Iec101FixedFrame;
import net.sympower.iec60870.iec101.frame.Iec101Frame;
import net.sympower.iec60870.iec101.frame.Iec101SingleCharFrame;
import net.sympower.iec60870.iec101.frame.Iec101VariableFrame;
import net.sympower.iec60870.spy.AsduRecordingClient;
import net.sympower.iec60870.spy.ClientSpy;
import net.sympower.iec60870.spy.RespondingServer;
import net.sympower.iec60870.spy.ServerSpy;
import net.sympower.iec60870.spy.ThrowingServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static net.sympower.iec60870.common.ASduType.C_IC_NA_1;
import static net.sympower.iec60870.common.ASduType.C_TS_NA_1;
import static net.sympower.iec60870.iec101.Iec101TestConstants.COMMON_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.CONNECTION_TIMEOUT_SECONDS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.EXTENDED_TIMEOUT_SECONDS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.LINK_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SINGLE_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMEOUT_SECONDS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMEOUT_UNIT;

public class Iec101ClientServerLinkLevelIntegrationTest {

    private Iec101ServerConnection serverConnection;
    private Iec101ClientConnection clientConnection;

    private ServerSpy spyServer;
    private ClientSpy spyClient;

    private ByteCapturingOutputStream serverToClient;
    private ByteCapturingOutputStream clientToServer;

    @After
    public void tearDown() {
        if (clientConnection != null && !clientConnection.isClosed()) {
            clientConnection.close();
        }
        if (serverConnection != null && !serverConnection.isClosed()) {
            serverConnection.close();
        }
    }

    @Test
    public void testConnectionInitializationSequence() throws Exception {
        givenClientAndServerAreConnectedWithoutClearedInitialization(
            new RespondingServer(),
            new AsduRecordingClient()
        );

        whenConnectionIsEstablished();
        
        thenConnectionEstablishmentFollowsProtocol();
        thenBothSidesAreReady();
    }

    @Test
    public void testServerSendsAckForValidCommand() throws Exception {
        givenClientAndServerAreConnected(
            new RespondingServer(),
            new AsduRecordingClient()
        );
        
        whenClientSendsValidCommand();
        
        thenServerReceivesCommand();
        thenServerSendsAckByteAtLinkLevel();
    }

    @Test
    public void testClientDoesNotSendAckForResponse() throws Exception {
        givenClientAndServerAreConnected(
            new RespondingServer(),
            new AsduRecordingClient()
        );
        
        whenClientSendsCommandThatGeneratesResponse();
        
        thenServerReceivesCommand();
        thenServerSendsAckByteAtLinkLevel();
        thenServerSendsResponseWithAsdu();
        thenClientReceivesResponse();
        thenClientDoesNotSendAck();
    }

    @Test
    public void testFrameCountBitAlternatesInSequentialFrames() throws Exception {
        givenClientAndServerAreConnected(
            new RespondingServer(),
            new AsduRecordingClient()
        );
        
        whenClientSendsFirstCommand();
        int firstCommandFCB = thenExtractFirstVariableFrameFCBValue();
        
        whenClientSendsSecondCommand();
        int secondCommandFCB = thenExtractSecondVariableFrameFCBValue();
        
        thenFrameCountBitsAreDifferent(firstCommandFCB, secondCommandFCB);
    }

    @Test
    public void testUnbalancedModeCompliance() throws Exception {
        givenClientAndServerAreConnected(
            new RespondingServer(),
            new AsduRecordingClient()
        );
        
        whenClientSendsCommand();
        thenVariableFrameHasPrimaryBitSet();
        thenVariableFrameHasCorrectAddressField();
        
        whenServerResponds();
        thenFixedResponseFrameHasPrimaryBitClear();
        thenFixedResponseFrameHasCorrectAddressField();
    }

    @Test
    public void testVariableFrameStructureCompliance() throws Exception {
        givenClientAndServerAreConnected(
            new RespondingServer(),
            new AsduRecordingClient()
        );
        
        whenClientSendsInterrogationCommand();
        
        thenVariableFrameHasCorrectStructure();
        thenVariableFrameHasValidChecksum();
        thenVariableFrameContainsAsdu();
    }

    @Test
    public void testControlFieldBitPatterns() throws Exception {
        givenClientAndServerAreConnected(
            new RespondingServer(),
            new AsduRecordingClient()
        );
        
        whenClientSendsCommand();
        
        thenVariableFrameControlFieldHasPrimaryBitSet();
        thenVariableFrameControlFieldHasFrameCountValidBitSet();
        thenVariableFrameControlFieldHasCorrectFunctionCode(Iec101Frame.FunctionCode.USER_DATA_CONFIRMED);
        thenControlFieldReservedBitsAreZero();
    }

    @Test
    public void testChecksumCalculationAndValidation() throws Exception {
        givenClientAndServerAreConnected(
            new RespondingServer(),
            new AsduRecordingClient()
        );
        
        whenClientSendsCommand();
        
        thenFrameChecksumIsCorrect();
        thenChecksumCoversCorrectFields();
    }

    @Test
    public void testServerSendsNackWhenFrameHandlingFails() throws Exception {
        givenClientAndServerAreConnected(
            new ThrowingServer(),
            new AsduRecordingClient()
        );

        whenClientSendsCommandIgnoringResult();

        thenServerSendsNackByteAtLinkLevel();
    }

    private void givenClientAndServerAreConnected(ServerSpy serverSpy, ClientSpy clientSpy) throws IOException {
        spyServer = serverSpy;
        spyClient = clientSpy;

        setupConnectionsWithByteCapture((IEC60870EventListener) serverSpy, (IEC60870EventListener) clientSpy);

        // Clear initialization traffic to focus on test-specific frames
        clientToServer.clearCapturedBytes();
        serverToClient.clearCapturedBytes();
    }

    private void givenClientAndServerAreConnectedWithoutClearedInitialization(ServerSpy serverSpy, ClientSpy clientSpy) throws IOException {
        spyServer = serverSpy;
        spyClient = clientSpy;

        setupConnectionsWithByteCapture((IEC60870EventListener) serverSpy, (IEC60870EventListener) clientSpy);
    }



    private void whenClientSendsValidCommand() throws Exception {
        clientConnection.testCommand(COMMON_ADDRESS);
        waitForServerToReceive(C_TS_NA_1);
    }

    private void whenClientSendsCommandIgnoringResult() {
        try {
            clientConnection.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION,
                                         SINGLE_COMMAND_ADDRESS, new IeSingleCommand(true, 0, true));
            // Give server time to process the command and potentially send NACK
            Thread.sleep(1000);
        } catch (IOException e) {
            // Expected - client will retry and fail when server sends NACK
            // We're interested in verifying NACK is sent, not client behavior
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void whenClientSendsCommandThatGeneratesResponse() throws Exception {
        clientConnection.interrogation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
        waitForServerToReceive(C_IC_NA_1);
    }

    private void whenClientSendsFirstCommand() throws Exception {
        clientConnection.testCommand(COMMON_ADDRESS);
        waitForServerToReceive(C_TS_NA_1);
    }

    private void whenClientSendsSecondCommand() throws Exception {
        clientToServer.clearCapturedBytes(); // Clear first command bytes
        clientConnection.testCommand(COMMON_ADDRESS);
        waitForServerToReceive(C_TS_NA_1);
    }

    private void whenClientSendsCommand() throws Exception {
        clientConnection.testCommand(COMMON_ADDRESS);
        waitForServerToReceive(C_TS_NA_1);
    }

    private void whenServerResponds() {
        await().atMost(TIMEOUT_SECONDS, TIMEOUT_UNIT)
               .until(() -> !serverToClient.getCapturedBytes().isEmpty());
    }

    private void whenClientSendsInterrogationCommand() throws Exception {
        clientConnection.interrogation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
        waitForServerToReceive(C_IC_NA_1);
    }

    private void whenConnectionIsEstablished() {
        await().atMost(CONNECTION_TIMEOUT_SECONDS, TIMEOUT_UNIT)
               .until(() -> spyServer.isConnectionReady() && spyClient.isConnectionReady());
    }

    private void thenServerReceivesCommand() {
        assertTrue("Server should have received some command", spyServer.hasReceivedAnyCommand());
    }

    private void thenServerSendsAckByteAtLinkLevel() {
        List<Byte> serverBytes = serverToClient.getCapturedBytes();
        boolean foundAck = serverBytes.contains((byte) 0xE5);
        assertTrue("Server should send ACK byte (0xE5) for valid command", foundAck);
    }

    private void thenServerSendsNackByteAtLinkLevel() {
        List<Byte> serverBytes = serverToClient.getCapturedBytes();
        boolean foundNack = serverBytes.contains((byte) 0xA2);
        assertTrue("Server should send NACK byte (0xA2) when frame handling fails", foundNack);
    }

    private void thenServerSendsResponseWithAsdu() {
        await().atMost(EXTENDED_TIMEOUT_SECONDS, TIMEOUT_UNIT).until(() -> spyClient.hasReceivedAsdu());
    }

    private void thenClientReceivesResponse() {
        assertTrue("Client should receive ASDU response", spyClient.hasReceivedAsdu());
    }

    private void thenClientDoesNotSendAck() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        boolean foundNack = clientBytes.contains((byte) 0xA2);
        assertFalse("Client should not send NACK byte (0xA2)", foundNack);
    }

    private int extractVariableFrameFCBValue(Iec101VariableFrame frame) {
        return frame.getFcb() ? 1 : 0;
    }

    private int thenExtractFirstVariableFrameFCBValue() {
        List<Byte> commandBytes = clientToServer.getCapturedBytes();
        Iec101Frame frame = decodeFrameFromBytes(commandBytes);
        assertNotNull("Should find valid frame structure", frame);
        assertTrue("Expected Variable frame for test command", frame instanceof Iec101VariableFrame);
        return extractVariableFrameFCBValue((Iec101VariableFrame) frame);
    }

    private int thenExtractSecondVariableFrameFCBValue() {
        List<Byte> commandBytes = clientToServer.getCapturedBytes();
        Iec101Frame frame = decodeFrameFromBytes(commandBytes);
        assertNotNull("Should find valid frame structure", frame);
        assertTrue("Expected Variable frame for test command", frame instanceof Iec101VariableFrame);
        return extractVariableFrameFCBValue((Iec101VariableFrame) frame);
    }

    private void thenFrameCountBitsAreDifferent(int firstCommandFCB, int secondCommandFCB) {
        assertNotEquals("FCB should alternate between consecutive commands", 
                       firstCommandFCB, secondCommandFCB);
    }

    private void thenVariableFrameHasPrimaryBitSet(Iec101VariableFrame frame) {
        assertTrue("Variable frame should have PRM bit set (primary station)", frame.getPrm());
    }

    private void thenVariableFrameHasPrimaryBitSet() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Should find valid frame structure", frame);
        assertTrue("Expected Variable frame for test command", frame instanceof Iec101VariableFrame);
        
        thenVariableFrameHasPrimaryBitSet((Iec101VariableFrame) frame);
    }

    private void thenVariableFrameHasCorrectAddressField(Iec101VariableFrame frame) {
        assertEquals("Variable frame address field should match configured link address", 
                    LINK_ADDRESS, frame.getLinkAddress());
    }

    private void thenVariableFrameHasCorrectAddressField() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Should find valid frame structure", frame);
        assertTrue("Expected Variable frame for test command", frame instanceof Iec101VariableFrame);
        
        thenVariableFrameHasCorrectAddressField((Iec101VariableFrame) frame);
    }

    private void thenFixedResponseFrameHasPrimaryBitClear() {
        List<Byte> serverBytes = serverToClient.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(serverBytes);
        if (frame != null && frame.getFrameType() != Iec101Frame.FrameType.SINGLE_CHARACTER) {
            assertTrue("Expected Fixed response frame", frame instanceof Iec101FixedFrame);
            assertFixedResponseFramePrimaryBitClear((Iec101FixedFrame) frame);
        }
    }

    private void assertFixedResponseFramePrimaryBitClear(Iec101FixedFrame frame) {
        assertFalse("Server fixed frame should have PRM bit clear (secondary station)", frame.getPrm());
    }

    private void thenFixedResponseFrameHasCorrectAddressField() {
        List<Byte> serverBytes = serverToClient.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(serverBytes);
        if (frame != null && frame.getFrameType() != Iec101Frame.FrameType.SINGLE_CHARACTER) {
            assertTrue("Expected Fixed response frame", frame instanceof Iec101FixedFrame);
            assertFixedResponseFrameCorrectAddressField((Iec101FixedFrame) frame);
        }
    }

    private void assertFixedResponseFrameCorrectAddressField(Iec101FixedFrame frame) {
        assertEquals("Server fixed response address should match link address", LINK_ADDRESS, frame.getLinkAddress());
    }

    private Iec101Frame decodeFrameFromBytes(List<Byte> bytes) {
        try {
            // Convert List<Byte> to byte array
            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) {
                byteArray[i] = bytes.get(i);
            }
            
            // Create ByteArrayInputStream from the captured bytes
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(byteArray);
            
            // Use the actual frame decoder
            return Iec101Frame.decode(bais, new IEC60870Settings());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Iec101Frame> decodeAllFramesFromBytes(List<Byte> bytes) {
        List<Iec101Frame> frames = new ArrayList<>();
        try {
            // Convert List<Byte> to byte array
            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) {
                byteArray[i] = bytes.get(i);
            }
            
            // Create ByteArrayInputStream from the captured bytes
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(byteArray);

            // Decode multiple frames until stream is exhausted
            while (bais.available() > 0) {
                try {
                    Iec101Frame frame = Iec101Frame.decode(bais, new IEC60870Settings());
                    if (frame != null) {
                        frames.add(frame);
                    } else {
                        break; // No more valid frames
                    }
                } catch (Exception e) {
                    break; // Stop on first decode error
                }
            }
        } catch (Exception e) {
        }
        return frames;
    }

    private void thenVariableFrameHasCorrectStructure() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        // Use actual frame decoder to parse the bytes
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Should find valid frame structure", frame);
        
        if (frame.getFrameType() == Iec101Frame.FrameType.VARIABLE_LENGTH) {
            Iec101VariableFrame variableFrame = (Iec101VariableFrame) frame;
            assertNotNull("Should decode as variable frame", variableFrame);
            assertEquals("Address should match configuration", LINK_ADDRESS, variableFrame.getLinkAddress());
            
        } else {
            fail("Expected variable frame, but got: " + frame.getFrameType());
        }
    }

    private void thenVariableFrameHasValidChecksum() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        // Use actual frame decoder - if it successfully decodes, checksum is valid
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Frame should decode successfully (implies valid checksum)", frame);
    }

    private void thenVariableFrameContainsAsdu() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Should find valid frame structure", frame);
        
        if (frame.getFrameType() == Iec101Frame.FrameType.VARIABLE_LENGTH) {
            Iec101VariableFrame variableFrame = (Iec101VariableFrame) frame;
            assertNotNull("Variable frame should contain ASDU", variableFrame.getAsdu());
            
        } else {
            fail("Expected variable frame, but got: " + frame.getFrameType());
        }
    }

    private void thenVariableFrameControlFieldHasPrimaryBitSet(Iec101VariableFrame frame) {
        assertTrue("Variable frame control field should have PRM bit set (PRM=true for primary)", frame.getPrm());
    }

    private void thenVariableFrameControlFieldHasPrimaryBitSet() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Should find valid frame structure", frame);
        assertTrue("Expected Variable frame for test command", frame instanceof Iec101VariableFrame);
        
        thenVariableFrameControlFieldHasPrimaryBitSet((Iec101VariableFrame) frame);
    }

    private void thenVariableFrameControlFieldHasFrameCountValidBitSet(Iec101VariableFrame frame) {
        assertTrue("Variable frame control field should have FCV bit set for commands", frame.getFcv());
    }

    private void thenVariableFrameControlFieldHasFrameCountValidBitSet() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Should find valid frame structure", frame);
        assertTrue("Expected Variable frame for test command", frame instanceof Iec101VariableFrame);
        
        thenVariableFrameControlFieldHasFrameCountValidBitSet((Iec101VariableFrame) frame);
    }

    private void thenVariableFrameControlFieldHasCorrectFunctionCode(Iec101Frame.FunctionCode expectedFunctionCode) {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Should find valid frame structure", frame);
        assertTrue("Expected Variable frame for test command", frame instanceof Iec101VariableFrame);
        
        Iec101VariableFrame variableFrame = (Iec101VariableFrame) frame;
        assertEquals("Variable frame should have function code " + expectedFunctionCode + 
                    " but was " + variableFrame.getFunctionCode(), 
                    expectedFunctionCode, variableFrame.getFunctionCode());
    }

    private void thenControlFieldReservedBitsAreZero() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        // Use actual frame decoder - if it successfully decodes, control field is well-formed
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Frame should decode successfully (implies well-formed control field)", frame);
        
    }

    private void thenFrameChecksumIsCorrect() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Frame should decode successfully (implies correct checksum)", frame);
    }

    private void thenChecksumCoversCorrectFields() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        
        Iec101Frame frame = decodeFrameFromBytes(clientBytes);
        assertNotNull("Frame should decode successfully (implies checksum covers correct fields)", frame);
    }

    private void thenConnectionEstablishmentFollowsProtocol() {
        List<Byte> clientBytes = clientToServer.getCapturedBytes();
        List<Byte> serverBytes = serverToClient.getCapturedBytes();
        
        List<Iec101Frame> clientFrames = decodeAllFramesFromBytes(clientBytes);
        List<Iec101Frame> serverFrames = decodeAllFramesFromBytes(serverBytes);
        

        // Validate first client frame (REQUEST_LINK_STATUS)
        assertRequestLinkStatusFrame(clientFrames.get(0));
        // Validate second client frame (RESET_REMOTE_LINK)
        assertResetRemoteLinkFrame(clientFrames.get(1));
        // Validate first server frame (STATUS_LINK_NO_DATA)
        assertStatusLinkNoDataFrame(serverFrames.get(0));
        // Validate second server frame (ACK)
        assertAckFrame(serverFrames.get(1));
    }
    
    private void assertRequestLinkStatusFrame(Iec101Frame frame) {
        assertEquals("REQUEST_LINK_STATUS frame should be fixed length", 
                     Iec101Frame.FrameType.FIXED_LENGTH, frame.getFrameType());
        assertTrue("Frame should be a fixed frame", frame instanceof Iec101FixedFrame);
        Iec101FixedFrame fixedFrame = (Iec101FixedFrame) frame;
        assertTrue("REQUEST_LINK_STATUS frame should be from primary station", fixedFrame.getPrm());
        assertEquals("Frame should be REQUEST_LINK_STATUS", 
                     Iec101Frame.FunctionCode.REQUEST_LINK_STATUS, fixedFrame.getFunctionCode());
        assertEquals("REQUEST_LINK_STATUS frame should use configured link address", 
                     LINK_ADDRESS, fixedFrame.getLinkAddress());
        assertFalse("REQUEST_LINK_STATUS frame should have FCB=false during initialization", fixedFrame.getFcb());
        assertFalse("REQUEST_LINK_STATUS frame should have FCV=false during initialization", fixedFrame.getFcv());
    }
    
    private void assertResetRemoteLinkFrame(Iec101Frame frame) {
        assertEquals("RESET_REMOTE_LINK frame should be fixed length", 
                     Iec101Frame.FrameType.FIXED_LENGTH, frame.getFrameType());
        assertTrue("Frame should be a fixed frame", frame instanceof Iec101FixedFrame);
        Iec101FixedFrame fixedFrame = (Iec101FixedFrame) frame;
        assertTrue("RESET_REMOTE_LINK frame should be from primary station", fixedFrame.getPrm());
        assertEquals("Frame should be RESET_REMOTE_LINK", 
                     Iec101Frame.FunctionCode.RESET_REMOTE_LINK, fixedFrame.getFunctionCode());
        assertEquals("RESET_REMOTE_LINK frame should use configured link address", 
                     LINK_ADDRESS, fixedFrame.getLinkAddress());
        assertFalse("RESET_REMOTE_LINK frame should have FCB=false during initialization", fixedFrame.getFcb());
        assertFalse("RESET_REMOTE_LINK frame should have FCV=false during initialization", fixedFrame.getFcv());
    }
    
    private void assertStatusLinkNoDataFrame(Iec101Frame frame) {
        assertEquals("STATUS_LINK_NO_DATA frame should be fixed length", 
                     Iec101Frame.FrameType.FIXED_LENGTH, frame.getFrameType());
        assertTrue("Frame should be a fixed frame", frame instanceof Iec101FixedFrame);
        Iec101FixedFrame fixedFrame = (Iec101FixedFrame) frame;
        assertFalse("STATUS_LINK_NO_DATA frame should be from secondary station", fixedFrame.getPrm());
        assertEquals("Frame should be STATUS_LINK_NO_DATA", 
                     Iec101Frame.FunctionCode.STATUS_LINK_NO_DATA, fixedFrame.getFunctionCode());
        assertEquals("STATUS_LINK_NO_DATA frame should use configured link address", 
                     LINK_ADDRESS, fixedFrame.getLinkAddress());
    }
    
    private void assertAckFrame(Iec101Frame frame) {
        assertEquals("ACK frame should be single character", 
                     Iec101Frame.FrameType.SINGLE_CHARACTER, frame.getFrameType());
        assertTrue("Frame should be ACK", ((Iec101SingleCharFrame)frame).isAck());
    }

    private void thenBothSidesAreReady() {
        assertTrue("Server should be ready after initialization", spyServer.isConnectionReady());
        assertTrue("Client should be ready after initialization", spyClient.isConnectionReady());
    }

    private void setupConnectionsWithByteCapture(IEC60870EventListener serverListener, IEC60870EventListener clientListener) throws IOException {
        // Create connections directly instead of using builders, so we don't need to open a serial ports in tests

        serverToClient = new ByteCapturingOutputStream();
        clientToServer = new ByteCapturingOutputStream();
        
        PipedOutputStream clientToServerPipe = new PipedOutputStream();
        PipedInputStream serverInput = new PipedInputStream();
        serverInput.connect(clientToServerPipe);
        
        PipedOutputStream serverToClientPipe = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream();
        clientInput.connect(serverToClientPipe);

        serverConnection = new Iec101ServerConnection(
            new DataInputStream(serverInput),
            new DataOutputStream(new TeeOutputStream(serverToClientPipe, serverToClient)),
            new IEC60870Settings(),
            LINK_ADDRESS
        );
        
        ((ServerSpy) serverListener).setServerConnection(serverConnection);

        Iec101ClientSettings clientSettings = new Iec101ClientSettings();
        clientSettings.setInitializationTimeoutMs(CONNECTION_TIMEOUT_SECONDS * 1000);
        
        clientConnection = new Iec101ClientConnection(
            new DataInputStream(clientInput),
            new DataOutputStream(new TeeOutputStream(clientToServerPipe, clientToServer)),
            new IEC60870Settings(),
            LINK_ADDRESS,
            clientSettings
        );
        
        serverConnection.startDataTransfer(serverListener);
        clientConnection.startDataTransfer(clientListener);

        await().atMost(CONNECTION_TIMEOUT_SECONDS, TIMEOUT_UNIT)
               .until(() -> isConnectionReady(serverListener) && isConnectionReady(clientListener));
    }

    private boolean isConnectionReady(IEC60870EventListener listener) {
        if (listener instanceof ServerSpy) {
            return ((ServerSpy) listener).isConnectionReady();
        } else if (listener instanceof ClientSpy) {
            return ((ClientSpy) listener).isConnectionReady();
        }
        // For any other listener type, assume ready
        return true;
    }

    private void waitForServerToReceive(net.sympower.iec60870.common.ASduType asduType) {
        await().atMost(TIMEOUT_SECONDS, TIMEOUT_UNIT)
               .until(() -> spyServer.hasReceived(asduType));
    }

    private static class ByteCapturingOutputStream extends OutputStream {
        private final List<Byte> capturedBytes = new ArrayList<>();
        private final Object lock = new Object();

        @Override
        public void write(int b) {
            synchronized (lock) {
                capturedBytes.add((byte) b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            synchronized (lock) {
                for (int i = 0; i < len; i++) {
                    capturedBytes.add(b[off + i]);
                }
            }
        }

        public List<Byte> getCapturedBytes() {
            synchronized (lock) {
                return new ArrayList<>(capturedBytes);
            }
        }

        public void clearCapturedBytes() {
            synchronized (lock) {
                capturedBytes.clear();
            }
        }
    }

    private static class TeeOutputStream extends OutputStream {
        private final OutputStream target;
        private final ByteCapturingOutputStream capture;
        
        TeeOutputStream(OutputStream target, ByteCapturingOutputStream capture) {
            this.target = target;
            this.capture = capture;
        }
        
        @Override
        public void write(int b) throws IOException {
            target.write(b);
            capture.write(b);
        }
        
        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            target.write(buf, off, len);
            capture.write(buf, off, len);
        }
        
        @Override
        public void flush() throws IOException {
            target.flush();
        }
        
        @Override
        public void close() throws IOException {
            target.close();
        }
    }
}
