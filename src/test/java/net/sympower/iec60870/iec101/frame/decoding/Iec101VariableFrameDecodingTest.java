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
package net.sympower.iec60870.iec101.frame.decoding;

import org.junit.Test;
import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.common.elements.InformationObject;
import net.sympower.iec60870.iec101.frame.Iec101Frame.FunctionCode;
import net.sympower.iec60870.iec101.frame.Iec101VariableFrame;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.*;

public class Iec101VariableFrameDecodingTest {

    private static final IEC60870Settings SETTINGS = DEFAULT_SETTINGS;

    @Test
    public void testDecodeVariableFrame_ValidPrimaryFrame() throws IOException {
        byte[] frameBytes = givenValidPrimaryVariableFrame(
            TEST_ADDRESS_3,
            FCB_SET,
            FCV_DISABLED
        );

        Iec101VariableFrame decodedFrame = whenVariableFrameIsDecoded(frameBytes);

        thenDecodedPrimaryFrameHasCorrectProperties(decodedFrame, frameBytes);
    }

    @Test
    public void testDecodeVariableFrame_ValidSecondaryFrame() throws IOException {
        byte[] frameBytes = givenValidSecondaryVariableFrame(
            TEST_ADDRESS_4,
            ACD_CLEAR,
            DFC_SET
        );

        Iec101VariableFrame decodedFrame = whenVariableFrameIsDecoded(frameBytes);

        thenDecodedSecondaryFrameHasCorrectProperties(decodedFrame, frameBytes);
    }

    @Test
    public void testDecodeVariableFrame_LengthMismatch_ShouldThrowException() throws IOException {
        byte[] frameBytes = givenVariableFrameWithLengthMismatch();

        whenVariableDecodingExpectsException(frameBytes, "length");
    }

    @Test
    public void testDecodeVariableFrame_InvalidSecondStartByte_ShouldThrowException() throws IOException {
        byte[] frameBytes = givenVariableFrameWithInvalidSecondStart();

        whenVariableDecodingExpectsException(frameBytes, "start");
    }

    private static byte[] givenVariableFrameWithLengthMismatch() {
        return new byte[] {
            START_VARIABLE_FRAME,
            0x08,
            0x09,
            START_VARIABLE_FRAME,
            0x43,
            0x55,
        };
    }

    private static byte[] givenVariableFrameWithInvalidSecondStart() {
        return new byte[] {
            START_VARIABLE_FRAME,
            0x08,
            0x08,
            0x69,
            0x43,
            0x55,
        };
    }

    private static byte[] givenValidPrimaryVariableFrame(int address, boolean fcb, boolean fcv) {
        byte[] asduBytes = new byte[] {
            (byte) 100,  // Type ID: C_IC_NA_1 (100)
            0x01,        // VSQ: 1 object, no sequence
            0x06,        // COT: Activation (6)
            0x00,        // COT high byte (for 2-byte COT)
            0x01,        // Common Address low
            0x00,        // Common Address high (for 2-byte common address)
            0x00,        // IOA: Information Object Address (0)
            0x00,        // IOA middle byte
            0x00,        // IOA high byte (for 3-byte IOA)
            QOI_STATION_INTERROGATION  // QOI: Station interrogation (20)
        };
        
        // Calculate frame length (control + address + ASDU + checksum)
        byte length = (byte) (3 + asduBytes.length);
        
        // Control field: PRM=1 + FCB/FCV + function code
        byte control = (byte) (PRM_BIT_MASK | (fcb ? FCB_ACD_BIT_MASK : 0) | (fcv ? FCV_DFC_BIT_MASK : 0) | FunctionCode.USER_DATA_CONFIRMED.getCode());
        
        // Calculate checksum
        int checksum = control & BYTE_TO_UNSIGNED_MASK;
        checksum += address & BYTE_TO_UNSIGNED_MASK;
        for (byte b : asduBytes) {
            checksum += b & BYTE_TO_UNSIGNED_MASK;
        }
        
        // Build complete frame: START + L + L + START + control + address + ASDU + checksum + END
        byte[] frame = new byte[6 + asduBytes.length + 2];
        int pos = 0;
        frame[pos++] = START_VARIABLE_FRAME;
        frame[pos++] = length;
        frame[pos++] = length;
        frame[pos++] = START_VARIABLE_FRAME;
        frame[pos++] = control;
        frame[pos++] = (byte) address;
        System.arraycopy(asduBytes, 0, frame, pos, asduBytes.length);
        pos += asduBytes.length;
        frame[pos++] = (byte) (checksum & BYTE_TO_UNSIGNED_MASK);
        frame[pos] = END_FRAME;
        
        return frame;
    }

    private static byte[] givenValidSecondaryVariableFrame(int address, boolean acd, boolean dfc) {
        byte[] asduBytes = new byte[] {
            (byte) 100,  // Type ID: C_IC_NA_1 (100)
            0x01,        // VSQ: 1 object, no sequence
            0x07,        // COT: Activation Confirmation (7)
            0x00,        // COT high byte (for 2-byte COT)
            0x01,        // Common Address low
            0x00,        // Common Address high (for 2-byte common address)
            0x00,        // IOA: Information Object Address (0)
            0x00,        // IOA middle byte
            0x00,        // IOA high byte (for 3-byte IOA)
            QOI_STATION_INTERROGATION  // QOI: Station interrogation (20)
        };
        
        // Calculate frame length (control + address + ASDU + checksum)
        byte length = (byte) (3 + asduBytes.length);
        
        // Control field: PRM=0 + ACD/DFC + function code
        byte control = (byte) ((acd ? FCB_ACD_BIT_MASK : 0) | (dfc ? FCV_DFC_BIT_MASK : 0) | FunctionCode.USER_DATA_NO_REPLY.getCode());
        
        // Calculate checksum
        int checksum = control & BYTE_TO_UNSIGNED_MASK;
        checksum += address & BYTE_TO_UNSIGNED_MASK;
        for (byte b : asduBytes) {
            checksum += b & BYTE_TO_UNSIGNED_MASK;
        }
        
        // Build complete frame: START + L + L + START + control + address + ASDU + checksum + END
        byte[] frame = new byte[6 + asduBytes.length + 2];
        int pos = 0;
        frame[pos++] = START_VARIABLE_FRAME;
        frame[pos++] = length;
        frame[pos++] = length;
        frame[pos++] = START_VARIABLE_FRAME;
        frame[pos++] = control;
        frame[pos++] = (byte) address;
        System.arraycopy(asduBytes, 0, frame, pos, asduBytes.length);
        pos += asduBytes.length;
        frame[pos++] = (byte) (checksum & BYTE_TO_UNSIGNED_MASK);
        frame[pos] = END_FRAME;
        
        return frame;
    }

    private static Iec101VariableFrame whenVariableFrameIsDecoded(byte[] frameBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(frameBytes, 1, frameBytes.length - 1);
        return Iec101VariableFrame.decode(inputStream, SETTINGS);
    }

    private static void whenVariableDecodingExpectsException(byte[] frameBytes, String expectedMessagePart) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(frameBytes, 1, frameBytes.length - 1);
            Iec101VariableFrame.decode(inputStream, SETTINGS);
            fail("Expected IOException to be thrown");
        } catch (IOException e) {
            assertTrue("Exception message should contain '" + expectedMessagePart + "'", 
                      e.getMessage().toLowerCase().contains(expectedMessagePart.toLowerCase()));
        }
    }

    private static void thenDecodedPrimaryFrameHasCorrectProperties(Iec101VariableFrame decodedFrame, byte[] originalFrameBytes) {
        thenDecodedFrameHasCorrectLinkAddress(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectFunctionCode(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectPrimaryControlBits(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectAsdu(decodedFrame, originalFrameBytes);
    }
    
    private static void thenDecodedSecondaryFrameHasCorrectProperties(Iec101VariableFrame decodedFrame, byte[] originalFrameBytes) {
        thenDecodedFrameHasCorrectLinkAddress(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectFunctionCode(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectSecondaryControlBits(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectAsdu(decodedFrame, originalFrameBytes);
    }

    private static void thenDecodedFrameHasCorrectLinkAddress(Iec101VariableFrame decodedFrame, byte[] originalFrameBytes) {
        int expectedAddress = getUnsignedByte(originalFrameBytes, VARIABLE_FRAME_ADDRESS_BYTE_POSITION);
        assertEquals("Decoded link address should match original frame", expectedAddress, decodedFrame.getLinkAddress());
    }

    private static void thenDecodedFrameHasCorrectFunctionCode(Iec101VariableFrame decodedFrame, byte[] originalFrameBytes) {
        byte controlByte = originalFrameBytes[VARIABLE_FRAME_CONTROL_BYTE_POSITION];
        int expectedFunctionCode = controlByte & FUNCTION_CODE_MASK;
        assertEquals("Decoded function code should match original frame", expectedFunctionCode, decodedFrame.getFunctionCode().getCode());
    }

    private static void thenDecodedFrameHasCorrectPrimaryControlBits(Iec101VariableFrame decodedFrame, byte[] originalFrameBytes) {
        byte controlByte = originalFrameBytes[VARIABLE_FRAME_CONTROL_BYTE_POSITION];
        
        boolean expectedPrm = getBit(controlByte, PRM_BIT_POS);
        boolean expectedFcb = getBit(controlByte, FCB_ACD_BIT_POS);
        boolean expectedFcv = getBit(controlByte, FCV_DFC_BIT_POS);
        
        assertTrue("Decoded PRM bit should be set for primary frame", decodedFrame.getPrm());
        assertEquals("Decoded FCB bit should match original primary frame", expectedFcb, decodedFrame.getFcb());
        assertEquals("Decoded FCV bit should match original primary frame", expectedFcv, decodedFrame.getFcv());
        
        // For primary frames, ACD and DFC should always be false
        assertFalse("Decoded ACD bit should be false for primary frame", decodedFrame.getAcd());
        assertFalse("Decoded DFC bit should be false for primary frame", decodedFrame.getDfc());
    }
    
    private static void thenDecodedFrameHasCorrectSecondaryControlBits(Iec101VariableFrame decodedFrame, byte[] originalFrameBytes) {
        byte controlByte = originalFrameBytes[VARIABLE_FRAME_CONTROL_BYTE_POSITION];
        
        boolean expectedPrm = getBit(controlByte, PRM_BIT_POS);
        boolean expectedAcd = getBit(controlByte, FCB_ACD_BIT_POS);
        boolean expectedDfc = getBit(controlByte, FCV_DFC_BIT_POS);
        
        assertFalse("Decoded PRM bit should be clear for secondary frame", decodedFrame.getPrm());
        assertEquals("Decoded ACD bit should match original secondary frame", expectedAcd, decodedFrame.getAcd());
        assertEquals("Decoded DFC bit should match original secondary frame", expectedDfc, decodedFrame.getDfc());
        
        // For secondary frames, FCB and FCV should always be false
        assertFalse("Decoded FCB bit should be false for secondary frame", decodedFrame.getFcb());
        assertFalse("Decoded FCV bit should be false for secondary frame", decodedFrame.getFcv());
    }

    private static void thenDecodedFrameHasCorrectAsdu(Iec101VariableFrame decodedFrame, byte[] originalFrameBytes) {
        ASdu decodedAsdu = decodedFrame.getAsdu();
        assertNotNull("Decoded ASDU should not be null", decodedAsdu);
        
        // Extract ASDU bytes from original frame for comparison
        int asduStart = VARIABLE_FRAME_ASDU_START_POSITION;
        int asduEnd = originalFrameBytes.length - 2; // Before checksum and end byte
        int asduLength = asduEnd - asduStart;
        
        assertTrue("ASDU should have positive length", asduLength > 0);
        
        // Verify ASDU properties are correctly decoded
        assertEquals("ASDU type should be decoded correctly", ASduType.C_IC_NA_1, decodedAsdu.getTypeIdentification());
        assertEquals("Common address should be decoded correctly", DEFAULT_COMMON_ADDRESS, decodedAsdu.getCommonAddress());
        assertEquals("Originator address should be decoded correctly", Integer.valueOf(DEFAULT_ORIGINATOR_ADDRESS), decodedAsdu.getOriginatorAddress());
        
        // Verify information objects are present
        InformationObject[] informationObjects = decodedAsdu.getInformationObjects();
        assertNotNull("Information objects should not be null", informationObjects);
        assertTrue("Should have at least one information object", informationObjects.length > 0);
    }
}
