/*
 * This file is part of the enhanced IEC 60870 library.
 * Original project: https://github.com/openmuc/j60870
 * Enhanced version: https://github.com/josh-mills-sympower/IEC-60870-5
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package net.sympower.iec60870.app.common;

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.IEC60870Protocol;
import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.elements.IeBinaryCounterReading;
import net.sympower.iec60870.common.elements.IeDoubleCommand;
import net.sympower.iec60870.common.elements.IeDoubleCommand.DoubleCommandState;
import net.sympower.iec60870.common.elements.IeDoublePointWithQuality;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQuality;
import net.sympower.iec60870.common.elements.IeQualifierOfSetPointCommand;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeSinglePointWithQuality;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationElement;
import net.sympower.iec60870.common.elements.InformationObject;
import net.sympower.iec60870.iec101.connection.Iec101ServerConnection;

import java.io.IOException;

public class SampleServerEventListener implements IEC60870EventListener {

    private final IEC60870Connection connection;
    private final Iec101ServerConnection iec101Connection;
    private boolean selected = false;

    public SampleServerEventListener(IEC60870Connection connection) {
        this.connection = connection;
        this.iec101Connection = (Iec101ServerConnection) connection;
    }

    @Override
    public void onConnectionReady() {
        System.out.println("Connection ready for data transfer");
    }

    @Override
    public void onAsduReceived(ASdu asdu) {
        try {
            switch (asdu.getTypeIdentification()) {
                case C_IC_NA_1:
                    handleInterrogation(asdu);
                    break;
                case C_SC_NA_1:
                    handleSingleCommand(asdu);
                    break;
                case C_DC_NA_1:
                    handleDoubleCommand(asdu);
                    break;
                case C_CS_NA_1:
                    handleClockSync(asdu);
                    break;
                case C_CI_NA_1:
                    handleCounterInterrogation(asdu);
                    break;
                case C_RD_NA_1:
                    handleReadCommand(asdu);
                    break;
                case C_SE_NA_1:
                    handleSetPointCommand(asdu);
                    break;
                default:
                    System.out.println("Unsupported ASDU type: " + asdu.getTypeIdentification());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error handling ASDU: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionLost(IOException cause) {
        if (cause != null) {
            System.err.println("Connection lost: " + cause.getMessage());
        } else {
            System.out.println("Connection closed normally");
        }
    }

    private void handleInterrogation(ASdu asdu) throws IOException {
        System.out.println("Got interrogation command (C_IC_NA_1)");

        iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));

        iec101Connection.queueClass2Response(new ASdu(
                ASduType.M_SP_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                false, false, 0, asdu.getCommonAddress(),
                new InformationObject(1, new InformationElement[][] {
                        { new IeSinglePointWithQuality(true, false, false, false, false) }
                })
        ));

        iec101Connection.queueClass2Response(new ASdu(
                ASduType.M_ME_NB_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                false,
                false, 0, asdu.getCommonAddress(),
                new InformationObject(2, new InformationElement[][] {
                        { new IeScaledValue(1234),
                                new IeQuality(false, false, false, false, false) }
                })
        ));

        iec101Connection.queueClass2Response(new ASdu(
                ASduType.M_ME_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                false, false, 0, asdu.getCommonAddress(),
                new InformationObject(3, new InformationElement[][] {
                        { new IeNormalizedValue(0.85),
                                new IeQuality(false, false, false, false, false) }
                })
        ));

        iec101Connection.queueClass1Response(IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress()));
    }

    private void handleSingleCommand(ASdu asdu) throws IOException, InterruptedException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        IeSingleCommand singleCommand = (IeSingleCommand) infoObj.getInformationElements()[0][0];
        int ioa = infoObj.getInformationObjectAddress();

        System.out.println("Got single command (C_SC_NA_1) for IOA: " + ioa + 
                          ", State: " + (singleCommand.isCommandStateOn() ? "ON" : "OFF") +
                          ", Select: " + singleCommand.isSelect());

        if (singleCommand.isSelect()) {
            selected = true;
            
            iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));
        } else {
            selected = false;
            
            iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));
            iec101Connection.queueClass1Response(IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress()));
        }
    }

    private void handleDoubleCommand(ASdu asdu) throws IOException, InterruptedException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        IeDoubleCommand doubleCommand = (IeDoubleCommand) infoObj.getInformationElements()[0][0];
        int ioa = infoObj.getInformationObjectAddress();

        System.out.println("Got double command (C_DC_NA_1) for IOA: " + ioa + 
                          ", State: " + doubleCommand.getCommandState() +
                          ", Select: " + doubleCommand.isSelect());

        if (doubleCommand.isSelect()) {
            selected = true;
            
            iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));
        } else {
            selected = false;
            
            iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));
            iec101Connection.queueClass1Response(IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress()));
        }
    }


    private void handleCounterInterrogation(ASdu asdu) throws IOException, InterruptedException {
        System.out.println("Got counter interrogation command (C_CI_NA_1)");

        iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));

        iec101Connection.queueClass2Response(new ASdu(
                ASduType.M_IT_NA_1,
                true,
                CauseOfTransmission.INTERROGATED_BY_STATION,
                false, false, 0,
                asdu.getCommonAddress(),
                new InformationObject(1, new InformationElement[][] {
                        { new IeBinaryCounterReading(12345, 1) }
                })
        ));

        iec101Connection.queueClass1Response(IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress()));
    }


    private void handleClockSync(ASdu asdu) throws IOException, InterruptedException {
        IeTime56 currentTime = new IeTime56(System.currentTimeMillis());
        System.out.println("Got clock synchronization command (C_CS_NA_1)");

        InformationObject updatedInfoObj = new InformationObject(
                asdu.getInformationObjects()[0].getInformationObjectAddress(),
                new InformationElement[][] {{ currentTime }}
        );

        iec101Connection.queueClass1Response(new ASdu(
                ASduType.C_CS_NA_1,
                asdu.isSequenceOfElements(),
                CauseOfTransmission.ACTIVATION_CON,
                asdu.isTestFrame(),
                false,
                asdu.getOriginatorAddress(),
                asdu.getCommonAddress(),
                new InformationObject[] { updatedInfoObj }
        ));

        iec101Connection.queueClass1Response(new ASdu(
                ASduType.C_CS_NA_1,
                asdu.isSequenceOfElements(),
                CauseOfTransmission.ACTIVATION_TERMINATION,
                asdu.isTestFrame(),
                false,
                asdu.getOriginatorAddress(),
                asdu.getCommonAddress(),
                new InformationObject[] { updatedInfoObj }
        ));
    }
    
    private void handleReadCommand(ASdu asdu) throws IOException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        int ioa = infoObj.getInformationObjectAddress();
        
        System.out.println("Got read command (C_RD_NA_1) for IOA: " + ioa);
        
        iec101Connection.queueClass2Response(new ASdu(
                ASduType.M_ME_NA_1, 
                false, 
                CauseOfTransmission.REQUEST,
                asdu.isTestFrame(),
                false, 
                asdu.getOriginatorAddress(), 
                asdu.getCommonAddress(),
                new InformationObject(ioa, new InformationElement[][] {
                        { new IeNormalizedValue(0.75),
                          new IeQuality(false, false, false, false, false) }
                })
        ));
    }
    
    private void handleSetPointCommand(ASdu asdu) {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        int ioa = infoObj.getInformationObjectAddress();
        
        IeNormalizedValue normalizedValue = (IeNormalizedValue) infoObj.getInformationElements()[0][0];
        IeQualifierOfSetPointCommand qualifier = (IeQualifierOfSetPointCommand) infoObj.getInformationElements()[0][1];
        
        System.out.println("Got set point command (C_SE_NA_1) for IOA: " + ioa + 
                          ", Value: " + normalizedValue.getNormalizedValue() +
                          ", Select: " + qualifier.isSelect());
        
        if (qualifier.isSelect()) {
            selected = true;
            
            iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));
        } else {
            selected = false;
            
            iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));
            iec101Connection.queueClass1Response(IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress()));
        }
    }
}
