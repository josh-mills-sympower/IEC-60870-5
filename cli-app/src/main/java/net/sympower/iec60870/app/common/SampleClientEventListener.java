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
import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.elements.IeBinaryCounterReading;
import net.sympower.iec60870.common.elements.IeDoublePointWithQuality;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQuality;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeSinglePointWithQuality;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationElement;
import net.sympower.iec60870.common.elements.InformationObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SampleClientEventListener implements IEC60870EventListener {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private volatile boolean commandPending = false;
    private volatile String lastCommandType = "";

    @Override
    public void onConnectionReady() {
        System.out.println("\n[Client] ✓ IEC-101 connection established and ready");
        System.out.println("[Client] ℹ  Automatic polling - responses will arrive continuously");
    }

    @Override
    public void onAsduReceived(ASdu asdu) {
        String timestamp = timeFormat.format(new Date());
        System.out.println("\n[" + timestamp + "] Received ASDU:");
        interpretAsdu(asdu);
        
        if (isConfirmationOrTermination(asdu)) {
            commandPending = false;
        }
    }

    @Override
    public void onConnectionLost(IOException cause) {
        if (cause != null) {
            System.err.println("\n[Client] ✗ Connection lost: " + cause.getMessage());
        } else {
            System.out.println("\n[Client] Connection closed normally");
        }
        commandPending = false;
    }
    
    public void onCommandSent(String commandType) {
        commandPending = true;
        lastCommandType = commandType;
        System.out.println("\n[Client] → " + commandType + " sent");
        System.out.println("[Client] ℹ  Response will arrive automatically via periodic polling");
    }

    private void interpretAsdu(ASdu asdu) {
        ASduType type = asdu.getTypeIdentification();
        CauseOfTransmission cot = asdu.getCauseOfTransmission();
        
        System.out.println("  Type: " + type + " (" + getReadableAsduType(type) + ")");
        System.out.println("  Cause: " + cot + " (" + getReadableCause(cot) + ")");
        System.out.println("  Common Address: " + asdu.getCommonAddress());
        
        switch (type) {
            case C_IC_NA_1:
                interpretInterrogationCommand(asdu, cot);
                break;
            case C_SC_NA_1:
                interpretSingleCommand(asdu, cot);
                break;
            case C_CS_NA_1:
                interpretClockSync(asdu, cot);
                break;
            case M_SP_NA_1:
                interpretSinglePointInfo(asdu);
                break;
            case M_DP_NA_1:
                interpretDoublePointInfo(asdu);
                break;
            case M_ME_NA_1:
                interpretNormalizedMeasurement(asdu);
                break;
            case M_ME_NB_1:
                interpretScaledMeasurement(asdu);
                break;
            case M_IT_NA_1:
                interpretCounterReading(asdu);
                break;
            default:
                System.out.println("  ℹ  Raw ASDU: " + asdu);
                break;
        }
    }
    
    private void interpretInterrogationCommand(ASdu asdu, CauseOfTransmission cot) {
        switch (cot) {
            case ACTIVATION_CON:
                System.out.println("  ✓ Interrogation confirmed by server");
                System.out.println("  → Measurements will arrive automatically");
                break;
            case ACTIVATION_TERMINATION:
                System.out.println("  ✓ Interrogation completed");
                break;
            default:
                System.out.println("  ⚠  Unexpected cause for interrogation: " + cot);
        }
    }
    
    private void interpretSingleCommand(ASdu asdu, CauseOfTransmission cot) {
        if (asdu.getInformationObjects() != null && asdu.getInformationObjects().length > 0) {
            InformationObject io = asdu.getInformationObjects()[0];
            IeSingleCommand cmd = (IeSingleCommand) io.getInformationElements()[0][0];
            
            switch (cot) {
                case ACTIVATION_CON:
                    System.out.println("  ✓ Single command confirmed - IOA: " + io.getInformationObjectAddress() + 
                                     ", State: " + (cmd.isCommandStateOn() ? "ON" : "OFF"));
                    break;
                case ACTIVATION_TERMINATION:
                    System.out.println("  ✓ Single command executed - IOA: " + io.getInformationObjectAddress());
                    break;
                default:
                    System.out.println("  ⚠  Unexpected cause for single command: " + cot);
            }
        }
    }
    
    private void interpretClockSync(ASdu asdu, CauseOfTransmission cot) {
        if (asdu.getInformationObjects() != null && asdu.getInformationObjects().length > 0) {
            InformationObject io = asdu.getInformationObjects()[0];
            IeTime56 time = (IeTime56) io.getInformationElements()[0][0];
            
            switch (cot) {
                case ACTIVATION_CON:
                    System.out.println("  ✓ Clock sync confirmed - Server time: " + new Date(time.getTimestamp()));
                    break;
                case ACTIVATION_TERMINATION:
                    System.out.println("  ✓ Clock sync completed");
                    break;
                default:
                    System.out.println("  ⚠  Unexpected cause for clock sync: " + cot);
            }
        }
    }
    
    private void interpretSinglePointInfo(ASdu asdu) {
        System.out.println("  📊 Single Point Information:");
        for (InformationObject io : asdu.getInformationObjects()) {
            for (InformationElement[] elements : io.getInformationElements()) {
                IeSinglePointWithQuality sp = (IeSinglePointWithQuality) elements[0];
                System.out.println("    IOA " + io.getInformationObjectAddress() + ": " + 
                                 (sp.isOn() ? "ON" : "OFF") + 
                                 (sp.isInvalid() ? " (INVALID)" : "") +
                                 (sp.isBlocked() ? " (BLOCKED)" : ""));
            }
        }
    }
    
    private void interpretDoublePointInfo(ASdu asdu) {
        System.out.println("  📊 Double Point Information:");
        for (InformationObject io : asdu.getInformationObjects()) {
            for (InformationElement[] elements : io.getInformationElements()) {
                IeDoublePointWithQuality dp = (IeDoublePointWithQuality) elements[0];
                System.out.println("    IOA " + io.getInformationObjectAddress() + ": " + dp.getDoublePointInformation() +
                                 (dp.isInvalid() ? " (INVALID)" : ""));
            }
        }
    }
    
    private void interpretNormalizedMeasurement(ASdu asdu) {
        System.out.println("  📊 Normalized Measurements:");
        for (InformationObject io : asdu.getInformationObjects()) {
            for (InformationElement[] elements : io.getInformationElements()) {
                IeNormalizedValue value = (IeNormalizedValue) elements[0];
                IeQuality quality = elements.length > 1 ? (IeQuality) elements[1] : null;
                System.out.println("    IOA " + io.getInformationObjectAddress() + ": " + 
                                 String.format("%.3f", value.getNormalizedValue()) +
                                 (quality != null && quality.isInvalid() ? " (INVALID)" : ""));
            }
        }
    }
    
    private void interpretScaledMeasurement(ASdu asdu) {
        System.out.println("  📊 Scaled Measurements:");
        for (InformationObject io : asdu.getInformationObjects()) {
            for (InformationElement[] elements : io.getInformationElements()) {
                IeScaledValue value = (IeScaledValue) elements[0];
                IeQuality quality = elements.length > 1 ? (IeQuality) elements[1] : null;
                System.out.println("    IOA " + io.getInformationObjectAddress() + ": " + 
                                 value.getUnnormalizedValue() +
                                 (quality != null && quality.isInvalid() ? " (INVALID)" : ""));
            }
        }
    }
    
    private void interpretCounterReading(ASdu asdu) {
        System.out.println("  📊 Counter Readings:");
        for (InformationObject io : asdu.getInformationObjects()) {
            for (InformationElement[] elements : io.getInformationElements()) {
                IeBinaryCounterReading counter = (IeBinaryCounterReading) elements[0];
                System.out.println("    IOA " + io.getInformationObjectAddress() + ": " + 
                                 counter.getCounterReading() + " (seq: " + counter.getSequenceNumber() + ")");
            }
        }
    }
    
    private boolean isConfirmationOrTermination(ASdu asdu) {
        CauseOfTransmission cot = asdu.getCauseOfTransmission();
        return cot == CauseOfTransmission.ACTIVATION_CON || 
               cot == CauseOfTransmission.ACTIVATION_TERMINATION;
    }
    
    private String getReadableAsduType(ASduType type) {
        switch (type) {
            case C_IC_NA_1: return "Interrogation Command";
            case C_SC_NA_1: return "Single Command";
            case C_CS_NA_1: return "Clock Synchronization";
            case M_SP_NA_1: return "Single Point Information";
            case M_DP_NA_1: return "Double Point Information";
            case M_ME_NA_1: return "Normalized Measurement";
            case M_ME_NB_1: return "Scaled Measurement";
            case M_IT_NA_1: return "Counter Reading";
            default: return "Unknown";
        }
    }
    
    private String getReadableCause(CauseOfTransmission cot) {
        switch (cot) {
            case ACTIVATION: return "Activation";
            case ACTIVATION_CON: return "Activation Confirmation";
            case ACTIVATION_TERMINATION: return "Activation Termination";
            case INTERROGATED_BY_STATION: return "Interrogated by Station";
            case SPONTANEOUS: return "Spontaneous";
            case REQUEST: return "Request";
            default: return "Other";
        }
    }
}
