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

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.common.elements.IeDoubleCommand;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQualifierOfCounterInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfSetPointCommand;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeShortFloat;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationObject;
import net.sympower.iec60870.iec101.connection.Iec101ClientConnection;
import net.sympower.iec60870.iec101.connection.Iec101ClientSettings;
import net.sympower.iec60870.iec101.connection.Iec101ServerConnection;
import net.sympower.iec60870.spy.AsduRecordingClient;
import net.sympower.iec60870.spy.Iec101RespondingServer;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static net.sympower.iec60870.iec101.Iec101TestConstants.COMMON_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.CONNECTION_TIMEOUT_SECONDS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.DOUBLE_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.LINK_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.READ_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SCALED_SET_POINT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SET_POINT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SHORT_FLOAT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SINGLE_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMEOUT_UNIT;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Iec101ClientServerIntegrationTest extends Iec101IntegrationTest {

    private Iec101RespondingServer spyIec101ServerEventListener;
    private AsduRecordingClient asduRecordingClient;

    private PipedOutputStream clientToServerPipe;
    private PipedInputStream serverInput;
    private PipedOutputStream serverToClientPipe;
    private PipedInputStream clientInput;

    
    @Test
    public void testInterrogationCommandWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsInterrogationCommand();
        thenIec101ServerReceivesCommand(ASduType.C_IC_NA_1);
        
        thenClientReceivesConfirmation(ASduType.C_IC_NA_1, CauseOfTransmission.ACTIVATION_CON);
        
        thenClientReceivesSinglePointData();
        thenClientReceivesDoublePointData();
        thenClientReceivesMeasurementData();
        
        thenClientReceivesTermination(ASduType.C_IC_NA_1, CauseOfTransmission.ACTIVATION_TERMINATION);
    }

    @Test
    public void testSingleCommandWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsSingleCommand(true);
        thenIec101ServerReceivesCommand(ASduType.C_SC_NA_1);
        
        thenClientReceivesConfirmation(ASduType.C_SC_NA_1, CauseOfTransmission.ACTIVATION_CON);
    }

    @Test
    public void testDoubleCommandWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsDoubleCommand(IeDoubleCommand.DoubleCommandState.ON);
        thenIec101ServerReceivesCommand(ASduType.C_DC_NA_1);
        
        thenClientReceivesConfirmation(ASduType.C_DC_NA_1, CauseOfTransmission.ACTIVATION_CON);
    }

    @Test
    public void testCounterInterrogationWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsCounterInterrogation();
        thenIec101ServerReceivesCommand(ASduType.C_CI_NA_1);
        
        thenClientReceivesConfirmation(ASduType.C_CI_NA_1, CauseOfTransmission.ACTIVATION_CON);
        thenClientReceivesCounterData();
        thenClientReceivesTermination(ASduType.C_CI_NA_1, CauseOfTransmission.ACTIVATION_TERMINATION);
    }

    @Test
    public void testClockSynchronizationWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsClockSynchronization();
        thenIec101ServerReceivesCommand(ASduType.C_CS_NA_1);
        
        thenClientReceivesClockSyncResponse();
    }

    @Test
    public void testReadCommandWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsReadCommand();
        thenIec101ServerReceivesCommand(ASduType.C_RD_NA_1);
        
        thenClientReceivesReadResponse();
    }

    @Test
    public void testNormalizedSetPointCommandWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsNormalizedSetPointCommand(0.75f);
        thenIec101ServerReceivesCommand(ASduType.C_SE_NA_1);
        
        thenClientReceivesConfirmation(ASduType.C_SE_NA_1, CauseOfTransmission.ACTIVATION_CON);
    }

    @Test
    public void testScaledSetPointCommandWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsScaledSetPointCommand(100);
        thenIec101ServerReceivesCommand(ASduType.C_SE_NB_1);
        
        thenClientReceivesConfirmation(ASduType.C_SE_NB_1, CauseOfTransmission.ACTIVATION_CON);
    }

    @Test
    public void testShortFloatSetPointCommandWithIec101Server() throws Exception {
        givenClientAndIec101ServerAreConnectedWithFastPolling();
        
        whenClientSendsShortFloatSetPointCommand(3.14f);
        thenIec101ServerReceivesCommand(ASduType.C_SE_NC_1);
        
        thenClientReceivesConfirmation(ASduType.C_SE_NC_1, CauseOfTransmission.ACTIVATION_CON);
    }

    private void whenClientSendsInterrogationCommand() throws IOException {
        client.interrogation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
    }
    

    private void whenClientSendsSingleCommand(boolean state) throws IOException {
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION,
                           SINGLE_COMMAND_ADDRESS, new IeSingleCommand(state, 0, false));
    }

    private void whenClientSendsDoubleCommand(IeDoubleCommand.DoubleCommandState state) throws IOException {
        client.doubleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                           DOUBLE_COMMAND_ADDRESS, new IeDoubleCommand(state, 0, false));
    }


    private void whenClientSendsClockSynchronization() throws IOException {
        client.synchronizeClocks(COMMON_ADDRESS, new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsCounterInterrogation() throws IOException {
        client.counterInterrogation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                  new IeQualifierOfCounterInterrogation(5, 0));
    }

    private void whenClientSendsReadCommand() throws IOException {
        client.read(COMMON_ADDRESS, READ_COMMAND_ADDRESS);
    }

    private void whenClientSendsNormalizedSetPointCommand(float value) throws IOException {
        client.setNormalizedValueCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                       SET_POINT_ADDRESS, new IeNormalizedValue(value), 
                                       new IeQualifierOfSetPointCommand(0, false));
    }

    private void whenClientSendsScaledSetPointCommand(int value) throws IOException {
        client.setScaledValueCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                   SCALED_SET_POINT_ADDRESS, new IeScaledValue(value), 
                                   new IeQualifierOfSetPointCommand(0, false));
    }

    private void whenClientSendsShortFloatSetPointCommand(float value) throws IOException {
        client.setShortFloatCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                  SHORT_FLOAT_ADDRESS, new IeShortFloat(value), 
                                  new IeQualifierOfSetPointCommand(0, false));
    }

    @Override
    public void tearDown() {
        super.tearDown();

        try {
            if (clientToServerPipe != null) {
                clientToServerPipe.close();
                clientToServerPipe = null;
            }
        } catch (IOException e) { /* ignore */ }
        try {
            if (serverInput != null) {
                serverInput.close();
                serverInput = null;
            }
        } catch (IOException e) { /* ignore */ }
        try {
            if (serverToClientPipe != null) {
                serverToClientPipe.close();
                serverToClientPipe = null;
            }
        } catch (IOException e) { /* ignore */ }
        try {
            if (clientInput != null) {
                clientInput.close();
                clientInput = null;
            }
        } catch (IOException e) { /* ignore */ }
    }

    private void givenClientAndIec101ServerAreConnectedWithFastPolling() throws IOException {
        // Create connections directly instead of using builders, so we don't need to open serial ports in tests
        clientToServerPipe = new PipedOutputStream();
        serverInput = new PipedInputStream();
        serverInput.connect(clientToServerPipe);
        
        serverToClientPipe = new PipedOutputStream();
        clientInput = new PipedInputStream();
        clientInput.connect(serverToClientPipe);

        spyIec101ServerEventListener = new Iec101RespondingServer();
        server = new Iec101ServerConnection(
            new DataInputStream(serverInput),
            new DataOutputStream(serverToClientPipe),
            new IEC60870Settings(),
            LINK_ADDRESS
        );

        spyIec101ServerEventListener.setServerConnection(server);
        server.startDataTransfer(spyIec101ServerEventListener);

        asduRecordingClient = new AsduRecordingClient();
        spyClientListener = asduRecordingClient;
        Iec101ClientSettings clientSettings = new Iec101ClientSettings();
        clientSettings.setPollingIntervalMs(200);
        
        client = new Iec101ClientConnection(
            new DataInputStream(clientInput),
            new DataOutputStream(clientToServerPipe),
            new IEC60870Settings(),
            LINK_ADDRESS,
            clientSettings
        );
        client.startDataTransfer(asduRecordingClient);

        await().atMost(CONNECTION_TIMEOUT_SECONDS, TIMEOUT_UNIT)
            .until(() -> spyIec101ServerEventListener.isConnectionReady() && spyClientListener.isConnectionReady());
    }
    
    private void thenIec101ServerReceivesCommand(ASduType expectedType) {
        await().atMost(CONNECTION_TIMEOUT_SECONDS, TIMEOUT_UNIT)
            .until(() -> spyIec101ServerEventListener.getReceivedAsdus().contains(expectedType));
        
        assertTrue("Server should receive " + expectedType, 
                  spyIec101ServerEventListener.getReceivedAsdus().contains(expectedType));
    }

    private ASdu awaitAndAssertAsdu(ASduType type, CauseOfTransmission cot, String description) {
        await().atMost(CONNECTION_TIMEOUT_SECONDS, TIMEOUT_UNIT)
            .until(() -> asduRecordingClient.hasReceived(type, cot));
        
        ASdu asdu = asduRecordingClient.findAsdu(type, cot).orElse(null);
        assertNotNull(description + " should be received", asdu);
        return asdu;
    }

    private ASdu awaitAndAssertAsduWithContent(ASduType type, CauseOfTransmission cot, String description) {
        ASdu asdu = awaitAndAssertAsdu(type, cot, description);
        
        InformationObject[] infoObjects = asdu.getInformationObjects();
        assertNotNull(description + " should have information objects", infoObjects);
        assertTrue(description + " should have at least one information object", infoObjects.length > 0);
        
        return asdu;
    }
    
    private void thenClientReceivesConfirmation(ASduType expectedType, CauseOfTransmission expectedCot) {
        awaitAndAssertAsdu(expectedType, expectedCot, expectedType + " confirmation");
    }
    
    private void thenClientReceivesTermination(ASduType expectedType, CauseOfTransmission expectedCot) {
        awaitAndAssertAsdu(expectedType, expectedCot, expectedType + " termination");
    }
    
    private void thenClientReceivesSinglePointData() {
        awaitAndAssertAsduWithContent(ASduType.M_SP_NA_1, CauseOfTransmission.INTERROGATED_BY_STATION, "single point data");
    }
    
    private void thenClientReceivesDoublePointData() {
        awaitAndAssertAsduWithContent(ASduType.M_DP_NA_1, CauseOfTransmission.INTERROGATED_BY_STATION, "double point data");
    }
    
    private void thenClientReceivesMeasurementData() {
        awaitAndAssertAsduWithContent(ASduType.M_ME_NB_1, CauseOfTransmission.INTERROGATED_BY_STATION, "measurement data");
    }
    
    private void thenClientReceivesCounterData() {
        awaitAndAssertAsduWithContent(ASduType.M_IT_NA_1, CauseOfTransmission.INTERROGATED_BY_STATION, "counter data");
    }
    
    private void thenClientReceivesClockSyncResponse() {
        awaitAndAssertAsduWithContent(ASduType.C_CS_NA_1, CauseOfTransmission.ACTIVATION_CON, "clock sync response");
    }
    
    private void thenClientReceivesReadResponse() {
        awaitAndAssertAsduWithContent(ASduType.M_ME_NA_1, CauseOfTransmission.REQUEST, "read response");
    }
}
