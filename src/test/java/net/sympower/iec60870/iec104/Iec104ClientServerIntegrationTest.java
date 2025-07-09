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
package net.sympower.iec60870.iec104;

import org.junit.After;
import org.junit.Test;
import net.sympower.iec60870.spy.RespondingServer;
import net.sympower.iec60870.spy.AsduRecordingClient;
import net.sympower.iec60870.common.api.IEC60870Server;
import net.sympower.iec60870.common.api.IEC60870ServerBuilder;
import net.sympower.iec60870.common.api.IEC60870ClientBuilder;
import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.elements.IeBinaryStateInformation;
import net.sympower.iec60870.common.elements.IeDoubleCommand;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQualifierOfCounterInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfParameterOfMeasuredValues;
import net.sympower.iec60870.common.elements.IeQualifierOfSetPointCommand;
import net.sympower.iec60870.common.elements.IeRegulatingStepCommand;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeShortFloat;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeTestSequenceCounter;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationElement;
import net.sympower.iec60870.common.elements.InformationObject;
import net.sympower.iec60870.iec104.connection.Iec104ClientConnection;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class Iec104ClientServerIntegrationTest {

    private IEC60870Server server;
    private Iec104ClientConnection client;

    private RespondingServer spyServerListener;
    private AsduRecordingClient spyClientListener;

    private static final int TEST_PORT = 12345;
    private static final int COMMON_ADDRESS = 1;
    private static final int CONNECTION_TIMEOUT = 5000;

    @After
    public void tearDown() {
        if (client != null && !client.isClosed()) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testInterrogationCommand_shouldReceiveResponseFromServer() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsInterrogation();

        thenServerReceivesInterrogation();
        thenClientReceivesMeasurementData();
    }

    @Test
    public void testSingleCommand_shouldReceiveConfirmationFromServer() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsSingleCommand();

        thenServerReceivesSingleCommand();
        thenClientReceivesCommandConfirmation();
    }

    @Test
    public void testClockSynchronization_shouldReceiveTimeFromServer() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsClockSync();

        thenServerReceivesClockSync();
        thenClientReceivesTimeResponse();
    }

    @Test
    public void testCounterInterrogation_shouldReceiveCounterValues() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsCounterInterrogation();

        thenServerReceivesCounterInterrogation();
        thenClientReceivesCounterValues();
    }

    @Test
    public void testSetPointCommand_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsSetPointCommand();

        thenServerReceivesSetPointCommand();
        thenClientReceivesSetPointConfirmation();
    }

    @Test
    public void testReadCommand_shouldReceiveResponse() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsReadCommand();

        thenServerReceivesReadCommand();
        thenClientReceivesReadResponse();
    }

    @Test
    public void testDoubleCommand_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsDoubleCommand();

        thenServerReceivesDoubleCommand();
        thenClientReceivesDoubleCommandConfirmation();
    }

    @Test
    public void testRegulatingStepCommand_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsRegulatingStepCommand();

        thenServerReceivesRegulatingStepCommand();
        thenClientReceivesRegulatingStepConfirmation();
    }

    @Test
    public void testScaledValueSetPointCommand_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsScaledValueSetPointCommand();

        thenServerReceivesScaledValueSetPoint();
        thenClientReceivesScaledValueSetPointConfirmation();
    }

    @Test
    public void testShortFloatSetPointCommand_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsShortFloatSetPointCommand();

        thenServerReceivesShortFloatSetPoint();
        thenClientReceivesShortFloatSetPointConfirmation();
    }

    @Test
    public void testTestCommand_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsTestCommand();

        thenServerReceivesTestCommand();
        thenClientReceivesTestCommandConfirmation();
    }

    @Test
    public void testTestCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsTestCommandWithTimeTag();

        thenServerReceivesTestCommandWithTimeTag();
        thenClientReceivesTestCommandWithTimeTagConfirmation();
    }

    @Test
    public void testParameterActivation_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsParameterActivation();

        thenServerReceivesParameterActivation();
        thenClientReceivesParameterActivationConfirmation();
    }

    @Test
    public void testResetProcess_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsResetProcess();

        thenServerReceivesResetProcess();
        thenClientReceivesResetProcessConfirmation();
    }

    @Test
    public void testDelayAcquisition_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsDelayAcquisition();

        thenServerReceivesDelayAcquisition();
        thenClientReceivesDelayAcquisitionConfirmation();
    }

    @Test
    public void testSingleCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsSingleCommandWithTimeTag();

        thenServerReceivesSingleCommandWithTimeTag();
        thenClientReceivesSingleCommandWithTimeTagConfirmation();
    }

    @Test
    public void testDoubleCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsDoubleCommandWithTimeTag();

        thenServerReceivesDoubleCommandWithTimeTag();
        thenClientReceivesDoubleCommandWithTimeTagConfirmation();
    }

    @Test
    public void testRegulatingStepCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsRegulatingStepCommandWithTimeTag();

        thenServerReceivesRegulatingStepCommandWithTimeTag();
        thenClientReceivesRegulatingStepCommandWithTimeTagConfirmation();
    }

    @Test
    public void testSetNormalizedValueCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsSetNormalizedValueCommandWithTimeTag();

        thenServerReceivesSetNormalizedValueCommandWithTimeTag();
        thenClientReceivesSetNormalizedValueCommandWithTimeTagConfirmation();
    }

    @Test
    public void testSetScaledValueCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsSetScaledValueCommandWithTimeTag();

        thenServerReceivesSetScaledValueCommandWithTimeTag();
        thenClientReceivesSetScaledValueCommandWithTimeTagConfirmation();
    }

    @Test
    public void testSetShortFloatCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsSetShortFloatCommandWithTimeTag();

        thenServerReceivesSetShortFloatCommandWithTimeTag();
        thenClientReceivesSetShortFloatCommandWithTimeTagConfirmation();
    }

    @Test
    public void testBitstringCommand_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsBitstringCommand();

        thenServerReceivesBitstringCommand();
        thenClientReceivesBitstringCommandConfirmation();
    }

    @Test
    public void testBitstringCommandWithTimeTag_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsBitstringCommandWithTimeTag();

        thenServerReceivesBitstringCommandWithTimeTag();
        thenClientReceivesBitstringCommandWithTimeTagConfirmation();
    }

    @Test
    public void testParameterNormalizedValue_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsParameterNormalizedValue();

        thenServerReceivesParameterNormalizedValue();
        thenClientReceivesParameterNormalizedValueConfirmation();
    }

    @Test
    public void testParameterScaledValue_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsParameterScaledValue();

        thenServerReceivesParameterScaledValue();
        thenClientReceivesParameterScaledValueConfirmation();
    }

    @Test
    public void testParameterShortFloat_shouldReceiveConfirmation() throws Exception {
        givenServerIsRunning();
        givenClientIsConnected();

        whenClientSendsParameterShortFloat();

        thenServerReceivesParameterShortFloat();
        thenClientReceivesParameterShortFloatConfirmation();
    }

    private void givenServerIsRunning() throws IOException {
        spyServerListener = new RespondingServer();
        server = new IEC60870ServerBuilder()
                .iec104(TEST_PORT)
                .maxConnections(1)
                .build();
        server.start(spyServerListener);
    }

    private void givenClientIsConnected() throws IOException {
        client = new IEC60870ClientBuilder()
                .iec104("localhost", TEST_PORT)
                .connectionTimeout(CONNECTION_TIMEOUT)
                .build();

        spyClientListener = new AsduRecordingClient();
        client.startDataTransfer(spyClientListener);

        await().atMost(2, TimeUnit.SECONDS)
               .until(() -> spyServerListener.isConnectionAccepted() && spyClientListener.isConnectionReady());
    }

    private void whenClientSendsInterrogation() throws IOException {
        client.interrogation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 
                           new IeQualifierOfInterrogation(20));
    }

    private void whenClientSendsSingleCommand() throws IOException {
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 5000,
                           new IeSingleCommand(true, 0, false));
    }

    private void whenClientSendsClockSync() throws IOException {
        client.synchronizeClocks(COMMON_ADDRESS, new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsCounterInterrogation() throws IOException {
        client.counterInterrogation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION,
                                  new IeQualifierOfCounterInterrogation(5, 0));
    }

    private void whenClientSendsSetPointCommand() throws IOException {
        client.setNormalizedValueCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 6000,
                                       new IeNormalizedValue(0.75), 
                                       new IeQualifierOfSetPointCommand(0, false));
    }

    private void whenClientSendsReadCommand() throws IOException {
        client.read(COMMON_ADDRESS, 7000);
    }

    private void whenClientSendsDoubleCommand() throws IOException {
        client.doubleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 8000,
                           new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
    }

    private void whenClientSendsRegulatingStepCommand() throws IOException {
        client.regulatingStepCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 9000,
                                   new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER, 0, false));
    }

    private void whenClientSendsScaledValueSetPointCommand() throws IOException {
        client.setScaledValueCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 10000,
                                   new IeScaledValue(12345),
                                   new IeQualifierOfSetPointCommand(0, false));
    }

    private void whenClientSendsShortFloatSetPointCommand() throws IOException {
        client.setShortFloatCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 11000,
                                  new IeShortFloat(3.14159f),
                                  new IeQualifierOfSetPointCommand(0, false));
    }

    private void whenClientSendsTestCommand() throws IOException {
        client.testCommand(COMMON_ADDRESS);
    }

    private void whenClientSendsTestCommandWithTimeTag() throws IOException {
        client.testCommandWithTimeTag(COMMON_ADDRESS, 
                                    new IeTestSequenceCounter(0x5555),
                                    new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsParameterActivation() throws IOException {
        client.parameterActivation(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 12000, 1);
    }

    private void whenClientSendsResetProcess() throws IOException {
        client.resetProcess(COMMON_ADDRESS, 1);
    }

    private void whenClientSendsDelayAcquisition() throws IOException {
        client.delayAcquisition(COMMON_ADDRESS, 5000);
    }

    private void whenClientSendsSingleCommandWithTimeTag() throws IOException {
        client.singleCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 13000,
                                      new IeSingleCommand(true, 0, false),
                                      new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsDoubleCommandWithTimeTag() throws IOException {
        client.doubleCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 14000,
                                      new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false),
                                      new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsRegulatingStepCommandWithTimeTag() throws IOException {
        client.regulatingStepCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 15000,
                                               new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER, 0, false),
                                               new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsSetNormalizedValueCommandWithTimeTag() throws IOException {
        client.setNormalizedValueCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 16000,
                                                   new IeNormalizedValue(0.5),
                                                   new IeQualifierOfSetPointCommand(0, false),
                                                   new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsSetScaledValueCommandWithTimeTag() throws IOException {
        client.setScaledValueCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 17000,
                                               new IeScaledValue(9876),
                                               new IeQualifierOfSetPointCommand(0, false),
                                               new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsSetShortFloatCommandWithTimeTag() throws IOException {
        client.setShortFloatCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 18000,
                                              new IeShortFloat(2.718f),
                                              new IeQualifierOfSetPointCommand(0, false),
                                              new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsBitstringCommand() throws IOException {
        client.bitstringCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 19000,
                              new IeBinaryStateInformation(0xABCDEF12));
    }

    private void whenClientSendsBitstringCommandWithTimeTag() throws IOException {
        client.bitstringCommandWithTimeTag(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 20000,
                                          new IeBinaryStateInformation(0x12345678),
                                          new IeTime56(System.currentTimeMillis()));
    }

    private void whenClientSendsParameterNormalizedValue() throws IOException {
        client.parameterMeasuredValueNormalized(COMMON_ADDRESS, 21000,
                                               new IeNormalizedValue(0.25),
                                               new IeQualifierOfParameterOfMeasuredValues(1, false, false));
    }

    private void whenClientSendsParameterScaledValue() throws IOException {
        client.parameterMeasuredValueScaled(COMMON_ADDRESS, 22000,
                                           new IeScaledValue(5555),
                                           new IeQualifierOfParameterOfMeasuredValues(1, false, false));
    }

    private void whenClientSendsParameterShortFloat() throws IOException {
        client.parameterMeasuredValueShortFloat(COMMON_ADDRESS, 23000,
                                               new IeShortFloat(1.414f),
                                               new IeQualifierOfParameterOfMeasuredValues(1, false, false));
    }

    private void thenServerReceivesInterrogation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedInterrogation());
    }

    private void thenClientReceivesMeasurementData() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received an ASDU response", receivedAsdu);
        assertEquals("Should be scaled measurement type", ASduType.M_ME_NB_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should have correct common address", COMMON_ADDRESS, receivedAsdu.getCommonAddress());
        
        InformationObject[] infoObjects = receivedAsdu.getInformationObjects();
        assertTrue("Should have at least one information object", infoObjects.length > 0);
        
        InformationObject firstObject = infoObjects[0];
        InformationElement[][] elements = firstObject.getInformationElements();
        assertTrue("Should have measurement elements", elements.length > 0);
        assertTrue("First element should be a scaled value", elements[0][0] instanceof IeScaledValue);
    }

    private void thenServerReceivesSingleCommand() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedCommand());
    }

    private void thenClientReceivesCommandConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received command confirmation", receivedAsdu);
        assertEquals("Should be single command confirmation", ASduType.C_SC_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesClockSync() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedClockSync());
    }

    private void thenClientReceivesTimeResponse() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received time response", receivedAsdu);
        assertEquals("Should be clock sync confirmation", ASduType.C_CS_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesCounterInterrogation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedCounterInterrogation());
    }

    private void thenClientReceivesCounterValues() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.getLastReceivedAsduOfType(ASduType.M_IT_NA_1) != null);

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsduOfType(ASduType.M_IT_NA_1);
        assertNotNull("Should have received counter values", receivedAsdu);
        assertEquals("Should be integrated totals", ASduType.M_IT_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be interrogated by station", CauseOfTransmission.INTERROGATED_BY_STATION, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesSetPointCommand() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedSetPoint());
    }

    private void thenClientReceivesSetPointConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received set point confirmation", receivedAsdu);
        assertEquals("Should be set point confirmation", ASduType.C_SE_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesReadCommand() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedReadCommand());
    }

    private void thenClientReceivesReadResponse() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received read response", receivedAsdu);
        assertTrue("Should be measurement data", 
                   receivedAsdu.getTypeIdentification() == ASduType.M_ME_NA_1 ||
                   receivedAsdu.getTypeIdentification() == ASduType.M_ME_NB_1 ||
                   receivedAsdu.getTypeIdentification() == ASduType.M_ME_NC_1);
    }

    private void thenServerReceivesDoubleCommand() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedDoubleCommand());
    }

    private void thenClientReceivesDoubleCommandConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received double command confirmation", receivedAsdu);
        assertEquals("Should be double command confirmation", ASduType.C_DC_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesRegulatingStepCommand() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedRegulatingStepCommand());
    }

    private void thenClientReceivesRegulatingStepConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received regulating step confirmation", receivedAsdu);
        assertEquals("Should be regulating step confirmation", ASduType.C_RC_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesScaledValueSetPoint() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedScaledValueSetPoint());
    }

    private void thenClientReceivesScaledValueSetPointConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received scaled value set point confirmation", receivedAsdu);
        assertEquals("Should be scaled value set point confirmation", ASduType.C_SE_NB_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesShortFloatSetPoint() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedShortFloatSetPoint());
    }

    private void thenClientReceivesShortFloatSetPointConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received short float set point confirmation", receivedAsdu);
        assertEquals("Should be short float set point confirmation", ASduType.C_SE_NC_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesTestCommand() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedTestCommand());
    }

    private void thenClientReceivesTestCommandConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received test command confirmation", receivedAsdu);
        assertEquals("Should be test command confirmation", ASduType.C_TS_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesTestCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedTestCommandWithTimeTag());
    }

    private void thenClientReceivesTestCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received test command with time tag confirmation", receivedAsdu);
        assertEquals("Should be test command with time tag confirmation", ASduType.C_TS_TA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesParameterActivation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedParameterActivation());
    }

    private void thenClientReceivesParameterActivationConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received parameter activation confirmation", receivedAsdu);
        assertEquals("Should be parameter activation confirmation", ASduType.P_AC_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesResetProcess() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedResetProcess());
    }

    private void thenClientReceivesResetProcessConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received reset process confirmation", receivedAsdu);
        assertEquals("Should be reset process confirmation", ASduType.C_RP_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesDelayAcquisition() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedDelayAcquisition());
    }

    private void thenClientReceivesDelayAcquisitionConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received delay acquisition confirmation", receivedAsdu);
        assertEquals("Should be delay acquisition confirmation", ASduType.C_CD_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesSingleCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedSingleCommandWithTimeTag());
    }

    private void thenClientReceivesSingleCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received single command with time tag confirmation", receivedAsdu);
        assertEquals("Should be single command with time tag confirmation", ASduType.C_SC_TA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesDoubleCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedDoubleCommandWithTimeTag());
    }

    private void thenClientReceivesDoubleCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received double command with time tag confirmation", receivedAsdu);
        assertEquals("Should be double command with time tag confirmation", ASduType.C_DC_TA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesRegulatingStepCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedRegulatingStepCommandWithTimeTag());
    }

    private void thenClientReceivesRegulatingStepCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received regulating step command with time tag confirmation", receivedAsdu);
        assertEquals("Should be regulating step command with time tag confirmation", ASduType.C_RC_TA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesSetNormalizedValueCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedSetNormalizedValueCommandWithTimeTag());
    }

    private void thenClientReceivesSetNormalizedValueCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received set normalized value command with time tag confirmation", receivedAsdu);
        assertEquals("Should be set normalized value command with time tag confirmation", ASduType.C_SE_TA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesSetScaledValueCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedSetScaledValueCommandWithTimeTag());
    }

    private void thenClientReceivesSetScaledValueCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received set scaled value command with time tag confirmation", receivedAsdu);
        assertEquals("Should be set scaled value command with time tag confirmation", ASduType.C_SE_TB_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesSetShortFloatCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedSetShortFloatCommandWithTimeTag());
    }

    private void thenClientReceivesSetShortFloatCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received set short float command with time tag confirmation", receivedAsdu);
        assertEquals("Should be set short float command with time tag confirmation", ASduType.C_SE_TC_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesBitstringCommand() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedBitstringCommand());
    }

    private void thenClientReceivesBitstringCommandConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received bitstring command confirmation", receivedAsdu);
        assertEquals("Should be bitstring command confirmation", ASduType.C_BO_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesBitstringCommandWithTimeTag() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedBitstringCommandWithTimeTag());
    }

    private void thenClientReceivesBitstringCommandWithTimeTagConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received bitstring command with time tag confirmation", receivedAsdu);
        assertEquals("Should be bitstring command with time tag confirmation", ASduType.C_BO_TA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesParameterNormalizedValue() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedParameterNormalizedValue());
    }

    private void thenClientReceivesParameterNormalizedValueConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received parameter normalized value confirmation", receivedAsdu);
        assertEquals("Should be parameter normalized value confirmation", ASduType.P_ME_NA_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesParameterScaledValue() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedParameterScaledValue());
    }

    private void thenClientReceivesParameterScaledValueConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received parameter scaled value confirmation", receivedAsdu);
        assertEquals("Should be parameter scaled value confirmation", ASduType.P_ME_NB_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    private void thenServerReceivesParameterShortFloat() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyServerListener.hasReceivedParameterShortFloat());
    }

    private void thenClientReceivesParameterShortFloatConfirmation() {
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> spyClientListener.hasReceivedAsdu());

        ASdu receivedAsdu = spyClientListener.getLastReceivedAsdu();
        assertNotNull("Should have received parameter short float confirmation", receivedAsdu);
        assertEquals("Should be parameter short float confirmation", ASduType.P_ME_NC_1, receivedAsdu.getTypeIdentification());
        assertEquals("Should be activation confirmation", CauseOfTransmission.ACTIVATION_CON, receivedAsdu.getCauseOfTransmission());
    }

    // ========== TIMEOUT TESTS ==========

    @Test
    public void testT1Timeout_shouldCloseConnectionImmediately() throws Exception {
        // Given: A server that doesn't send acknowledgments and client with short T1 timeout
        givenServerWithNoAcknowledgments();
        givenClientWithShortT1Timeout();

        // When: Client sends command and waits for T1 timeout
        whenClientSendsCommandWithoutAcknowledgment();

        // Then: Connection should close immediately after T1 timeout (no retries)
        thenServerReceivesExactlyOneCommand();
        thenConnectionIsClosedDueToT1Timeout();
    }

    @Test
    public void testT3Timeout_shouldSendTestFrames() throws Exception {
        // Given: Connected client and server with short T3 timeout
        givenServerWithShortT3Timeout();
        givenClientWithShortT3Timeout();

        // When: No communication occurs for T3 period
        whenNoDataIsTransmittedForT3Period();

        // Then: Connection should remain active (test frames maintain the connection)
        thenConnectionRemainsActive();
    }


    @Test
    public void testWParameterImmediateAcknowledgment() throws Exception {
        // Given: Server and client with W parameter set to 3
        givenServerWithWParameterThreshold();
        givenClientWithWParameterThreshold();

        // When: Client sends multiple I-frames up to W threshold
        whenClientSendsMultipleIFramesUpToWThreshold();

        // Then: Server sends immediate acknowledgment at W threshold
        thenServerSendsImmediateAcknowledgmentAtWThreshold();
    }

    @Test
    public void testKParameterFlowControl() throws Exception {
        // Given: Server and client with K parameter set to 3
        givenServerWithKParameterThreshold();
        givenClientWithKParameterThreshold();

        // When: Client tries to send more I-frames than K allows
        whenClientSendsMoreFramesThanKThreshold();

        // Then: Client should block after K frames and throw exception
        thenClientBlocksAfterKFramesAndThrowsException();
    }

    // Helper methods for timeout tests

    private void givenServerWithNoAcknowledgments() throws IOException {
        spyServerListener = new NoAckSpyServerListener();
        server = new IEC60870ServerBuilder()
                .iec104(TEST_PORT)
                .maxConnections(1)
                .build();
        server.start(spyServerListener);
    }

    private void givenClientWithShortT1Timeout() throws IOException {
        client = new IEC60870ClientBuilder()
                .iec104("localhost", TEST_PORT)
                .commonAddress(COMMON_ADDRESS)
                .connectionTimeout(CONNECTION_TIMEOUT)
                .maxTimeNoAckReceived(1000) // T1: 1 seconds instead of default 15
                .build();
        
        spyClientListener = new AsduRecordingClient();
        client.startDataTransfer(spyClientListener);
        
        await().atMost(2, TimeUnit.SECONDS).until(() -> spyClientListener.isConnectionReady());
    }

    private void givenServerWithShortT3Timeout() throws IOException {
        spyServerListener = new RespondingServer();
        server = new IEC60870ServerBuilder()
                .iec104(TEST_PORT)
                .maxConnections(1)
                .maxIdleTime(3000) // T3: 3 seconds instead of default 20
                .build();
        server.start(spyServerListener);
    }

    private void givenClientWithShortT3Timeout() throws IOException {
        client = new IEC60870ClientBuilder()
                .iec104("localhost", TEST_PORT)
                .commonAddress(COMMON_ADDRESS)
                .connectionTimeout(CONNECTION_TIMEOUT)
                .maxIdleTime(3000) // T3: 3 seconds instead of default 20
                .build();
        
        spyClientListener = new AsduRecordingClient();
        client.startDataTransfer(spyClientListener);
        
        await().atMost(2, TimeUnit.SECONDS).until(() -> spyClientListener.isConnectionReady());
    }



    private void givenServerWithWParameterThreshold() throws IOException {
        spyServerListener = new RespondingServer();
        server = new IEC60870ServerBuilder()
                .iec104(TEST_PORT)
                .maxConnections(1)
                .maxUnconfirmedIPdusReceived(3) // W parameter: 3 unconfirmed I-frames
                .build();
        server.start(spyServerListener);
    }

    private void givenClientWithWParameterThreshold() throws IOException {
        client = new IEC60870ClientBuilder()
                .iec104("localhost", TEST_PORT)
                .commonAddress(COMMON_ADDRESS)
                .connectionTimeout(CONNECTION_TIMEOUT)
                .maxUnconfirmedIPdusReceived(3) // W parameter: 3 unconfirmed I-frames
                .build();
        
        spyClientListener = new AsduRecordingClient();
        client.startDataTransfer(spyClientListener);
        
        await().atMost(2, TimeUnit.SECONDS).until(() -> spyClientListener.isConnectionReady());
    }

    private void givenServerWithKParameterThreshold() throws IOException {
        spyServerListener = new NoAckSpyServerListener(); // Server that doesn't send acknowledgments
        server = new IEC60870ServerBuilder()
                .iec104(TEST_PORT)
                .maxConnections(1)
                .maxNumOfOutstandingIPdus(3) // K parameter: 3 outstanding I-frames
                .build();
        server.start(spyServerListener);
    }

    private void givenClientWithKParameterThreshold() throws IOException {
        client = new IEC60870ClientBuilder()
                .iec104("localhost", TEST_PORT)
                .commonAddress(COMMON_ADDRESS)
                .connectionTimeout(CONNECTION_TIMEOUT)
                .maxNumOfOutstandingIPdus(3) // K parameter: 3 outstanding I-frames
                .build();
        
        spyClientListener = new AsduRecordingClient();
        client.startDataTransfer(spyClientListener);
        
        await().atMost(2, TimeUnit.SECONDS).until(() -> spyClientListener.isConnectionReady());
    }

    private void whenClientSendsCommandWithoutAcknowledgment() throws IOException {
        // Send a command that won't be acknowledged by the NoAckSpyServerListener
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1001, 
                            new IeSingleCommand(true, 0, false));
    }

    private void whenNoDataIsTransmittedForT3Period() throws InterruptedException {
        // Wait for longer than T3 timeout period
        Thread.sleep(4000); // Wait 4 seconds (longer than 3 second T3)
    }


    private void whenClientSendsMultipleIFramesUpToWThreshold() throws Exception {
        // Send exactly W (3) I-frames to trigger immediate acknowledgment
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1001, 
                            new IeSingleCommand(true, 0, false));
        Thread.sleep(50); // Small delay to ensure proper ordering
        
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1002, 
                            new IeSingleCommand(true, 0, false));
        Thread.sleep(50);
        
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1003, 
                            new IeSingleCommand(true, 0, false));
        Thread.sleep(50);
    }

    private void whenClientSendsMoreFramesThanKThreshold() throws Exception {
        // This should work for the first 3 frames (K=3)
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1001, 
                            new IeSingleCommand(true, 0, false));
        Thread.sleep(50);
        
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1002, 
                            new IeSingleCommand(true, 0, false));
        Thread.sleep(50);
        
        client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1003, 
                            new IeSingleCommand(true, 0, false));
        Thread.sleep(50);
        
        // This 4th frame should fail because K=3 and no acknowledgments received
        try {
            client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1004, 
                                new IeSingleCommand(true, 0, false));
        } catch (IOException e) {
            // Expected - this should throw "Too many unacknowledged I-frames"
        }
    }

    private void thenServerReceivesExactlyOneCommand() {
        // Verify that command was received exactly once (no retries)
        await().atMost(1, TimeUnit.SECONDS).until(() -> 
            ((NoAckSpyServerListener) spyServerListener).getCommandReceiveCount() == 1);
        
        // Wait a bit more to ensure no retries occur
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify count is still 1 (no retries)
        assertEquals("Server should receive exactly one command (no retries)", 1, 
                    ((NoAckSpyServerListener) spyServerListener).getCommandReceiveCount());
    }

    private void thenConnectionIsClosedDueToT1Timeout() {
        await().atMost(8, TimeUnit.SECONDS).until(() -> spyClientListener.isConnectionLost());
        assertTrue("Connection should be closed due to T1 timeout", client.isClosed());
    }

    private void thenConnectionRemainsActive() {
        // Wait a bit to ensure test frames have been exchanged
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify connection is still active after test frame exchange
        assertFalse("Connection should remain active", client.isClosed());
        assertTrue("Connection should be ready", spyClientListener.isConnectionReady());
    }


    private void thenServerSendsImmediateAcknowledgmentAtWThreshold() {
        await().atMost(3, TimeUnit.SECONDS).until(() ->
            spyServerListener.getReceivedCommandCount() >= 3);
        
        assertEquals("Server should receive exactly 3 commands", 3,
                    spyServerListener.getReceivedCommandCount());
        
        assertFalse("Connection should remain active with proper acknowledgments", client.isClosed());
        assertTrue("Connection should be ready", spyClientListener.isConnectionReady());
    }

    private void thenClientBlocksAfterKFramesAndThrowsException() {
        await().atMost(3, TimeUnit.SECONDS).until(() ->
            ((NoAckSpyServerListener) spyServerListener).getCommandReceiveCount() >= 3);
        
        assertEquals("Server should receive exactly 3 commands due to K parameter", 3,
                    ((NoAckSpyServerListener) spyServerListener).getCommandReceiveCount());
        
        boolean exceptionThrown = false;
        try {
            client.singleCommand(COMMON_ADDRESS, CauseOfTransmission.ACTIVATION, 1005, 
                                new IeSingleCommand(true, 0, false));
        } catch (IOException e) {
            exceptionThrown = true;
            assertTrue("Should get 'Too many unacknowledged I-frames' error", 
                      e.getMessage().contains("Too many unacknowledged I-frames"));
        }
        
        assertTrue("Should throw IOException when exceeding K parameter", exceptionThrown);
    }

    private static class NoAckSpyServerListener extends RespondingServer {
        private volatile int commandReceiveCount = 0;

        public int getCommandReceiveCount() {
            return commandReceiveCount;
        }

        @Override
        public void onConnectionAccepted(IEC60870Connection connection) {
            this.serverConnection = connection;
            this.connectionAccepted = true;

            try {
                connection.startDataTransfer(new NoAckServerConnectionHandler());
            } catch (IOException e) {
                fail("Failed to start data transfer on server connection: " + e.getMessage());
            }
        }

        private class NoAckServerConnectionHandler implements IEC60870EventListener {
            @Override
            public void onConnectionReady() {
                connectionReady = true;
            }

            @Override
            public void onAsduReceived(ASdu asdu) {
                switch (asdu.getTypeIdentification()) {
                    case C_SC_NA_1: // Single Command
                        commandReceiveCount++;
                        // Intentionally NOT sending acknowledgment to trigger T1 timeout
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onConnectionLost(IOException cause) {
            }
        }
    }

}
