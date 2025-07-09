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
import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.elements.IeBinaryCounterReading;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQuality;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationElement;
import net.sympower.iec60870.common.elements.InformationObject;

import java.io.IOException;

public class SampleServerEventListener implements IEC60870EventListener {

    private final IEC60870Connection connection;
    private boolean selected = false;

    public SampleServerEventListener(IEC60870Connection connection) {
        this.connection = connection;
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
                case C_CS_NA_1:
                    handleClockSync(asdu);
                    break;
                case C_CI_NA_1:
                    handleCounterInterrogation(asdu);
                    break;
                case C_RD_NA_1:
                    handleReadCommand(asdu);
                    break;
                default:
                    System.out.println("Unsupported ASDU type: " + asdu.getTypeIdentification());
            }
        } catch (IOException e) {
            System.err.println("Error handling ASDU: " + e.getMessage());
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
        System.out.println("Got interrogation command (C_IC_NA_1). Will send scaled measured values.");
        
        connection.sendConfirmation(asdu);
        connection.send(new ASdu(
            ASduType.M_ME_NB_1, true, CauseOfTransmission.INTERROGATED_BY_STATION, false,
            false, 0, asdu.getCommonAddress(),
            new InformationObject(1, new InformationElement[][] {
                                     { new IeScaledValue(-32768), new IeQuality(true, true, true, true, true) },
                                     { new IeScaledValue(10), new IeQuality(true, true, true, true, true) },
                                     { new IeScaledValue(-5), new IeQuality(true, true, true, true, true) } })));
        
        System.out.println("Sent measured values response");
    }

    private void handleSingleCommand(ASdu asdu) throws IOException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        IeSingleCommand singleCommand = (IeSingleCommand) infoObj.getInformationElements()[0][0];

        if (infoObj.getInformationObjectAddress() == 5000) {
            if (singleCommand.isSelect()) {
                System.out.println("Got single command (C_SC_NA_1) with select=true. Select command.");
                selected = true;
            } else if (!singleCommand.isSelect() && selected) {
                System.out.println("Got single command (C_SC_NA_1) with select=false. Execute selected command.");
                selected = false;
            } else {
                System.out.println("Got single command (C_SC_NA_1) with select=false. But no command is selected, no execution.");
            }
            
            connection.sendConfirmation(asdu);
        }
    }

    private void handleClockSync(ASdu asdu) throws IOException {
        IeTime56 currentTime = new IeTime56(System.currentTimeMillis());
        System.out.println("Got Clock synchronization command (C_CS_NA_1). Send current time: " + currentTime);
        connection.synchronizeClocksResponse(asdu.getCommonAddress(), currentTime);
    }
    
    private void handleCounterInterrogation(ASdu asdu) throws IOException {
        System.out.println("Got counter interrogation command (C_CI_NA_1). Sending sample counter values.");
        
        connection.sendConfirmation(asdu);
        connection.send(new ASdu(ASduType.M_IT_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION, false,
                                 false, 0, asdu.getCommonAddress(),
                                 new InformationObject(200, new InformationElement[][] {
                                     { new IeBinaryCounterReading(12345, 1) },
                                     { new IeBinaryCounterReading(67890, 2) } })));
    }
    
    private void handleReadCommand(ASdu asdu) throws IOException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        int ioa = infoObj.getInformationObjectAddress();
        
        System.out.println("Got read command (C_RD_NA_1) for IOA: " + ioa);
        
        connection.send(new ASdu(ASduType.M_ME_NA_1, false, CauseOfTransmission.REQUEST, false,
                                 false, 0, asdu.getCommonAddress(),
                                 new InformationObject(ioa, new InformationElement[][] {
                                     { new IeNormalizedValue(0.75),
                                       new IeQuality(false, false, false, false, false) } })));
    }
}
