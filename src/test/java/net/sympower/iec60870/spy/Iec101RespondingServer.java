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
import net.sympower.iec60870.common.IEC60870Protocol;
import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.api.IEC60870ServerListener;
import net.sympower.iec60870.common.elements.*;
import net.sympower.iec60870.iec101.connection.Iec101ServerConnection;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

public class Iec101RespondingServer implements IEC60870EventListener, ServerSpy {

    private static final int COMMON_ADDRESS = 1;
    private static final int MEASUREMENT_ADDRESS = 100;
    private static final int COUNTER_ADDRESS = 200;
    private static final int READ_COMMAND_ADDRESS = 7000;
    private static final int SINGLE_POINT_ADDRESS = 300;
    private static final int DOUBLE_POINT_ADDRESS = 400;
    
    // Test values
    private static final int SCALED_VALUE = 1234;
    private static final short NORMALIZED_VALUE = 5678;
    private static final int COUNTER_VALUE = 999;

    private Iec101ServerConnection serverConnection;
    private final ConcurrentLinkedQueue<ASduType> receivedAsdus = new ConcurrentLinkedQueue<>();
    private final AtomicInteger receivedCommandCount = new AtomicInteger(0);
    private volatile boolean connectionReady = false;
    private volatile boolean connectionLost = false;

    public void onConnectionAccepted(IEC60870Connection connection) {
        this.serverConnection = (Iec101ServerConnection) connection;
    }

    @Override
    public void onConnectionReady() {
        this.connectionReady = true;
        this.connectionLost = false;
    }

    @Override
    public void onAsduReceived(ASdu asdu) {
        ASduType type = asdu.getTypeIdentification();
        receivedAsdus.add(type);

        try {
            switch (type) {
                case C_IC_NA_1:  // Interrogation
                    handleInterrogation(asdu);
                    break;
                case C_CI_NA_1:  // Counter interrogation
                    handleCounterInterrogation(asdu);
                    break;
                case C_CS_NA_1:  // Clock sync
                    handleClockSync(asdu);
                    break;
                case C_RD_NA_1:  // Read command
                    handleReadCommand(asdu);
                    break;
                default:
                    // All other commands just get confirmation
                    if (isCommand(type)) {
                        receivedCommandCount.incrementAndGet();
                        serverConnection.queueClass1Response(
                            IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress())
                        );
                    }
            }
        } catch (Exception e) {
            fail("Error handling ASDU in server: " + e.getMessage());
        }
    }

    private void handleInterrogation(ASdu asdu) {
        // Queue confirmation as Class 1 (high priority)
        serverConnection.queueClass1Response(
            IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress())
        );
        
        // Send single point information
        InformationObject[] singlePoints = new InformationObject[] {
            new InformationObject(SINGLE_POINT_ADDRESS, new InformationElement[][] {
                { new IeSinglePointWithQuality(true, false, false, false, false) },
                { new IeSinglePointWithQuality(false, false, false, false, false) }
            })
        };
        serverConnection.queueClass2Response(new ASdu(
            ASduType.M_SP_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
            false, false, 0, asdu.getCommonAddress(), singlePoints
        ));
        
        // Send double point information
        InformationObject[] doublePoints = new InformationObject[] {
            new InformationObject(DOUBLE_POINT_ADDRESS, new InformationElement[][] {
                { new IeDoublePointWithQuality(IeDoublePointWithQuality.DoublePointInformation.ON, false, false, false, false) }
            })
        };
        serverConnection.queueClass2Response(new ASdu(
            ASduType.M_DP_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
            false, false, 0, asdu.getCommonAddress(), doublePoints
        ));
        
        // Send measurements as Class 2 (low priority)
        InformationObject[] measurements = new InformationObject[] {
            new InformationObject(MEASUREMENT_ADDRESS, new InformationElement[][] {
                { new IeScaledValue(SCALED_VALUE), new IeQuality(false, false, false, false, false) },
                { new IeScaledValue(-5432), new IeQuality(false, false, false, false, false) },
                { new IeScaledValue(10000), new IeQuality(false, false, false, false, false) }
            })
        };
        serverConnection.queueClass2Response(new ASdu(
            ASduType.M_ME_NB_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
            false, false, 0, asdu.getCommonAddress(), measurements
        ));
        
        // Queue termination as Class 1 (high priority)
        serverConnection.queueClass1Response(
            IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress())
        );
    }

    private void handleCounterInterrogation(ASdu asdu) {
        // Queue confirmation
        serverConnection.queueClass1Response(
            IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress())
        );
        
        // Send counter values
        InformationObject[] counters = new InformationObject[] {
            new InformationObject(COUNTER_ADDRESS, new InformationElement[][] {
                { new IeBinaryCounterReading(COUNTER_VALUE, 0) }
            })
        };
        serverConnection.queueClass2Response(new ASdu(
            ASduType.M_IT_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
            false, false, 0, asdu.getCommonAddress(), counters
        ));
        
        // Queue termination
        serverConnection.queueClass1Response(
            IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress())
        );
    }

    private void handleClockSync(ASdu asdu) {
        InformationObject io = asdu.getInformationObjects()[0];
        IeTime56 time = (IeTime56) io.getInformationElements()[0][0];
        
        serverConnection.queueClass1Response(new ASdu(
            ASduType.C_CS_NA_1, false, CauseOfTransmission.ACTIVATION_CON,
            false, false, asdu.getOriginatorAddress(), asdu.getCommonAddress(),
            new InformationObject(0, time)
        ));
    }

    private void handleReadCommand(ASdu asdu) {
        InformationObject io = asdu.getInformationObjects()[0];
        int address = io.getInformationObjectAddress();
        
        if (address == READ_COMMAND_ADDRESS) {
            InformationObject responseObj = new InformationObject(address, new InformationElement[][] {
                { new IeNormalizedValue(NORMALIZED_VALUE), new IeQuality(false, false, false, false, false) }
            });
            
            serverConnection.queueClass1Response(new ASdu(
                ASduType.M_ME_NA_1, false, CauseOfTransmission.REQUEST,
                false, false, 0, asdu.getCommonAddress(), responseObj
            ));
        }
    }

    @Override
    public void onConnectionLost(IOException cause) {
        this.connectionLost = true;
        this.connectionReady = false;
    }

    public void setServerConnection(IEC60870Connection connection) {
        this.serverConnection = (Iec101ServerConnection) connection;
    }

    private boolean isCommand(ASduType type) {
        return type.getId() >= 45 && type.getId() <= 113; // Command range
    }

    public boolean isConnectionReady() {
        return connectionReady;
    }

    public boolean isConnectionLost() {
        return connectionLost;
    }

    public ConcurrentLinkedQueue<ASduType> getReceivedAsdus() {
        return receivedAsdus;
    }

    public int getReceivedCommandCount() {
        return receivedCommandCount.get();
    }

    public ASduType getLastReceivedAsdu() {
        return receivedAsdus.isEmpty() ? null : receivedAsdus.peek();
    }
    
    @Override
    public boolean hasReceived(ASduType type) {
        return receivedAsdus.contains(type);
    }
    
    @Override
    public boolean hasReceivedAnyCommand() {
        return receivedCommandCount.get() > 0;
    }
    
}