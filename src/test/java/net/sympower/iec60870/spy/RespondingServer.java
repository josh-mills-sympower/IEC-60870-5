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
package net.sympower.iec60870.spy;

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.api.IEC60870ServerListener;
import net.sympower.iec60870.common.elements.IeBinaryCounterReading;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQuality;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationElement;
import net.sympower.iec60870.common.elements.InformationObject;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.fail;

public class RespondingServer implements IEC60870ServerListener, IEC60870EventListener, ServerSpy {

    private static final int COMMON_ADDRESS = 1;
    private static final int MEASUREMENT_ADDRESS = 100;
    private static final int COUNTER_ADDRESS = 200;
    private static final int READ_COMMAND_ADDRESS = 7000;
    private static final int SCALED_VALUE = 12345;
    private static final int COUNTER_VALUE_1 = 54321;
    private static final int COUNTER_VALUE_2 = 98765;

    private final Set<ASduType> receivedAsdus = EnumSet.noneOf(ASduType.class);
    protected volatile boolean connectionReady = false;
    protected volatile boolean connectionAccepted = false;
    protected IEC60870Connection serverConnection;
    private volatile int receivedCommandCount = 0;

    public boolean hasReceived(ASduType type) {
        return receivedAsdus.contains(type);
    }

    public boolean hasReceivedAnyCommand() {
        return !receivedAsdus.isEmpty();
    }

    public boolean isConnectionReady() {
        return connectionReady;
    }

    public boolean isConnectionAccepted() {
        return connectionAccepted;
    }

    public int getReceivedCommandCount() {
        return receivedCommandCount;
    }

    @Override
    public void onConnectionAccepted(IEC60870Connection connection) {
        this.serverConnection = connection;
        this.connectionAccepted = true;

        try {
            connection.startDataTransfer(this);
        } catch (IOException e) {
            fail("Failed to start data transfer on server connection: " + e.getMessage());
        }
    }

    @Override
    public void onConnectionReady() {
        connectionReady = true;
    }

    @Override
    public void onAsduReceived(ASdu asdu) {
        ASduType type = asdu.getTypeIdentification();
        receivedAsdus.add(type);

        try {
            switch (type) {
                case C_IC_NA_1:  // Interrogation
                    serverConnection.sendConfirmation(asdu);
                    sendMeasurementResponse();
                    break;
                case C_CI_NA_1:  // Counter interrogation
                    serverConnection.sendConfirmation(asdu);
                    sendCounterResponse();
                    break;
                case C_CS_NA_1:  // Clock sync
                    sendClockResponse();
                    break;
                case C_RD_NA_1:  // Read command
                    sendReadResponse();
                    break;
                default:
                    // All other commands just get confirmation
                    if (isCommand(type)) {
                        receivedCommandCount++;
                        serverConnection.sendConfirmation(asdu);
                    }
            }
        } catch (IOException e) {
            fail("Error handling ASDU in server: " + e.getMessage());
        }
    }

    @Override
    public void onConnectionLost(IOException cause) {
        // Connection lost - no action needed for tests
    }

    public void setServerConnection(IEC60870Connection connection) {
        this.serverConnection = connection;
    }

    private boolean isCommand(ASduType type) {
        return type.getId() >= 45 && type.getId() <= 113; // Command range
    }

    private void sendMeasurementResponse() throws IOException {
        InformationObject[] measurements = new InformationObject[] {
            new InformationObject(MEASUREMENT_ADDRESS, new InformationElement[][] {
                { new IeScaledValue(SCALED_VALUE), new IeQuality(false, false, false, false, false) },
                { new IeScaledValue(-5432), new IeQuality(false, false, false, false, false) },
                { new IeScaledValue(0), new IeQuality(false, false, false, false, false) }
            })
        };

        ASdu responseAsdu = new ASdu(ASduType.M_ME_NB_1, true, CauseOfTransmission.SPONTANEOUS,
                                   false, false, 0, COMMON_ADDRESS, measurements);
        serverConnection.send(responseAsdu);
    }

    private void sendCounterResponse() throws IOException {
        InformationObject[] counters = new InformationObject[] {
            new InformationObject(COUNTER_ADDRESS, new InformationElement[][] {
                { new IeBinaryCounterReading(COUNTER_VALUE_1, 1) },
                { new IeBinaryCounterReading(COUNTER_VALUE_2, 2) }
            })
        };

        ASdu responseAsdu = new ASdu(ASduType.M_IT_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                                   false, false, 0, COMMON_ADDRESS, counters);
        serverConnection.send(responseAsdu);
    }

    private void sendClockResponse() throws IOException {
        IeTime56 currentTime = new IeTime56(System.currentTimeMillis());
        InformationObject[] timeObject = new InformationObject[] {
            new InformationObject(0, new InformationElement[][] {
                { currentTime }
            })
        };

        ASdu responseAsdu = new ASdu(ASduType.C_CS_NA_1, false, CauseOfTransmission.ACTIVATION_CON,
                                   false, false, 0, COMMON_ADDRESS, timeObject);
        serverConnection.send(responseAsdu);
    }

    private void sendReadResponse() throws IOException {
        InformationObject[] readData = new InformationObject[] {
            new InformationObject(READ_COMMAND_ADDRESS, new InformationElement[][] {
                { new IeNormalizedValue(0.85f), new IeQuality(false, false, false, false, false) }
            })
        };

        ASdu responseAsdu = new ASdu(ASduType.M_ME_NB_1, false, CauseOfTransmission.REQUEST,
                                   false, false, 0, COMMON_ADDRESS, readData);
        serverConnection.send(responseAsdu);
    }

    public boolean hasReceivedInterrogation() {
        return hasReceived(ASduType.C_IC_NA_1);
    }

    public boolean hasReceivedCommand() {
        return hasReceived(ASduType.C_SC_NA_1);
    }

    public boolean hasReceivedClockSync() {
        return hasReceived(ASduType.C_CS_NA_1);
    }

    public boolean hasReceivedCounterInterrogation() {
        return hasReceived(ASduType.C_CI_NA_1);
    }

    public boolean hasReceivedSetPoint() {
        return hasReceived(ASduType.C_SE_NA_1);
    }

    public boolean hasReceivedReadCommand() {
        return hasReceived(ASduType.C_RD_NA_1);
    }

    public boolean hasReceivedDoubleCommand() {
        return hasReceived(ASduType.C_DC_NA_1);
    }

    public boolean hasReceivedRegulatingStepCommand() {
        return hasReceived(ASduType.C_RC_NA_1);
    }

    public boolean hasReceivedScaledValueSetPoint() {
        return hasReceived(ASduType.C_SE_NB_1);
    }

    public boolean hasReceivedShortFloatSetPoint() {
        return hasReceived(ASduType.C_SE_NC_1);
    }

    public boolean hasReceivedTestCommand() {
        return hasReceived(ASduType.C_TS_NA_1);
    }

    public boolean hasReceivedTestCommandWithTimeTag() {
        return hasReceived(ASduType.C_TS_TA_1);
    }

    public boolean hasReceivedParameterActivation() {
        return hasReceived(ASduType.P_AC_NA_1);
    }

    public boolean hasReceivedResetProcess() {
        return hasReceived(ASduType.C_RP_NA_1);
    }

    public boolean hasReceivedDelayAcquisition() {
        return hasReceived(ASduType.C_CD_NA_1);
    }

    public boolean hasReceivedSingleCommandWithTimeTag() {
        return hasReceived(ASduType.C_SC_TA_1);
    }

    public boolean hasReceivedDoubleCommandWithTimeTag() {
        return hasReceived(ASduType.C_DC_TA_1);
    }

    public boolean hasReceivedRegulatingStepCommandWithTimeTag() {
        return hasReceived(ASduType.C_RC_TA_1);
    }

    public boolean hasReceivedSetNormalizedValueCommandWithTimeTag() {
        return hasReceived(ASduType.C_SE_TA_1);
    }

    public boolean hasReceivedSetScaledValueCommandWithTimeTag() {
        return hasReceived(ASduType.C_SE_TB_1);
    }

    public boolean hasReceivedSetShortFloatCommandWithTimeTag() {
        return hasReceived(ASduType.C_SE_TC_1);
    }

    public boolean hasReceivedBitstringCommand() {
        return hasReceived(ASduType.C_BO_NA_1);
    }

    public boolean hasReceivedBitstringCommandWithTimeTag() {
        return hasReceived(ASduType.C_BO_TA_1);
    }

    public boolean hasReceivedParameterNormalizedValue() {
        return hasReceived(ASduType.P_ME_NA_1);
    }

    public boolean hasReceivedParameterScaledValue() {
        return hasReceived(ASduType.P_ME_NB_1);
    }

    public boolean hasReceivedParameterShortFloat() {
        return hasReceived(ASduType.P_ME_NC_1);
    }
}
