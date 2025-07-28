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

    private static final int SINGLE_POINT_IOA = 1;
    private static final int SCALED_MEASUREMENT_IOA = 2;
    private static final int NORMALIZED_MEASUREMENT_IOA = 3;
    private static final int COUNTER_IOA = 1;
    
    private static final int SAMPLE_SCALED_VALUE = 1234;
    private static final double SAMPLE_NORMALIZED_VALUE = 0.85;
    private static final double SAMPLE_READ_VALUE = 0.75;
    private static final int SAMPLE_COUNTER_VALUE = 12345;
    private static final int SAMPLE_COUNTER_SEQUENCE = 1;
    
    private static final int SERVER_ORIGINATOR_ADDRESS = 0;

    private final Iec101ServerConnection iec101Connection;
    private boolean selected = false;

    public SampleServerEventListener(IEC60870Connection connection) {
        this.iec101Connection = (Iec101ServerConnection) connection;
    }

    @Override
    public void onConnectionReady() {
        System.out.println("[SampleServer] IEC-101 connection ready for data transfer");
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
                    System.out.println("[SampleServer] Unsupported ASDU type: " + asdu.getTypeIdentification());
            }
        } catch (Exception e) {
            System.err.println("[SampleServer] Error handling ASDU: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionLost(IOException cause) {
        if (cause != null) {
            System.err.println("[SampleServer] Connection lost: " + cause.getMessage());
        } else {
            System.out.println("[SampleServer] Connection closed normally");
        }
        // Reset state on connection loss
        selected = false;
    }

    private void handleInterrogation(ASdu asdu) {
        System.out.println("[SampleServer] Processing interrogation command (C_IC_NA_1)");

        queueConfirmation(asdu);
        
        sendInterrogationSinglePoint(asdu);
        sendInterrogationScaledMeasurement(asdu);
        sendInterrogationNormalizedMeasurement(asdu);
        
        queueTermination(asdu);
    }
    
    private void sendInterrogationSinglePoint(ASdu asdu) {
        ASdu response = buildInterrogationResponse(
            ASduType.M_SP_NA_1, 
            SINGLE_POINT_IOA,
            createSinglePointInfo(true),
            asdu.getCommonAddress()
        );
        iec101Connection.queueClass1Response(response);
    }
    
    private void sendInterrogationScaledMeasurement(ASdu asdu) {
        ASdu response = buildInterrogationResponse(
            ASduType.M_ME_NB_1,
            SCALED_MEASUREMENT_IOA, 
            createScaledMeasurement(SAMPLE_SCALED_VALUE),
            asdu.getCommonAddress()
        );
        iec101Connection.queueClass1Response(response);
    }
    
    private void sendInterrogationNormalizedMeasurement(ASdu asdu) {
        ASdu response = buildInterrogationResponse(
            ASduType.M_ME_NA_1,
            NORMALIZED_MEASUREMENT_IOA,
            createNormalizedMeasurement(SAMPLE_NORMALIZED_VALUE),
            asdu.getCommonAddress()
        );
        iec101Connection.queueClass1Response(response);
    }

    private void handleSingleCommand(ASdu asdu) throws IOException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        IeSingleCommand singleCommand = (IeSingleCommand) infoObj.getInformationElements()[0][0];
        int ioa = infoObj.getInformationObjectAddress();

        System.out.println("[SampleServer] Processing single command (C_SC_NA_1) for IOA: " + ioa + 
                          ", State: " + (singleCommand.isCommandStateOn() ? "ON" : "OFF") +
                          ", Select: " + singleCommand.isSelect());

        handleSelectExecuteCommand(asdu, singleCommand.isSelect());
    }

    private void handleDoubleCommand(ASdu asdu) throws IOException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        IeDoubleCommand doubleCommand = (IeDoubleCommand) infoObj.getInformationElements()[0][0];
        int ioa = infoObj.getInformationObjectAddress();

        System.out.println("[SampleServer] Processing double command (C_DC_NA_1) for IOA: " + ioa + 
                          ", State: " + doubleCommand.getCommandState() +
                          ", Select: " + doubleCommand.isSelect());

        handleSelectExecuteCommand(asdu, doubleCommand.isSelect());
    }


    private void handleCounterInterrogation(ASdu asdu) {
        System.out.println("[SampleServer] Processing counter interrogation command (C_CI_NA_1)");

        queueConfirmation(asdu);

        ASdu response = buildInterrogationResponse(
            ASduType.M_IT_NA_1,
            COUNTER_IOA,
            createCounterReading(SAMPLE_COUNTER_VALUE, SAMPLE_COUNTER_SEQUENCE),
            asdu.getCommonAddress()
        );
        iec101Connection.queueClass1Response(response);

        queueTermination(asdu);
    }


    private void handleClockSync(ASdu asdu) {
        IeTime56 currentTime = new IeTime56(System.currentTimeMillis());
        System.out.println("[SampleServer] Processing clock synchronization command (C_CS_NA_1)");

        int originalIoa = asdu.getInformationObjects()[0].getInformationObjectAddress();
        InformationElement[] timeElements = new InformationElement[] { currentTime };
        
        ASdu confirmation = buildClockSyncResponse(asdu, timeElements, originalIoa, CauseOfTransmission.ACTIVATION_CON);
        iec101Connection.queueClass1Response(confirmation);

        ASdu termination = buildClockSyncResponse(asdu, timeElements, originalIoa, CauseOfTransmission.ACTIVATION_TERMINATION);
        iec101Connection.queueClass1Response(termination);
    }
    
    private void handleReadCommand(ASdu asdu) {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        int ioa = infoObj.getInformationObjectAddress();
        
        System.out.println("[SampleServer] Processing read command (C_RD_NA_1) for IOA: " + ioa);
        
        ASdu response = buildMeasurementResponse(
            ASduType.M_ME_NA_1, 
            ioa,
            new InformationElement[] { new IeNormalizedValue(SAMPLE_READ_VALUE), createGoodQuality() },
            CauseOfTransmission.REQUEST,
            asdu.getCommonAddress()
        );
        iec101Connection.queueClass2Response(response);
    }
    
    private void handleSetPointCommand(ASdu asdu) throws IOException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        int ioa = infoObj.getInformationObjectAddress();
        
        IeNormalizedValue normalizedValue = (IeNormalizedValue) infoObj.getInformationElements()[0][0];
        IeQualifierOfSetPointCommand qualifier = (IeQualifierOfSetPointCommand) infoObj.getInformationElements()[0][1];
        
        System.out.println("[SampleServer] Processing set point command (C_SE_NA_1) for IOA: " + ioa + 
                          ", Value: " + normalizedValue.getNormalizedValue() +
                          ", Select: " + qualifier.isSelect());
        
        handleSelectExecuteCommand(asdu, qualifier.isSelect());
    }
    
    private void handleSelectExecuteCommand(ASdu asdu, boolean isSelect) throws IOException {
        if (isSelect) {
            selected = true;
            queueConfirmation(asdu);
        } else {
            selected = false;
            queueConfirmation(asdu);
            queueTermination(asdu);
        }
    }
    
    private void queueConfirmation(ASdu asdu) {
        iec101Connection.queueClass1Response(IEC60870Protocol.createConfirmation(asdu, asdu.getOriginatorAddress()));
    }
    
    private void queueTermination(ASdu asdu) {
        iec101Connection.queueClass1Response(IEC60870Protocol.createTermination(asdu, asdu.getOriginatorAddress()));
    }
    
    private IeQuality createGoodQuality() {
        return new IeQuality(false, false, false, false, false);
    }
    
    private ASdu buildMeasurementResponse(ASduType type, int ioa, InformationElement[] elements,
                                        CauseOfTransmission cot, int commonAddress) {
        return new ASdu(type, false, cot, false, false, SERVER_ORIGINATOR_ADDRESS, 
                       commonAddress, new InformationObject(ioa, new InformationElement[][] { elements }));
    }
    
    private ASdu buildInterrogationResponse(ASduType type, int ioa, InformationElement[] elements, int commonAddress) {
        return new ASdu(type, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                       false, false, SERVER_ORIGINATOR_ADDRESS, commonAddress,
                       new InformationObject(ioa, new InformationElement[][] { elements }));
    }
    
    private InformationElement[] createSinglePointInfo(boolean value) {
        return new InformationElement[] { new IeSinglePointWithQuality(value, false, false, false, false) };
    }
    
    private InformationElement[] createScaledMeasurement(int value) {
        return new InformationElement[] { new IeScaledValue(value), createGoodQuality() };
    }
    
    private InformationElement[] createNormalizedMeasurement(double value) {
        return new InformationElement[] { new IeNormalizedValue(value), createGoodQuality() };
    }
    
    private InformationElement[] createCounterReading(int value, int sequence) {
        return new InformationElement[] { new IeBinaryCounterReading(value, sequence) };
    }
    
    private ASdu buildClockSyncResponse(ASdu originalAsdu, InformationElement[] timeElements, int ioa, CauseOfTransmission cot) {
        return new ASdu(
            ASduType.C_CS_NA_1,
            originalAsdu.isSequenceOfElements(),
            cot,
            originalAsdu.isTestFrame(),
            false,
            originalAsdu.getOriginatorAddress(),
            originalAsdu.getCommonAddress(),
            new InformationObject[] { new InformationObject(ioa, new InformationElement[][] { timeElements }) }
        );
    }
}
