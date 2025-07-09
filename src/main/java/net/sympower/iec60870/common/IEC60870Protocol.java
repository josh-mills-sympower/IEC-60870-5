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
package net.sympower.iec60870.common;

import net.sympower.iec60870.common.elements.*;
import java.util.Collection;

/**
 * IEC 60870-5 protocol utility class that handles ASDU construction according to protocol rules.
 * This class encapsulates all protocol-specific logic for creating ASDUs, separating it from
 * the transport layer concerns. All methods are static and stateless.
 * 
 * @since 2.0
 */
public final class IEC60870Protocol {

    /**
     * An interrogation command (C_IC_NA_1, TI: 100).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu interrogation(int originatorAddress, int commonAddress, CauseOfTransmission cot,
                                    IeQualifierOfInterrogation qualifier) {
        return new ASdu(ASduType.C_IC_NA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(0, qualifier));
    }

    /**
     * A counter interrogation command (C_CI_NA_1, TI: 101).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu counterInterrogation(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                           IeQualifierOfCounterInterrogation qualifier) {
        return new ASdu(ASduType.C_CI_NA_1, false, cot, false, false, 
                       originatorAddress, commonAddress, new InformationObject(0, qualifier));
    }

    /**
     * A read command (C_RD_NA_1, TI: 102).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress
     *            the information object address.
     */
    public static ASdu read(int originatorAddress, int commonAddress, int informationObjectAddress) {
        return new ASdu(ASduType.C_RD_NA_1, false, CauseOfTransmission.REQUEST, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress));
    }

    /**
     * A clock synchronization command (C_CS_NA_1, TI: 103).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param time
     *            the time to be sent.
     */
    public static ASdu synchronizeClocks(int originatorAddress, int commonAddress, IeTime56 time) {
        return new ASdu(ASduType.C_CS_NA_1, false, CauseOfTransmission.ACTIVATION, false, false,
                       originatorAddress, commonAddress, new InformationObject(0, time));
    }

    // TODO check
    public static ASdu synchronizeClocksResponse(int originatorAddress, int commonAddress, IeTime56 time) {
        return new ASdu(ASduType.C_CS_NA_1, false, CauseOfTransmission.ACTIVATION_CON, false, false,
                       originatorAddress, commonAddress, new InformationObject(0, time));
    }


    /**
     * A test command (C_TS_NA_1, TI: 104).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     */
    public static ASdu testCommand(int originatorAddress, int commonAddress) {
        return new ASdu(ASduType.C_TS_NA_1, false, CauseOfTransmission.ACTIVATION, false, false, 
                       originatorAddress, commonAddress, new InformationObject(0, new IeFixedTestBitPattern()));
    }

    /**
     * A test command with time tag CP56Time2a (C_TS_TA_1, TI: 107).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param testSequenceCounter
     *            the value to be sent.
     * @param time
     *            the time to be sent.
     */
    public static ASdu testCommandWithTimeTag(int originatorAddress, int commonAddress, 
                                             IeTestSequenceCounter testSequenceCounter, IeTime56 time) {
        return new ASdu(ASduType.C_TS_TA_1, false, CauseOfTransmission.ACTIVATION, false, false,
                       originatorAddress, commonAddress, new InformationObject(0, testSequenceCounter, time));
    }

    /**
     * A reset process command (C_RP_NA_1, TI: 105).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu resetProcess(int originatorAddress, int commonAddress, int qualifier) {
        return new ASdu(ASduType.C_RP_NA_1, false, CauseOfTransmission.ACTIVATION, false, false,
                       originatorAddress, commonAddress, new InformationObject(0, new IeQualifierOfResetProcessCommand(qualifier)));
    }

    /**
     * A delay acquisition command (C_CD_NA_1, TI: 106).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and spontaneous.
     * @param delayTime
     *            the time to be sent.
     */    // TODO cot is passed in in old impl
    public static ASdu delayAcquisition(int originatorAddress, int commonAddress, int delayTime) {
        return new ASdu(ASduType.C_CD_NA_1, false, CauseOfTransmission.ACTIVATION, false, false,
                       originatorAddress, commonAddress, new InformationObject(0, new IeTime16(delayTime)));
    }
    
    /**
     * A single command (C_SC_NA_1, TI: 45).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param singleCommand
     *            the command to be sent.
     */
    public static ASdu singleCommand(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                    int informationObjectAddress, IeSingleCommand singleCommand) {
        return new ASdu(ASduType.C_SC_NA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, singleCommand));
    }

    /**
     * A single command with time tag CP56Time2a (C_SC_TA_1, TI: 58).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param singleCommand
     *            the command to be sent.
     * @param timeTag
     *            the time tag to be sent.
     */
    public static ASdu singleCommandWithTimeTag(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                               int informationObjectAddress, IeSingleCommand singleCommand, IeTime56 timeTag) {
        return new ASdu(ASduType.C_SC_TA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, singleCommand, timeTag));
    }

    /**
     * A double command (C_DC_NA_1, TI: 46).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param doubleCommand
     *            the command to be sent.
     */
    public static ASdu doubleCommand(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                    int informationObjectAddress, IeDoubleCommand doubleCommand) {
        return new ASdu(ASduType.C_DC_NA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, doubleCommand));
    }

    /**
     * A double command with time tag CP56Time2a (C_DC_TA_1, TI: 59).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param doubleCommand
     *            the command to be sent.
     * @param timeTag
     *            the time tag to be sent.
     */
    public static ASdu doubleCommandWithTimeTag(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                               int informationObjectAddress, IeDoubleCommand doubleCommand, IeTime56 timeTag) {
        return new ASdu(ASduType.C_DC_TA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, doubleCommand, timeTag));
    }

    /**
     * A regulating step command (C_RC_NA_1, TI: 47).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param regulatingStep
     *            the command to be sent.
     */
    public static ASdu regulatingStepCommand(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                            int informationObjectAddress, IeRegulatingStepCommand regulatingStep) {
        return new ASdu(ASduType.C_RC_NA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, regulatingStep));
    }

    /**
     * A regulating step command with time tag CP56Time2a (C_RC_TA_1, TI: 60).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param regulatingStep
     *            the command to be sent.
     * @param timeTag
     *            the time tag to be sent.
     */
    public static ASdu regulatingStepCommandWithTimeTag(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                                       int informationObjectAddress, IeRegulatingStepCommand regulatingStep, 
                                                       IeTime56 timeTag) {
        return new ASdu(ASduType.C_RC_TA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, regulatingStep, timeTag));
    }

    /**
     * A set-point command, normalized value (C_SE_NA_1, TI: 48).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param normalizedValue
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu setNormalizedValueCommand(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                                int informationObjectAddress, IeNormalizedValue normalizedValue, 
                                                IeQualifierOfSetPointCommand qualifier) {
        return new ASdu(ASduType.C_SE_NA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, normalizedValue, qualifier));
    }

    /**
     * A set-point command with time tag CP56Time2a, normalized value (C_SE_TA_1, TI: 61).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param normalizedValue
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent
     * @param timeTag
     *            the time tag to be sent.
     */
    public static ASdu setNormalizedValueCommandWithTimeTag(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                                           int informationObjectAddress, IeNormalizedValue normalizedValue, 
                                                           IeQualifierOfSetPointCommand qualifier, IeTime56 timeTag) {
        return new ASdu(ASduType.C_SE_TA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, normalizedValue, qualifier, timeTag));
    }

    /**
     * A set-point command, scaled value (C_SE_NB_1, TI: 49).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param scaledValue
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu setScaledValueCommand(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                            int informationObjectAddress, IeScaledValue scaledValue, 
                                            IeQualifierOfSetPointCommand qualifier) {
        return new ASdu(ASduType.C_SE_NB_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, scaledValue, qualifier));
    }

    /**
     * A set-point command with time tag CP56Time2a, scaled value (C_SE_TB_1, TI: 62).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param scaledValue
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     * @param timeTag
     *            the time tag to be sent.
     */
    public static ASdu setScaledValueCommandWithTimeTag(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                                       int informationObjectAddress, IeScaledValue scaledValue, 
                                                       IeQualifierOfSetPointCommand qualifier, IeTime56 timeTag) {
        return new ASdu(ASduType.C_SE_TB_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, scaledValue, qualifier, timeTag));
    }

    /**
     * A set-point command, short floating point number (C_SE_NC_1, TI: 50).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param shortFloat
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu setShortFloatCommand(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                           int informationObjectAddress, IeShortFloat shortFloat, 
                                           IeQualifierOfSetPointCommand qualifier) {
        return new ASdu(ASduType.C_SE_NC_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, shortFloat, qualifier));
    }

    /**
     * A set-point command with time tag CP56Time2a, short floating point number (C_SE_TC_1, TI: 63).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param shortFloat
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     * @param timeTag
     *            the time tag to be sent.
     */
    public static ASdu setShortFloatCommandWithTimeTag(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                                      int informationObjectAddress, IeShortFloat shortFloat, 
                                                      IeQualifierOfSetPointCommand qualifier, IeTime56 timeTag) {
        return new ASdu(ASduType.C_SE_TC_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, shortFloat, qualifier, timeTag));
    }

    /**
     * A bitstring of 32 bit (C_BO_NA_1, TI: 51).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param binaryState
     *            the value to be sent.
     */
    public static ASdu bitstringCommand(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                       int informationObjectAddress, IeBinaryStateInformation binaryState) {
        return new ASdu(ASduType.C_BO_NA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, binaryState));
    }

    /**
     * A bitstring of 32 bit with time tag CP56Time2a (C_BO_TA_1, TI: 64).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param binaryState
     *            the value to be sent.
     * @param timeTag
     *            the time tag to be sent.
     */
    public static ASdu bitstringCommandWithTimeTag(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                                  int informationObjectAddress, IeBinaryStateInformation binaryState, 
                                                  IeTime56 timeTag) {
        return new ASdu(ASduType.C_BO_TA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, binaryState, timeTag));
    }

    /**
     * A parameter of measured values, normalized value (P_ME_NA_1, TI: 110).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress
     *            the information object address.
     * @param normalizedValue
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu parameterNormalizedValueCommand(int originatorAddress, int commonAddress,
                                                       int informationObjectAddress, IeNormalizedValue normalizedValue,
                                                       IeQualifierOfParameterOfMeasuredValues qualifier) {
        return new ASdu(ASduType.P_ME_NA_1, false, CauseOfTransmission.ACTIVATION, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, normalizedValue, qualifier));
    }

    /**
     * A parameter of measured values, scaled value (P_ME_NB_1, TI: 111).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress
     *            the information object address.
     * @param scaledValue
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu parameterScaledValueCommand(int originatorAddress, int commonAddress,
                                                   int informationObjectAddress, IeScaledValue scaledValue,
                                                   IeQualifierOfParameterOfMeasuredValues qualifier) {
        return new ASdu(ASduType.P_ME_NB_1, false, CauseOfTransmission.ACTIVATION, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, scaledValue, qualifier));
    }

    /**
     * A parameter of measured values, short floating point number (P_ME_NC_1, TI: 112).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress
     *            the information object address.
     * @param shortFloat
     *            the value to be sent.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu parameterShortFloatCommand(int originatorAddress, int commonAddress,
                                                  int informationObjectAddress, IeShortFloat shortFloat,
                                                  IeQualifierOfParameterOfMeasuredValues qualifier) {
        return new ASdu(ASduType.P_ME_NC_1, false, CauseOfTransmission.ACTIVATION, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, shortFloat, qualifier));
    }

    /**
     * A parameter activation (P_AC_NA_1, TI: 113).
     *
     * @param commonAddress
     *            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot
     *            the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress
     *            the information object address.
     * @param qualifier
     *            the qualifier to be sent.
     */
    public static ASdu parameterActivation(int originatorAddress, int commonAddress, CauseOfTransmission cot, 
                                          int informationObjectAddress, int qualifier) {
        return new ASdu(ASduType.P_AC_NA_1, false, cot, false, false,
                       originatorAddress, commonAddress, new InformationObject(informationObjectAddress, new IeQualifierOfParameterActivation(qualifier)));
    }
    

    public static ASdu fileReady(int originatorAddress, int commonAddress, int informationObjectAddress, 
                                IeFileReadyQualifier fileReadyQualifier, IeNameOfFile nameOfFile, 
                                IeLengthOfFileOrSection lengthOfFile) {
        return new ASdu(ASduType.F_FR_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, nameOfFile, fileReadyQualifier, lengthOfFile));
    }
    
    public static ASdu sectionReady(int originatorAddress, int commonAddress, int informationObjectAddress, 
                                   IeNameOfFile nameOfFile, IeNameOfSection nameOfSection, 
                                   IeLengthOfFileOrSection lengthOfSection, IeSectionReadyQualifier qualifier) {
        return new ASdu(ASduType.F_SR_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, nameOfFile, nameOfSection, lengthOfSection, qualifier));
    }

    // TODO cot is passed in old impl, so is nameOfSection
    public static ASdu callOrSelectFiles(int originatorAddress, int commonAddress, int informationObjectAddress,
                                         IeNameOfFile nameOfFile, IeFileSegment fileSegment) {
        return new ASdu(ASduType.F_SC_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, nameOfFile, fileSegment));
    }

    // TODO name of section is passed in old version
    public static ASdu lastSectionOrSegment(int originatorAddress, int commonAddress, int informationObjectAddress, 
                                           IeNameOfFile nameOfFile, IeFileSegment fileSegment, IeChecksum checksum) {
        return new ASdu(ASduType.F_LS_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, nameOfFile, fileSegment, checksum));
    }

    // TODO name of section passed in old version
    public static ASdu ackFileOrSection(int originatorAddress, int commonAddress, int informationObjectAddress, 
                                       IeNameOfFile nameOfFile, IeFileReadyQualifier fileReadyQualifier, 
                                       IeAckFileOrSectionQualifier ackQualifier) {
        return new ASdu(ASduType.F_AF_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, nameOfFile, fileReadyQualifier, ackQualifier));
    }

    // TODO completely different from sendSegment in old version
    public static ASdu fileSegment(int originatorAddress, int commonAddress, int informationObjectAddress, 
                                  IeNameOfFile nameOfFile, IeFileSegment fileSegment, Collection<byte[]> data) {
        // Convert data to information elements - use IeFileSegment for each data chunk
        InformationElement[][] elements = new InformationElement[data.size() + 2][];
        elements[0] = new InformationElement[] { nameOfFile };
        elements[1] = new InformationElement[] { fileSegment };
        
        int i = 2;
        for (byte[] segment : data) {
            elements[i++] = new InformationElement[] { new IeFileSegment(segment, 0, segment.length) };
        }
        
        return new ASdu(ASduType.F_SG_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, elements));
    }
    
    public static ASdu sendDirectory(int originatorAddress, int commonAddress, int informationObjectAddress, 
                                    InformationElement[][] directory) {
        return new ASdu(ASduType.F_DR_TA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, directory));
    }
    
    public static ASdu queryLog(int originatorAddress, int commonAddress, int informationObjectAddress, 
                               IeNameOfFile nameOfFile, IeTime56 rangeStartTime, IeTime56 rangeEndTime) {
        return new ASdu(ASduType.F_SC_NB_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                       originatorAddress, commonAddress,
                       new InformationObject(informationObjectAddress, nameOfFile, rangeStartTime, rangeEndTime));
    }
    
    public static ASdu createConfirmation(ASdu originalAsdu, int originatorAddress) {
        return new ASdu(originalAsdu.getTypeIdentification(), originalAsdu.isSequenceOfElements(),
                       CauseOfTransmission.ACTIVATION_CON, originalAsdu.isTestFrame(), 
                       originalAsdu.isNegativeConfirm(), originatorAddress, 
                       originalAsdu.getCommonAddress(), originalAsdu.getInformationObjects());
    }
}
