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
package net.sympower.iec60870.iec101.frame.encoding;

import org.junit.Test;
import net.sympower.iec60870.iec101.frame.Iec101FixedFrame;
import net.sympower.iec60870.iec101.frame.Iec101Frame.FunctionCode;
import net.sympower.iec60870.iec101.frame.BitUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.ACD_CLEAR;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.ACD_SET;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.BYTE_TO_UNSIGNED_MASK;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DECODE_OFFSET;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DEFAULT_LINK_ADDRESS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DEFAULT_SETTINGS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DFC_CLEAR;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCB_ACD_BIT_POS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCB_CLEAR;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCB_SET;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCV_DFC_BIT_POS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCV_DISABLED;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCV_ENABLED;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FIXED_FRAME_CONTROL_BYTE_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FIXED_FRAME_LENGTH;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FIXED_FRAME_START_BYTE_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FUNCTION_CODE_MASK;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.MIN_UNSIGNED_BYTE_VALUE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.PRIMARY_FUNCTION_CODE_COUNT;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.PRIMARY_STATION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.PRM_BIT_POS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.RESERVED_BIT_POS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.RESET_REMOTE_LINK_CODE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.SECONDARY_STATION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.START_BYTE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.STATUS_LINK_ACCESS_DEMAND_CODE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.STOP_BYTE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.TEST_ADDRESSES;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.TEST_ADDRESS_1;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.TEST_BUFFER_SIZE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.TEST_FUNCTION_LINK_CODE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.assertBitClear;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.assertBitSet;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.assertByteEquals;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.assertByteInRange;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.getUnsignedByte;

public class Iec101FixedFrameFormatTest {
    
    private static byte calculateChecksum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += b & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }

    @Test
    public void testResetRemoteLinkFrameFormat_shouldEncodeCorrectly() {
        Iec101FixedFrame frame = givenResetRemoteLinkFrame();
        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenFrameHasCorrectFormatWithAddress(encodedFrame, DEFAULT_LINK_ADDRESS);
        thenFrameHasCorrectPrimaryControlFieldWithFcbClearFcvClear(encodedFrame, RESET_REMOTE_LINK_CODE);
        thenFrameHasCorrectChecksum(encodedFrame);
    }

    @Test
    public void testTestFunctionLinkFrameFormat_shouldEncodeCorrectly() {
        Iec101FixedFrame frame = givenTestFunctionLinkFrame();
        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenFrameHasCorrectFormatWithAddress(encodedFrame, DEFAULT_LINK_ADDRESS);
        thenFrameHasCorrectPrimaryControlFieldWithFcbSetFcvSet(encodedFrame, TEST_FUNCTION_LINK_CODE);
        thenFrameHasCorrectChecksum(encodedFrame);
    }

    @Test
    public void testSecondaryResponseFrameFormat_shouldEncodeCorrectly() {
        Iec101FixedFrame frame = givenSecondaryResponseFrame();
        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenFrameHasCorrectFormatWithAddress(encodedFrame, DEFAULT_LINK_ADDRESS);
        thenFrameHasCorrectSecondaryControlFieldWithAcdSetDfcClear(encodedFrame, STATUS_LINK_ACCESS_DEMAND_CODE);
        thenFrameHasCorrectChecksum(encodedFrame);
    }

    @Test
    public void testFrameRoundTrip_shouldPreserveAllProperties() throws IOException {
        Iec101FixedFrame originalFrame = givenRequestLinkStatusFrame();
        Iec101FixedFrame decodedFrame = whenFrameIsEncodedAndDecoded(originalFrame);

        thenFramePropertiesMatch(originalFrame, decodedFrame);
    }

    @Test
    public void testChecksumValidation_shouldBeCorrectForAllCombinations() {
        FunctionCode[] testFunctions = givenTestFunctionCodes();

        for (int address : TEST_ADDRESSES) {
            for (FunctionCode functionCode : testFunctions) {
                boolean isPrimary = isPrimaryFunction(functionCode);

                Iec101FixedFrame frame = givenFrameWithAddressAndFunction(address, functionCode, isPrimary);
                byte[] encodedFrame = whenFrameIsEncoded(frame);

                thenFrameHasCorrectFormat(encodedFrame);
                thenFrameHasCorrectAddress(encodedFrame, address);
                thenFrameHasCorrectChecksum(encodedFrame);
            }
        }
    }

    @Test
    public void testFrameBytePositions_shouldMatchSpecification() {
        Iec101FixedFrame frame = givenFrameWithSpecificAddress(TEST_ADDRESS_1);
        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenBytePositionsMatchSpecification(encodedFrame, TEST_ADDRESS_1);
    }

    private Iec101FixedFrame givenResetRemoteLinkFrame() {
        return new Iec101FixedFrame(
            DEFAULT_LINK_ADDRESS,
            FunctionCode.RESET_REMOTE_LINK,
            PRIMARY_STATION,
            FCV_DISABLED,
            FCB_CLEAR,
            ACD_CLEAR,
            DFC_CLEAR
        );
    }

    private Iec101FixedFrame givenTestFunctionLinkFrame() {
        return new Iec101FixedFrame(
            DEFAULT_LINK_ADDRESS,
            FunctionCode.TEST_FUNCTION_LINK,
            PRIMARY_STATION,
            FCV_ENABLED,
            FCB_SET,
            ACD_CLEAR,
            DFC_CLEAR
        );
    }

    private Iec101FixedFrame givenSecondaryResponseFrame() {
        return new Iec101FixedFrame(
            DEFAULT_LINK_ADDRESS,
            FunctionCode.STATUS_LINK_ACCESS_DEMAND,
            SECONDARY_STATION,
            FCV_DISABLED,
            FCB_CLEAR,
            ACD_SET,
            DFC_CLEAR
        );
    }

    private Iec101FixedFrame givenRequestLinkStatusFrame() {
        return new Iec101FixedFrame(
            DEFAULT_LINK_ADDRESS,
            FunctionCode.REQUEST_LINK_STATUS,
            PRIMARY_STATION,
            FCV_ENABLED,
            FCB_CLEAR,
            ACD_CLEAR,
            DFC_CLEAR
        );
    }

    private FunctionCode[] givenTestFunctionCodes() {
        return new FunctionCode[]{
            FunctionCode.RESET_REMOTE_LINK,
            FunctionCode.TEST_FUNCTION_LINK,
            FunctionCode.REQUEST_LINK_STATUS,
            FunctionCode.RESP_NACK_NO_DATA
        };
    }

    private Iec101FixedFrame givenFrameWithAddressAndFunction(int address, FunctionCode functionCode, boolean isPrimary) {
        return new Iec101FixedFrame(
            address, functionCode, isPrimary, FCV_DISABLED, FCB_CLEAR, ACD_CLEAR, DFC_CLEAR
        );
    }

    private Iec101FixedFrame givenFrameWithSpecificAddress(int address) {
        return new Iec101FixedFrame(
            address,
            FunctionCode.RESET_REMOTE_LINK,
            PRIMARY_STATION,
            FCV_DISABLED,
            FCB_CLEAR,
            ACD_CLEAR,
            DFC_CLEAR
        );
    }

    private byte[] whenFrameIsEncoded(Iec101FixedFrame frame) {
        byte[] buffer = new byte[TEST_BUFFER_SIZE];
        int frameLength = frame.encode(buffer, DEFAULT_SETTINGS);
        int expectedLength = 5 + DEFAULT_SETTINGS.getLinkAddressLength() - 1; // start + control + address + checksum + end
        assertEquals("Frame should be exactly " + expectedLength + " bytes", expectedLength, frameLength);
        return buffer;
    }

    private Iec101FixedFrame whenFrameIsEncodedAndDecoded(Iec101FixedFrame originalFrame) throws IOException {
        byte[] buffer = new byte[TEST_BUFFER_SIZE];
        int length = originalFrame.encode(buffer, DEFAULT_SETTINGS);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer, DECODE_OFFSET, length - DECODE_OFFSET);
        return Iec101FixedFrame.decode(inputStream, DEFAULT_SETTINGS);
    }

    private int getFunctionCode(byte controlByte) {
        return controlByte & FUNCTION_CODE_MASK;
    }

    private void thenFrameHasCorrectFormat(byte[] frame) {
        assertByteEquals("Start byte must be 0x10", START_BYTE, frame, FIXED_FRAME_START_BYTE_POSITION);
        int stopBytePosition = 4 + DEFAULT_SETTINGS.getLinkAddressLength() - 1;
        assertByteEquals("Stop byte must be 0x16", STOP_BYTE, frame, stopBytePosition);
        
        assertBitClear(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, RESERVED_BIT_POS, "Reserved bit (bit 7) must always be clear");
    }

    private void thenFrameHasCorrectFormatWithAddress(byte[] frame, int expectedAddress) {
        thenFrameHasCorrectFormat(frame);
        // Extract address from frame based on address length
        int addressLength = DEFAULT_SETTINGS.getLinkAddressLength();
        byte[] addressBytes = new byte[addressLength];
        System.arraycopy(frame, 2, addressBytes, 0, addressLength); // Address starts at position 2
        int actualAddress = BitUtils.readBytes(addressBytes, 0, addressLength);
        assertEquals("Address field must match expected value", expectedAddress, actualAddress);
    }

    private void thenFrameHasCorrectPrimaryControlFieldWithFcbSetFcvSet(byte[] frame, int expectedFunctionCode) {
        thenFrameHasCorrectPrimaryControlFieldBase(frame, expectedFunctionCode);
        assertBitSet(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, FCB_ACD_BIT_POS, "FCB bit should be set");
        assertBitSet(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, FCV_DFC_BIT_POS, "FCV bit should be set");
    }

    private void thenFrameHasCorrectPrimaryControlFieldWithFcbClearFcvClear(byte[] frame, int expectedFunctionCode) {
        thenFrameHasCorrectPrimaryControlFieldBase(frame, expectedFunctionCode);
        assertBitClear(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, FCB_ACD_BIT_POS, "FCB bit should be clear");
        assertBitClear(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, FCV_DFC_BIT_POS, "FCV bit should be clear");
    }
    
    private void thenFrameHasCorrectPrimaryControlFieldBase(byte[] frame, int expectedFunctionCode) {
        byte controlByte = frame[FIXED_FRAME_CONTROL_BYTE_POSITION];
        
        assertBitSet(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, PRM_BIT_POS, "PRM bit must be set for primary station");
        assertBitClear(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, RESERVED_BIT_POS, "Reserved bit must be clear");
        
        assertEquals("Function code must match expected value", expectedFunctionCode, getFunctionCode(controlByte));
    }

    private void thenFrameHasCorrectSecondaryControlFieldWithAcdSetDfcClear(byte[] frame, int expectedFunctionCode) {
        thenFrameHasCorrectSecondaryControlFieldBase(frame, expectedFunctionCode);
        assertBitSet(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, FCB_ACD_BIT_POS, "ACD bit should be set");
        assertBitClear(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, FCV_DFC_BIT_POS, "DFC bit should be clear");
    }

    private void thenFrameHasCorrectSecondaryControlFieldBase(byte[] frame, int expectedFunctionCode) {
        byte controlByte = frame[FIXED_FRAME_CONTROL_BYTE_POSITION];
        
        assertBitClear(frame,
                       FIXED_FRAME_CONTROL_BYTE_POSITION, PRM_BIT_POS, "PRM bit must be clear for secondary station");
        assertBitClear(frame, FIXED_FRAME_CONTROL_BYTE_POSITION, RESERVED_BIT_POS, "Reserved bit must be clear");
        
        assertEquals("Function code must match expected value", expectedFunctionCode, getFunctionCode(controlByte));
    }

    private void thenFrameHasCorrectChecksum(byte[] frame) {
        int controlByte = getUnsignedByte(frame, FIXED_FRAME_CONTROL_BYTE_POSITION);
        // Extract address bytes and calculate checksum
        int addressLength = DEFAULT_SETTINGS.getLinkAddressLength();
        byte[] checksumData = new byte[1 + addressLength];
        checksumData[0] = (byte) controlByte;
        System.arraycopy(frame, 2, checksumData, 1, addressLength);
        int expectedChecksum = calculateChecksum(checksumData) & BYTE_TO_UNSIGNED_MASK;
        
        int checksumPosition = 2 + addressLength;
        int actualChecksum = getUnsignedByte(frame, checksumPosition);

        assertEquals("Checksum must be 8-bit sum of control field + address field", expectedChecksum, actualChecksum);
    }

    private void thenFrameHasCorrectAddress(byte[] frame, int expectedAddress) {
        // Extract address from frame based on address length
        int addressLength = DEFAULT_SETTINGS.getLinkAddressLength();
        byte[] addressBytes = new byte[addressLength];
        System.arraycopy(frame, 2, addressBytes, 0, addressLength); // Address starts at position 2
        int actualAddress = BitUtils.readBytes(addressBytes, 0, addressLength);
        assertEquals("Address field must match expected value", expectedAddress, actualAddress);
    }

    private void thenFramePropertiesMatch(Iec101FixedFrame original, Iec101FixedFrame decoded) {
        assertEquals("Link address must be preserved in round-trip", original.getLinkAddress(), decoded.getLinkAddress());
        assertEquals("Function code must be preserved in round-trip", original.getFunctionCode(), decoded.getFunctionCode());
        assertEquals("PRM bit must be preserved in round-trip", original.getPrm(), decoded.getPrm());
        assertEquals("FCV bit must be preserved in round-trip", original.getFcv(), decoded.getFcv());
        assertEquals("FCB bit must be preserved in round-trip", original.getFcb(), decoded.getFcb());
        assertEquals("ACD bit must be preserved in round-trip", original.getAcd(), decoded.getAcd());
        assertEquals("DFC bit must be preserved in round-trip", original.getDfc(), decoded.getDfc());
    }


    private void thenBytePositionsMatchSpecification(byte[] frame, int expectedAddress) {
        assertByteEquals("Byte 0: Start must be 0x10", START_BYTE, frame, FIXED_FRAME_START_BYTE_POSITION);
        assertByteInRange("Byte 1: Control field must be valid [0-255]", frame, FIXED_FRAME_CONTROL_BYTE_POSITION);
        // Check address bytes
        int addressLength = DEFAULT_SETTINGS.getLinkAddressLength();
        for (int i = 0; i < addressLength; i++) {
            assertByteInRange("Address byte " + i + " must be valid [0-255]", frame, 2 + i);
        }
        int checksumPosition = 2 + addressLength;
        assertByteInRange("Checksum must be valid [0-255]", frame, checksumPosition);
        int stopBytePosition = 3 + addressLength;
        assertByteEquals("Stop must be 0x16", STOP_BYTE, frame, stopBytePosition);

        int frameLength = 5 + DEFAULT_SETTINGS.getLinkAddressLength() - 1;
        for (int i = frameLength; i < frame.length; i++) {
            assertEquals("Buffer beyond frame length must be zero at position " + i, MIN_UNSIGNED_BYTE_VALUE, frame[i]);
        }
    }

    private boolean isPrimaryFunction(FunctionCode functionCode) {
        return functionCode.ordinal() < PRIMARY_FUNCTION_CODE_COUNT;
    }
}
