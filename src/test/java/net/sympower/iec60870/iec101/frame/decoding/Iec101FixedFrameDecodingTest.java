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
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.iec101.frame.Iec101FixedFrame;
import net.sympower.iec60870.iec101.frame.Iec101Frame.FunctionCode;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.*;

public class Iec101FixedFrameDecodingTest {

    private static final IEC60870Settings SETTINGS = DEFAULT_SETTINGS;

    @Test
    public void testDecodeFixedFrame_PrimaryStationUserDataConfirmed() throws IOException {
        byte[] frameBytes = givenPrimaryFixedFrame(
            DEFAULT_LINK_ADDRESS, 
            FCB_SET,
            FCV_ENABLED,
            FunctionCode.USER_DATA_CONFIRMED
        );

        Iec101FixedFrame decodedFrame = whenFixedFrameIsDecoded(frameBytes);

        thenDecodedPrimaryFrameHasCorrectProperties(decodedFrame, frameBytes);
    }

    @Test
    public void testDecodeFixedFrame_SecondaryStationWithAccessDemand() throws IOException {
        byte[] frameBytes = givenSecondaryFixedFrame(
            TEST_ADDRESS_1,
            ACD_SET,
            DFC_CLEAR,
            FunctionCode.STATUS_LINK_ACCESS_DEMAND
        );

        Iec101FixedFrame decodedFrame = whenFixedFrameIsDecoded(frameBytes);

        thenDecodedSecondaryFrameHasCorrectProperties(decodedFrame, frameBytes);
    }

    @Test
    public void testDecodeFixedFrame_InvalidChecksum_ShouldThrowException() {
        byte[] frameBytes = givenFixedFrameWithInvalidChecksum();

        whenDecodingExpectsException(frameBytes, "checksum");
    }

    @Test
    public void testDecodeFixedFrame_InvalidEndByte_ShouldThrowException() {
        byte[] frameBytes = givenFixedFrameWithInvalidEndByte();

        whenDecodingExpectsException(frameBytes, "end");
    }

    @Test
    public void testDecodeFixedFrame_AllPrimaryFunctionCodes() throws IOException {
        FunctionCode[] primaryFunctions = {
            FunctionCode.RESET_REMOTE_LINK,
            FunctionCode.TEST_FUNCTION_LINK,
            FunctionCode.USER_DATA_CONFIRMED,
            FunctionCode.REQUEST_LINK_STATUS
        };

        for (FunctionCode functionCode : primaryFunctions) {
            byte[] frameBytes = givenPrimaryFixedFrame(
                TEST_ADDRESS_2,
                FCB_CLEAR,
                FCV_DISABLED,
                functionCode
            );

            Iec101FixedFrame decodedFrame = whenFixedFrameIsDecoded(frameBytes);

            thenDecodedPrimaryFrameHasCorrectProperties(decodedFrame, frameBytes);
        }
    }

    @Test
    public void testDecodeFixedFrame_AllSecondaryFunctionCodes() throws IOException {
        FunctionCode[] secondaryFunctions = {
            FunctionCode.ACK_CONFIRM,
            FunctionCode.NACK_MESSAGE,
            FunctionCode.STATUS_LINK_ACCESS_DEMAND,
            FunctionCode.STATUS_LINK_NO_DATA,
            FunctionCode.NACK_NOT_FUNCTIONING,
            FunctionCode.NACK_NOT_IMPLEMENTED
        };

        for (FunctionCode functionCode : secondaryFunctions) {
            byte[] frameBytes = givenSecondaryFixedFrame(
                111,
                ACD_CLEAR,
                DFC_CLEAR,
                functionCode
            );

            Iec101FixedFrame decodedFrame = whenFixedFrameIsDecoded(frameBytes);

            thenDecodedSecondaryFrameHasCorrectProperties(decodedFrame, frameBytes);
        }
    }

    private static byte[] givenPrimaryFixedFrame(int address, boolean fcb, boolean fcv, FunctionCode functionCode) {
        byte control = (byte) (0x40 | (fcb ? 0x20 : 0) | (fcv ? 0x10 : 0) | functionCode.getCode());
        byte checksum = (byte) ((control + address) & 0xFF);
        
        return new byte[] {
            START_FIXED_FRAME,
            control,
            (byte) address,
            checksum,
            END_FRAME
        };
    }

    private static byte[] givenSecondaryFixedFrame(int address, boolean acd, boolean dfc, FunctionCode functionCode) {
        byte control = (byte) ((acd ? 0x20 : 0) | (dfc ? 0x10 : 0) | functionCode.getCode());
        byte checksum = (byte) ((control + address) & 0xFF);
        
        return new byte[] {
            START_FIXED_FRAME,
            control,
            (byte) address,
            checksum,
            END_FRAME
        };
    }

    private static byte[] givenFixedFrameWithInvalidChecksum() {
        return new byte[] {
            START_FIXED_FRAME,
            0x43,
            0x55,
            (byte) 0x99,
            END_FRAME
        };
    }

    private static byte[] givenFixedFrameWithInvalidEndByte() {
        byte control = 0x43;
        byte address = 0x55;
        byte checksum = (byte) ((control + address) & 0xFF);
        
        return new byte[] {
            START_FIXED_FRAME,
            control,
            address,
            checksum,
            0x17
        };
    }

    private static Iec101FixedFrame whenFixedFrameIsDecoded(byte[] frameBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(frameBytes, 1, frameBytes.length - 1);
        return Iec101FixedFrame.decode(inputStream, SETTINGS);
    }

    private static void whenDecodingExpectsException(byte[] frameBytes, String expectedMessagePart) {
        try {
            whenFixedFrameIsDecoded(frameBytes);
            fail("Expected IOException to be thrown");
        } catch (IOException e) {
            assertTrue("Exception message should contain '" + expectedMessagePart + "'", 
                      e.getMessage().toLowerCase().contains(expectedMessagePart.toLowerCase()));
        }
    }

    private static void thenDecodedPrimaryFrameHasCorrectProperties(Iec101FixedFrame decodedFrame, byte[] originalFrameBytes) {
        thenDecodedFrameHasCorrectLinkAddress(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectFunctionCode(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectPrimaryControlBits(decodedFrame, originalFrameBytes);
    }
    
    private static void thenDecodedSecondaryFrameHasCorrectProperties(Iec101FixedFrame decodedFrame, byte[] originalFrameBytes) {
        thenDecodedFrameHasCorrectLinkAddress(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectFunctionCode(decodedFrame, originalFrameBytes);
        thenDecodedFrameHasCorrectSecondaryControlBits(decodedFrame, originalFrameBytes);
    }

    private static void thenDecodedFrameHasCorrectLinkAddress(Iec101FixedFrame decodedFrame, byte[] originalFrameBytes) {
        int expectedAddress = getUnsignedByte(originalFrameBytes, FIXED_FRAME_ADDRESS_BYTE_POSITION);
        assertEquals("Decoded link address should match original frame", expectedAddress, decodedFrame.getLinkAddress());
    }

    private static void thenDecodedFrameHasCorrectFunctionCode(Iec101FixedFrame decodedFrame, byte[] originalFrameBytes) {
        byte controlByte = originalFrameBytes[FIXED_FRAME_CONTROL_BYTE_POSITION];
        int expectedFunctionCode = controlByte & FUNCTION_CODE_MASK;
        assertEquals("Decoded function code should match original frame", expectedFunctionCode, decodedFrame.getFunctionCode().getCode());
    }

    private static void thenDecodedFrameHasCorrectPrimaryControlBits(Iec101FixedFrame decodedFrame, byte[] originalFrameBytes) {
        byte controlByte = originalFrameBytes[FIXED_FRAME_CONTROL_BYTE_POSITION];
        
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
    
    private static void thenDecodedFrameHasCorrectSecondaryControlBits(Iec101FixedFrame decodedFrame, byte[] originalFrameBytes) {
        byte controlByte = originalFrameBytes[FIXED_FRAME_CONTROL_BYTE_POSITION];
        
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
}
