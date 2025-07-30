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
import net.sympower.iec60870.common.elements.IeQualifierOfSetPointCommand;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeSinglePointWithQuality;
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
                case C_SE_NA_1:
                    handleSetPointCommand(asdu);
                    break;
                default:
                    System.out.println("Unsupported ASDU type: " + asdu.getTypeIdentification());
            }
        } catch (IOException | InterruptedException e) {
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
        System.out.println("Got interrogation command (C_IC_NA_1)");

        connection.sendConfirmation(asdu);

        // Single point measurement
        connection.send(new ASdu(
                ASduType.M_SP_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                false, false, 0, asdu.getCommonAddress(),
                new InformationObject(1, new InformationElement[][] {
                        { new IeSinglePointWithQuality(true, false, false, false, false) }
                })
        ));

        // Scaled measurement
        connection.send(new ASdu(
                ASduType.M_ME_NB_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                false,
                false, 0, asdu.getCommonAddress(),
                new InformationObject(2, new InformationElement[][] {
                        { new IeScaledValue(1234),
                                new IeQuality(false, false, false, false, false) }
                })
        ));

        connection.send(new ASdu(
                ASduType.M_ME_NA_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                false, false, 0, asdu.getCommonAddress(),
                new InformationObject(3, new InformationElement[][] {
                        { new IeNormalizedValue(0.85),
                                new IeQuality(false, false, false, false, false) }
                })
        ));

        connection.sendTermination(asdu);
    }

    private void handleSingleCommand(ASdu asdu) throws IOException, InterruptedException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        IeSingleCommand singleCommand = (IeSingleCommand) infoObj.getInformationElements()[0][0];
        int ioa = infoObj.getInformationObjectAddress();

        System.out.println("Got single command (C_SC_NA_1) for IOA: " + ioa);
        System.out.println("  State: " + (singleCommand.isCommandStateOn() ? "ON" : "OFF"));
        System.out.println("  Select: " + singleCommand.isSelect());
        System.out.println("  Qualifier: " + singleCommand.getQualifier());

        // Handle select before operate logic
        if (singleCommand.isSelect()) {
            System.out.println("Single command with select=true. Storing selection.");
            selected = true;
            // In a real implementation, you would store the command details per IOA
        } else {
            if (selected || ioa != 5000) {
                // Execute immediately for non-5000 IOAs or if previously selected
                System.out.println("Single command with select=false. Executing command.");
                // In a real implementation, you would apply the command here
                selected = false;
            } else {
                System.out.println("Single command with select=false for IOA 5000, but no command selected.");
            }
        }
        
        // Always send confirmation for received commands
        connection.sendConfirmation(asdu);
        
        // Some clients expect a spontaneous update after a command
        // Send the new state back as a single point information
        if (!singleCommand.isSelect()) {
            // Only send update after execution, not after selection
            Thread.sleep(50); // Small delay
            
            connection.send(new ASdu(
                    ASduType.M_SP_NA_1,
                    false,
                    CauseOfTransmission.SPONTANEOUS,
                    asdu.isTestFrame(),
                    false,
                    asdu.getOriginatorAddress(),
                    asdu.getCommonAddress(),
                    new InformationObject(ioa, new InformationElement[][] {
                            { new IeSinglePointWithQuality(singleCommand.isCommandStateOn(), false, false, false, false) }
                    })
            ));
        }
    }


    private void handleCounterInterrogation(ASdu asdu) throws IOException, InterruptedException {
        System.out.println("Got counter interrogation command (C_CI_NA_1).");

        connection.sendConfirmation(asdu);

        connection.send(new ASdu(
                ASduType.M_IT_NA_1,
                true,
                CauseOfTransmission.INTERROGATED_BY_STATION,
                false, false, 0,
                asdu.getCommonAddress(),
                new InformationObject(1, new InformationElement[][] {
                        { new IeBinaryCounterReading(12345, 1) }
                })
        ));

        connection.sendTermination(asdu);
    }


    private void handleClockSync(ASdu asdu) throws IOException, InterruptedException {
        IeTime56 currentTime = new IeTime56(System.currentTimeMillis());
        System.out.println("Got Clock synchronization command (C_CS_NA_1). Send current time: " + currentTime);

        // For clock sync, we need to update the information element with current time
        InformationObject updatedInfoObj = new InformationObject(
                asdu.getInformationObjects()[0].getInformationObjectAddress(),
                new InformationElement[][] {{ currentTime }}
        );

        // 1. Send activation confirmation with current time
        connection.send(new ASdu(
                ASduType.C_CS_NA_1,
                asdu.isSequenceOfElements(),
                CauseOfTransmission.ACTIVATION_CON,
                asdu.isTestFrame(),
                false,
                asdu.getOriginatorAddress(),
                asdu.getCommonAddress(),
                new InformationObject[] { updatedInfoObj }
        ));

        // 2. Send activation termination
        connection.send(new ASdu(
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
        
        // Send the requested measurement value
        connection.send(new ASdu(
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
    
    private void handleSetPointCommand(ASdu asdu) throws IOException, InterruptedException {
        InformationObject infoObj = asdu.getInformationObjects()[0];
        int ioa = infoObj.getInformationObjectAddress();
        
        // Extract the normalized value and qualifier
        IeNormalizedValue normalizedValue = (IeNormalizedValue) infoObj.getInformationElements()[0][0];
        IeQualifierOfSetPointCommand qualifier = (IeQualifierOfSetPointCommand) infoObj.getInformationElements()[0][1];
        
        System.out.println("Got set point command (C_SE_NA_1) for IOA: " + ioa);
        System.out.println("  Value: " + normalizedValue.getNormalizedValue());
        System.out.println("  Select: " + qualifier.isSelect());
        System.out.println("  Qualifier: " + qualifier.getQl());
        
        // Check if it's select before operate
        if (qualifier.isSelect()) {
            System.out.println("Set point command with select=true. Storing selection.");
            // In a real implementation, you would store the selected value
            // and wait for the execute command
        } else {
            System.out.println("Set point command with select=false. Executing set point.");
            // In a real implementation, you would apply the set point here
            // For this example, we just acknowledge it
        }
        
        // Send confirmation back with same information objects
        System.out.println("Sending confirmation for C_SE_NA_1 to common address: " + asdu.getCommonAddress());
        connection.sendConfirmation(asdu);
        
        // Small delay to ensure the confirmation is processed before client polls
        Thread.sleep(50);
        
        System.out.println("Set point command handling complete");
    }
}
