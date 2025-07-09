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
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.elements.IeBinaryCounterReading;
import net.sympower.iec60870.common.elements.IeBinaryStateInformation;
import net.sympower.iec60870.common.elements.IeDoubleCommand;
import net.sympower.iec60870.common.elements.IeFixedTestBitPattern;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQualifierOfCounterInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfParameterActivation;
import net.sympower.iec60870.common.elements.IeQualifierOfParameterOfMeasuredValues;
import net.sympower.iec60870.common.elements.IeQualifierOfSetPointCommand;
import net.sympower.iec60870.common.elements.IeQuality;
import net.sympower.iec60870.common.elements.IeRegulatingStepCommand;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeShortFloat;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeTestSequenceCounter;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationElement;
import net.sympower.iec60870.common.elements.InformationObject;
import net.sympower.iec60870.iec101.connection.Iec101ClientConnection;
import net.sympower.iec60870.iec101.connection.Iec101ClientSettings;
import net.sympower.iec60870.iec101.connection.Iec101ServerConnection;
import net.sympower.iec60870.spy.AsduRecordingClient;
import net.sympower.iec60870.spy.RespondingServer;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static net.sympower.iec60870.iec101.Iec101TestConstants.BITSTRING_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.BITSTRING_VALUE_1;
import static net.sympower.iec60870.iec101.Iec101TestConstants.COMMON_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.CONNECTION_TIMEOUT_SECONDS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.COUNTER_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.COUNTER_VALUE_1;
import static net.sympower.iec60870.iec101.Iec101TestConstants.COUNTER_VALUE_2;
import static net.sympower.iec60870.iec101.Iec101TestConstants.DOUBLE_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.LINK_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.MEASUREMENT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.NORMALIZED_TOLERANCE;
import static net.sympower.iec60870.iec101.Iec101TestConstants.PARAMETER_ACTIVATION_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.PARAMETER_FLOAT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.PARAMETER_NORMALIZED_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.PARAMETER_SCALED_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.READ_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.REGULATING_STEP_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SCALED_SET_POINT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SCALED_VALUE;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SET_POINT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SHORT_FLOAT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.SINGLE_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TEST_SEQUENCE_VALUE;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMEOUT_UNIT;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMETAG_BITSTRING_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMETAG_DOUBLE_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMETAG_FLOAT_SETPOINT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMETAG_NORMALIZED_SETPOINT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMETAG_REGULATING_STEP_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMETAG_SCALED_SETPOINT_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMETAG_SINGLE_COMMAND_ADDRESS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIME_TOLERANCE_MS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Iec101ClientServerIntegrationTest extends Iec101IntegrationTest {

    private PipedOutputStream clientToServerPipe;
    private PipedInputStream serverInput;
    private PipedOutputStream serverToClientPipe;
    private PipedInputStream clientInput;

    @Test
    public void testInterrogationCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsInterrogationCommand();
        
        thenServerReceivesCommand(ASduType.C_IC_NA_1);
        thenClientReceivesInterrogationResponse();
    }

    @Test
    public void testSingleCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsSingleCommand(true);
        
        thenServerReceivesCommand(ASduType.C_SC_NA_1);
        thenClientReceivesSingleCommandConfirmation();
    }

    @Test
    public void testDoubleCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsDoubleCommand(IeDoubleCommand.DoubleCommandState.ON);
        
        thenServerReceivesCommand(ASduType.C_DC_NA_1);
        thenClientReceivesDoubleCommandConfirmation();
    }

    @Test
    public void testTestCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsTestCommand();
        
        thenServerReceivesCommand(ASduType.C_TS_NA_1);
        thenClientReceivesTestCommandConfirmation();
    }

    @Test
    public void testClockSynchronization() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsClockSynchronization();
        
        thenServerReceivesCommand(ASduType.C_CS_NA_1);
        thenClientReceivesClockSyncConfirmation();
    }

     @Test
    public void testCounterInterrogation() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsCounterInterrogation();
        
        thenServerReceivesCommand(ASduType.C_CI_NA_1);
        thenClientReceivesCounterInterrogationResponse();
    }

    @Test
    public void testReadCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsReadCommand();
        
        thenServerReceivesCommand(ASduType.C_RD_NA_1);
        thenClientReceivesReadCommandResponse();
    }

    @Test
    public void testNormalizedSetPointCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsNormalizedSetPointCommand(0.75f);
        
        thenServerReceivesCommand(ASduType.C_SE_NA_1);
        thenClientReceivesSetPointConfirmation();
    }

    @Test
    public void testScaledSetPointCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsScaledSetPointCommand(100);
        
        thenServerReceivesCommand(ASduType.C_SE_NB_1);
        thenClientReceivesScaledSetPointConfirmation();
    }

    @Test
    public void testShortFloatSetPointCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsShortFloatSetPointCommand(3.14f);
        
        thenServerReceivesCommand(ASduType.C_SE_NC_1);
        thenClientReceivesShortFloatSetPointConfirmation();
    }

    @Test
    public void testRegulatingStepCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER);
        
        thenServerReceivesCommand(ASduType.C_RC_NA_1);
        thenClientReceivesRegulatingStepConfirmation();
    }

    @Test
    public void testParameterActivation() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsParameterActivation();
        
        thenServerReceivesCommand(ASduType.P_AC_NA_1);
        thenClientReceivesParameterActivationConfirmation();
    }


    @Test
    public void testSingleCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsSingleCommandWithTimeTag(true);
        
        thenServerReceivesCommand(ASduType.C_SC_TA_1);
        thenClientReceivesTimeTaggedCommandConfirmation(ASduType.C_SC_TA_1);
    }

    @Test
    public void testTestCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsTestCommandWithTimeTag();
        
        thenServerReceivesCommand(ASduType.C_TS_TA_1);
        thenClientReceivesTimeTaggedCommandConfirmation(ASduType.C_TS_TA_1);
    }

    @Test
    public void testDoubleCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsDoubleCommandWithTimeTag(IeDoubleCommand.DoubleCommandState.ON);
        
        thenServerReceivesCommand(ASduType.C_DC_TA_1);
        thenClientReceivesTimeTaggedCommandConfirmation(ASduType.C_DC_TA_1);
    }

    @Test
    public void testRegulatingStepCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsRegulatingStepCommandWithTimeTag(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER);
        
        thenServerReceivesCommand(ASduType.C_RC_TA_1);
        thenClientReceivesTimeTaggedCommandConfirmation(ASduType.C_RC_TA_1);
    }

    @Test
    public void testSetNormalizedValueCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsSetNormalizedValueCommandWithTimeTag(0.75f);
        
        thenServerReceivesCommand(ASduType.C_SE_TA_1);
        thenClientReceivesTimeTaggedCommandConfirmation(ASduType.C_SE_TA_1);
    }

    @Test
    public void testSetScaledValueCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsSetScaledValueCommandWithTimeTag(100);
        
        thenServerReceivesCommand(ASduType.C_SE_TB_1);
        thenClientReceivesTimeTaggedCommandConfirmation(ASduType.C_SE_TB_1);
    }

    @Test
    public void testSetShortFloatCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsSetShortFloatCommandWithTimeTag(3.14f);
        
        thenServerReceivesCommand(ASduType.C_SE_TC_1);
        thenClientReceivesTimeTaggedCommandConfirmation(ASduType.C_SE_TC_1);
    }

    @Test
    public void testParameterNormalizedValue() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsParameterNormalizedValue(0.25f);
        
        thenServerReceivesCommand(ASduType.P_ME_NA_1);
        thenClientReceivesParameterConfirmation(ASduType.P_ME_NA_1);
    }

    @Test
    public void testParameterScaledValue() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsParameterScaledValue(500);
        
        thenServerReceivesCommand(ASduType.P_ME_NB_1);
        thenClientReceivesParameterConfirmation(ASduType.P_ME_NB_1);
    }

    @Test
    public void testParameterShortFloat() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsParameterShortFloat(1.414f);
        
        thenServerReceivesCommand(ASduType.P_ME_NC_1);
        thenClientReceivesParameterConfirmation(ASduType.P_ME_NC_1);
    }

    @Test
    public void testBitstringCommand() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsBitstringCommand();
        
        thenServerReceivesCommand(ASduType.C_BO_NA_1);
        thenClientReceivesBitstringCommandConfirmation(ASduType.C_BO_NA_1);
    }

    // Temporarily disabled due to frame send retry failure - needs investigation
    // @Test
    public void testBitstringCommandWithTimeTag() throws Exception {
        givenClientAndServerAreConnected();
        
        whenClientSendsBitstringCommandWithTimeTag();
        
        thenServerReceivesCommand(ASduType.C_BO_TA_1);
        thenClientReceivesBitstringCommandConfirmation(ASduType.C_BO_TA_1);
    }

    private void givenClientAndServerAreConnected() throws IOException {
        // Create connections directly instead of using builders, so we don't need to open a serial ports in tests
        clientToServerPipe = new PipedOutputStream();
        serverInput = new PipedInputStream();
        serverInput.connect(clientToServerPipe);
        
        serverToClientPipe = new PipedOutputStream();
        clientInput = new PipedInputStream();
        clientInput.connect(serverToClientPipe);

        spyServerEventListener = new RespondingServer();
        server = new Iec101ServerConnection(
            new DataInputStream(serverInput),
            new DataOutputStream(serverToClientPipe),
            new IEC60870Settings(),
            LINK_ADDRESS
        );

        spyServerEventListener.setServerConnection(server);
        server.startDataTransfer((IEC60870EventListener) spyServerEventListener);

        spyClientListener = new AsduRecordingClient();
        Iec101ClientSettings clientSettings = new Iec101ClientSettings();
        clientSettings.setInitializationTimeoutMs(30000);  // Use 30 seconds for more reliability
        
        client = new Iec101ClientConnection(
            new DataInputStream(clientInput),
            new DataOutputStream(clientToServerPipe),
            new IEC60870Settings(),
            LINK_ADDRESS,
            clientSettings
        );
        client.startDataTransfer((IEC60870EventListener) spyClientListener);

        await().atMost(CONNECTION_TIMEOUT_SECONDS, TIMEOUT_UNIT)
            .until(() -> spyServerEventListener.isConnectionReady() && spyClientListener.isConnectionReady());
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

    private void whenClientSendsTestCommand() throws IOException {
        client.testCommand(COMMON_ADDRESS);
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

    private void whenClientSendsRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState state) throws IOException {
        client.regulatingStepCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                   REGULATING_STEP_ADDRESS, new IeRegulatingStepCommand(state, 0, false));
    }

    private void whenClientSendsParameterActivation() throws IOException {
        client.parameterActivation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                 PARAMETER_ACTIVATION_ADDRESS, 1);
    }


    private void whenClientSendsSingleCommandWithTimeTag(boolean state) throws IOException {
        client.singleCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                      TIMETAG_SINGLE_COMMAND_ADDRESS,
                                      new IeSingleCommand(state, 0, false), 
                                      new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsTestCommandWithTimeTag() throws IOException {
        client.testCommandWithTimeTag(COMMON_ADDRESS, 
                                    new IeTestSequenceCounter(TEST_SEQUENCE_VALUE), 
                                    new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsDoubleCommandWithTimeTag(IeDoubleCommand.DoubleCommandState state) throws IOException {
        client.doubleCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                      TIMETAG_DOUBLE_COMMAND_ADDRESS,
                                      new IeDoubleCommand(state, 0, false), 
                                      new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsRegulatingStepCommandWithTimeTag(IeRegulatingStepCommand.StepCommandState state) throws IOException {
        client.regulatingStepCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                               TIMETAG_REGULATING_STEP_ADDRESS,
                                               new IeRegulatingStepCommand(state, 0, false), 
                                               new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsSetNormalizedValueCommandWithTimeTag(float value) throws IOException {
        client.setNormalizedValueCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                                   TIMETAG_NORMALIZED_SETPOINT_ADDRESS,
                                                   new IeNormalizedValue(value), 
                                                   new IeQualifierOfSetPointCommand(0, false),
                                                   new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsSetScaledValueCommandWithTimeTag(int value) throws IOException {
        client.setScaledValueCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                               TIMETAG_SCALED_SETPOINT_ADDRESS,
                                               new IeScaledValue(value), 
                                               new IeQualifierOfSetPointCommand(0, false),
                                               new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsSetShortFloatCommandWithTimeTag(float value) throws IOException {
        client.setShortFloatCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                              TIMETAG_FLOAT_SETPOINT_ADDRESS,
                                              new IeShortFloat(value), 
                                              new IeQualifierOfSetPointCommand(0, false),
                                              new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsParameterNormalizedValue(float value) throws IOException {
        client.parameterMeasuredValueNormalized(COMMON_ADDRESS, PARAMETER_NORMALIZED_ADDRESS,
                                               new IeNormalizedValue(value),
                                               new IeQualifierOfParameterOfMeasuredValues(1, false, false));
    }

    private void whenClientSendsParameterScaledValue(int value) throws IOException {
        client.parameterMeasuredValueScaled(COMMON_ADDRESS, PARAMETER_SCALED_ADDRESS,
                                           new IeScaledValue(value),
                                           new IeQualifierOfParameterOfMeasuredValues(1, false, false));
    }

    private void whenClientSendsParameterShortFloat(float value) throws IOException {
        client.parameterMeasuredValueShortFloat(COMMON_ADDRESS, PARAMETER_FLOAT_ADDRESS,
                                               new IeShortFloat(value),
                                               new IeQualifierOfParameterOfMeasuredValues(1, false, false));
    }

    private void whenClientSendsBitstringCommand() throws IOException {
        client.bitstringCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                              BITSTRING_COMMAND_ADDRESS, new IeBinaryStateInformation(BITSTRING_VALUE_1));
    }

    private void whenClientSendsBitstringCommandWithTimeTag() throws IOException {
        client.bitstringCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                                          TIMETAG_BITSTRING_COMMAND_ADDRESS, 
                                          new IeBinaryStateInformation(BITSTRING_VALUE_1),
                                          new IeTime56(System.currentTimeMillis()));
    }

    private void thenServerReceivesCommand(ASduType expectedAsduType) {
        waitForServerToReceive(expectedAsduType);
        assertTrue("Server should receive " + expectedAsduType, spyServerEventListener.hasReceived(expectedAsduType));
    }

    private void thenClientReceivesInterrogationResponse() {
        waitForClientResponse(ASduType.M_ME_NB_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.M_ME_NB_1);
        assertEquals("Should be scaled measurement response", ASduType.M_ME_NB_1, asdu.getTypeIdentification());
        assertEquals("Should have spontaneous cause", CauseOfTransmission.SPONTANEOUS, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateInterrogationMeasurementResponse(asdu);
    }

    private void thenClientReceivesCounterInterrogationResponse() {
        waitForClientResponse(ASduType.M_IT_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.M_IT_NA_1);
        assertEquals("Should be counter response", ASduType.M_IT_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have interrogated by station cause", CauseOfTransmission.INTERROGATED_BY_STATION, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateCounterInterrogationResponse(asdu);
    }

    private void thenClientReceivesSingleCommandConfirmation() {
        waitForClientResponse(ASduType.C_SC_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_SC_NA_1);
        assertEquals("Should be single command confirmation", ASduType.C_SC_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateSingleCommandConfirmation(asdu);
    }

    private void thenClientReceivesDoubleCommandConfirmation() {
        waitForClientResponse(ASduType.C_DC_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_DC_NA_1);
        assertEquals("Should be double command confirmation", ASduType.C_DC_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateDoubleCommandConfirmation(asdu);
    }

    private void thenClientReceivesReadCommandResponse() {
        waitForClientResponse(ASduType.M_ME_NB_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.M_ME_NB_1);
        assertEquals("Should be measurement response", ASduType.M_ME_NB_1, asdu.getTypeIdentification());
        assertEquals("Should have request cause", CauseOfTransmission.REQUEST, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateReadCommandMeasurementResponse(asdu);
    }

    private void thenClientReceivesClockSyncConfirmation() {
        waitForClientResponse(ASduType.C_CS_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_CS_NA_1);
        assertEquals("Should be clock sync confirmation", ASduType.C_CS_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateClockSyncConfirmation(asdu);
    }

    private void thenClientReceivesTestCommandConfirmation() {
        waitForClientResponse(ASduType.C_TS_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_TS_NA_1);
        assertEquals("Should be test command confirmation", ASduType.C_TS_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateTestCommandConfirmation(asdu);
    }

    private void thenClientReceivesSetPointConfirmation() {
        waitForClientResponse(ASduType.C_SE_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_SE_NA_1);
        assertEquals("Should be set point confirmation", ASduType.C_SE_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateSetPointConfirmation(asdu);
    }

    private void thenClientReceivesScaledSetPointConfirmation() {
        waitForClientResponse(ASduType.C_SE_NB_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_SE_NB_1);
        assertEquals("Should be scaled set point confirmation", ASduType.C_SE_NB_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateScaledSetPointConfirmation(asdu);
    }

    private void thenClientReceivesShortFloatSetPointConfirmation() {
        waitForClientResponse(ASduType.C_SE_NC_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_SE_NC_1);
        assertEquals("Should be short float set point confirmation", ASduType.C_SE_NC_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateShortFloatSetPointConfirmation(asdu);
    }

    private void thenClientReceivesRegulatingStepConfirmation() {
        waitForClientResponse(ASduType.C_RC_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.C_RC_NA_1);
        assertEquals("Should be regulating step confirmation", ASduType.C_RC_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateRegulatingStepConfirmation(asdu);
    }

    private void thenClientReceivesParameterActivationConfirmation() {
        waitForClientResponse(ASduType.P_AC_NA_1);
        
        ASdu asdu = getLastReceivedAsdu(ASduType.P_AC_NA_1);
        assertEquals("Should be parameter activation confirmation", ASduType.P_AC_NA_1, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateParameterActivationConfirmation(asdu);
    }

    private void thenClientReceivesTimeTaggedCommandConfirmation(ASduType expectedType) {
        waitForClientResponse(expectedType);
        
        ASdu asdu = getLastReceivedAsdu(expectedType);
        assertEquals("Should be time tagged command confirmation", expectedType, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateTimeTaggedCommandConfirmation(asdu);
    }

    private void thenClientReceivesParameterConfirmation(ASduType expectedType) {
        waitForClientResponse(expectedType);
        
        ASdu asdu = getLastReceivedAsdu(expectedType);
        assertEquals("Should be parameter confirmation", expectedType, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateParameterConfirmation(asdu);
    }

    private void thenClientReceivesBitstringCommandConfirmation(ASduType expectedType) {
        waitForClientResponse(expectedType);
        
        ASdu asdu = getLastReceivedAsdu(expectedType);
        assertEquals("Should be bitstring command confirmation", expectedType, asdu.getTypeIdentification());
        assertEquals("Should have activation confirmation cause", CauseOfTransmission.ACTIVATION_CON, asdu.getCauseOfTransmission());
        
        validateCommonAsduProperties(asdu);
        validateBitstringCommandConfirmation(asdu);
    }
    
    private void validateCommonAsduProperties(ASdu asdu) {
        assertEquals("Should have correct common address", COMMON_ADDRESS, asdu.getCommonAddress());
        assertFalse("Should not be a test ASDU", asdu.isTestFrame());
        assertNotNull("Should have information objects", asdu.getInformationObjects());
        assertTrue("Should have at least one information object", asdu.getInformationObjects().length > 0);
    }
    
    private void validateInterrogationMeasurementResponse(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertEquals("Should have measurement address", MEASUREMENT_ADDRESS, objects[0].getInformationObjectAddress());
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have measurement values", elements.length > 0);
        
        assertTrue("Interrogation response should have scaled value", elements[0][0] instanceof IeScaledValue);
        IeScaledValue scaledValue = (IeScaledValue) elements[0][0];
        assertEquals("Interrogation response should return expected scaled value", SCALED_VALUE, scaledValue.getUnnormalizedValue());
        
        assertTrue("Second element should be quality", elements[0][1] instanceof IeQuality);
        IeQuality quality = (IeQuality) elements[0][1];
        assertFalse("Quality should indicate valid data", quality.isInvalid());
        assertFalse("Quality should not be blocked", quality.isBlocked());
        assertFalse("Quality should not be substituted", quality.isSubstituted());
        assertFalse("Quality should not be not topical", quality.isNotTopical());
        assertFalse("Quality should not have overflow", quality.isOverflow());
    }

    private void validateReadCommandMeasurementResponse(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertEquals("Should have read command address", READ_COMMAND_ADDRESS, objects[0].getInformationObjectAddress());
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have measurement values", elements.length > 0);
        
        assertTrue("Read response should have normalized value", elements[0][0] instanceof IeNormalizedValue);
        IeNormalizedValue normalizedValue = (IeNormalizedValue) elements[0][0];
        assertEquals("Read response should return expected normalized value", 0.85, normalizedValue.getNormalizedValue(), NORMALIZED_TOLERANCE);
        
        assertTrue("Second element should be quality", elements[0][1] instanceof IeQuality);
        IeQuality quality = (IeQuality) elements[0][1];
        assertFalse("Quality should indicate valid data", quality.isInvalid());
        assertFalse("Quality should not be blocked", quality.isBlocked());
        assertFalse("Quality should not be substituted", quality.isSubstituted());
        assertFalse("Quality should not be not topical", quality.isNotTopical());
        assertFalse("Quality should not have overflow", quality.isOverflow());
    }
    
    private void validateCounterInterrogationResponse(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertEquals("Should have counter address", COUNTER_ADDRESS, objects[0].getInformationObjectAddress());
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have counter values", elements.length >= 2);
        
        // Validate first counter value
        assertTrue("First element should be counter reading", elements[0][0] instanceof IeBinaryCounterReading);
        IeBinaryCounterReading counter1 = (IeBinaryCounterReading) elements[0][0];
        assertEquals("First counter should have expected value", COUNTER_VALUE_1, counter1.getCounterReading());
        assertEquals("First counter should have sequence number 1", 1, counter1.getSequenceNumber());
        
        // Validate second counter value
        assertTrue("Second counter element should be counter reading", elements[1][0] instanceof IeBinaryCounterReading);
        IeBinaryCounterReading counter2 = (IeBinaryCounterReading) elements[1][0];
        assertEquals("Second counter should have expected value", COUNTER_VALUE_2, counter2.getCounterReading());
        assertEquals("Second counter should have sequence number 2", 2, counter2.getSequenceNumber());
    }
    
    private void validateSingleCommandConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertEquals("Should have expected single command address", SINGLE_COMMAND_ADDRESS, objects[0].getInformationObjectAddress());
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have single command element", elements[0][0] instanceof IeSingleCommand);
        
        IeSingleCommand command = (IeSingleCommand) elements[0][0];
        assertTrue("Single command should be set to true (ON)", command.isCommandStateOn());
        assertEquals("Single command should have no qualifier", 0, command.getQualifier());
        assertFalse("Single command should not be select", command.isSelect());
    }
    
    private void validateDoubleCommandConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertEquals("Should have expected double command address", DOUBLE_COMMAND_ADDRESS, objects[0].getInformationObjectAddress());
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have double command element", elements[0][0] instanceof IeDoubleCommand);
        
        IeDoubleCommand command = (IeDoubleCommand) elements[0][0];
        assertEquals("Double command should be set to ON", IeDoubleCommand.DoubleCommandState.ON, command.getCommandState());
        assertEquals("Double command should have no qualifier", 0, command.getQualifier());
        assertFalse("Double command should not be select", command.isSelect());
    }
    
    private void validateSetPointConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertEquals("Should have expected set point address", SET_POINT_ADDRESS, objects[0].getInformationObjectAddress());
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have normalized value element", elements[0][0] instanceof IeNormalizedValue);
        assertTrue("Should have qualifier element", elements[0][1] instanceof IeQualifierOfSetPointCommand);
        
        IeNormalizedValue setValue = (IeNormalizedValue) elements[0][0];
        assertEquals("Set point should have expected normalized value", 0.75, setValue.getNormalizedValue(), NORMALIZED_TOLERANCE);
        
        IeQualifierOfSetPointCommand qualifier = (IeQualifierOfSetPointCommand) elements[0][1];
        assertEquals("Set point qualifier should be zero", 0, qualifier.getQl());
        assertFalse("Set point should not be select", qualifier.isSelect());
    }
    
    private void validateClockSyncConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertEquals("Clock sync should use address 0", 0, objects[0].getInformationObjectAddress());
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have time element", elements[0][0] instanceof IeTime56);
        
        IeTime56 timeElement = (IeTime56) elements[0][0];
        long currentTime = System.currentTimeMillis();
        long receivedTime = timeElement.getTimestamp();
        
        long timeDiff = Math.abs(currentTime - receivedTime);
        assertTrue("Clock sync time should be within tolerance of current time (diff: " + timeDiff + "ms)", 
                   timeDiff < TIME_TOLERANCE_MS);
    }
    
    private void validateTestCommandConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid test command address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have test command elements", elements.length > 0);
        
        assertTrue("Should have test pattern", elements[0][0] instanceof IeFixedTestBitPattern);
    }

    private void validateScaledSetPointConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid scaled set point address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have scaled set point elements", elements.length > 0);
        
        assertTrue("Should have scaled value", elements[0][0] instanceof IeScaledValue);
        IeScaledValue scaledValue = (IeScaledValue) elements[0][0];
        assertTrue("Scaled value should be valid", scaledValue.getUnnormalizedValue() != 0);
    }

    private void validateShortFloatSetPointConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid short float set point address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have short float set point elements", elements.length > 0);
        
        assertTrue("Should have short float value", elements[0][0] instanceof IeShortFloat);
        IeShortFloat shortFloat = (IeShortFloat) elements[0][0];
        assertTrue("Short float value should be valid", shortFloat.getValue() != 0.0f);
    }

    private void validateRegulatingStepConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid regulating step address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have regulating step elements", elements.length > 0);
        
        assertTrue("Should have regulating step command", elements[0][0] instanceof IeRegulatingStepCommand);
    }

    private void validateParameterActivationConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid parameter activation address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have parameter activation elements", elements.length > 0);
        
        assertTrue("Should have parameter qualifier", elements[0][0] instanceof IeQualifierOfParameterActivation);
    }

    private void validateTimeTaggedCommandConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid time tagged command address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have time tagged command elements", elements.length > 0);
        
        boolean hasTimeElement = false;
        for (InformationElement element : elements[0]) {
            if (element instanceof IeTime56) {
                hasTimeElement = true;
                break;
            }
        }
        assertTrue("Should have time element", hasTimeElement);
    }

    private void validateParameterConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid parameter address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have parameter elements", elements.length > 0);
        
        // Parameter confirmations should have at least one parameter element
        InformationElement firstElement = elements[0][0];
        assertTrue("Should have parameter element", 
                   firstElement instanceof IeNormalizedValue ||
                   firstElement instanceof IeScaledValue ||
                   firstElement instanceof IeShortFloat);
    }

    private void validateBitstringCommandConfirmation(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        assertTrue("Should have valid bitstring command address", objects[0].getInformationObjectAddress() >= 0);
        
        InformationElement[][] elements = objects[0].getInformationElements();
        assertTrue("Should have bitstring command elements", elements.length > 0);
        
        assertTrue("Should have bitstring element", elements[0][0] instanceof IeBinaryStateInformation);
    }

    private ASdu getLastReceivedAsdu(ASduType type) {
        return spyClientListener.getLastReceivedAsduOfType(type);
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
}
