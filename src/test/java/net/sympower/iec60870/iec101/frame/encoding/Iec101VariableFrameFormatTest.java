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
import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.common.elements.IeQualifierOfInterrogation;
import net.sympower.iec60870.common.elements.InformationObject;
import net.sympower.iec60870.iec101.frame.Iec101Frame.FunctionCode;
import net.sympower.iec60870.iec101.frame.Iec101VariableFrame;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.ACD_CLEAR;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.BYTE_TO_UNSIGNED_MASK;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DECODE_OFFSET;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DEFAULT_COMMON_ADDRESS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DEFAULT_ORIGINATOR_ADDRESS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DFC_CLEAR;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.END_FRAME;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCB_CLEAR;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCB_SET;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCV_DISABLED;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.FCV_ENABLED;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.MINIMUM_VARIABLE_FRAME_SIZE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.PRIMARY_STATION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.QOI_STATION_INTERROGATION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.SECONDARY_STATION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.START_VARIABLE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.VARIABLE_FRAME_ADDRESS_BYTE_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.VARIABLE_FRAME_ASDU_START_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.VARIABLE_FRAME_CONTROL_BYTE_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.VARIABLE_FRAME_LENGTH_1_BYTE_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.VARIABLE_FRAME_LENGTH_2_BYTE_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.VARIABLE_FRAME_START_2_BYTE_POSITION;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.assertByteEquals;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.getUnsignedByte;

/**
 * Tests for IEC-101 Variable Length Frame format compliance.
 * 
 * Variable Frame Format (IEC 60870-5-101 specification):
 * <table>
 * <tr><th>Offset</th><th>Field</th><th>Size</th><th>Description</th></tr>
 * <tr><td>0</td><td>Start</td><td>1 byte</td><td>Always 0x68</td></tr>
 * <tr><td>1</td><td>Length</td><td>1 byte</td><td>Number of bytes from control to checksum</td></tr>
 * <tr><td>2</td><td>Length (repeat)</td><td>1 byte</td><td>Same as above (for reliability)</td></tr>
 * <tr><td>3</td><td>Start (repeat)</td><td>1 byte</td><td>Always 0x68</td></tr>
 * <tr><td>4</td><td>Control</td><td>1 byte</td><td>Control field bitfield</td></tr>
 * <tr><td>5</td><td>Link Address</td><td>1-2 bytes</td><td>Device address (we use 1 byte)</td></tr>
 * <tr><td>6+</td><td>ASDU</td><td>Variable</td><td>Application data</td></tr>
 * <tr><td>N-2</td><td>Checksum</td><td>1 byte</td><td>8-bit sum of all bytes except start/stop</td></tr>
 * <tr><td>N-1</td><td>Stop</td><td>1 byte</td><td>Always 0x16</td></tr>
 * </table>
 */
public class Iec101VariableFrameFormatTest {

    private static final IEC60870Settings SETTINGS = new IEC60870Settings();
    
    private static final int START_POSITION = 0;
    private static final int VARIABLE_FRAME_HEADER_SIZE = 4;
    private static final int DEFAULT_LINK_ADDRESS = 42;
    private static final int TEST_ADDRESS_1 = 1;
    private static final int TEST_ADDRESS_2 = 99;
    private static final int TEST_ADDRESS_3 = 123;
    private static final int MIN_LENGTH_VALUE = 3;
    private static final int BUFFER_SIZE = 255;
    private static final int TEST_IOA_1 = 0;
    private static final int TEST_IOA_2 = 5000;
    private static final int TEST_IOA_3 = 8000;
    private static final int CHECKSUM_POSITION_OFFSET = 2;
    private static final int CHECKSUM_INCLUSION_OFFSET = 1;

    @Test
    public void testVariableFrameFormat_PrimaryFrameWithInterrogationAsdu() {
        Iec101VariableFrame frame = givenPrimaryFrameWithInterrogationAsdu(
            DEFAULT_LINK_ADDRESS, FCV_ENABLED, FCB_CLEAR
        );

        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenFrameHasCorrectVariableFormat(encodedFrame);
        thenFrameHasCorrectHeader(encodedFrame, DEFAULT_LINK_ADDRESS);
        thenFrameHasValidAsdu(encodedFrame);
        thenFrameHasCorrectChecksum(encodedFrame);
        thenFrameHasCorrectLengthField(encodedFrame);
    }

    @Test
    public void testVariableFrameFormat_SecondaryFrameWithMinimalAsdu() {
        Iec101VariableFrame frame = givenSecondaryFrameWithMinimalAsdu(
            TEST_ADDRESS_1, ACD_CLEAR, DFC_CLEAR
        );

        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenFrameHasCorrectVariableFormat(encodedFrame);
        thenFrameHasCorrectHeader(encodedFrame, TEST_ADDRESS_1);
        thenFrameHasMinimumSize(encodedFrame);
        thenFrameHasCorrectChecksum(encodedFrame);
        thenFrameHasCorrectLengthField(encodedFrame);
    }

    @Test
    public void testVariableFrameFormat_EncodingDecodingRoundTrip() throws IOException {
        Iec101VariableFrame originalFrame = givenPrimaryFrameWithInterrogationAsdu(
            DEFAULT_LINK_ADDRESS, FCV_ENABLED, FCB_CLEAR
        );

        Iec101VariableFrame decodedFrame = whenFrameIsEncodedAndDecoded(originalFrame);

        thenFramePropertiesMatch(originalFrame, decodedFrame);
        thenAsduPropertiesMatch(originalFrame.getAsdu(), decodedFrame.getAsdu());
    }

    @Test
    public void testVariableFrameFormat_LengthFieldValidation() {
        Iec101VariableFrame frame = givenFrameWithSingleCommandAsdu(
            TEST_ADDRESS_3, FCV_DISABLED, FCB_SET
        );

        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenLengthFieldMatchesActualDataLength(encodedFrame);
        thenLengthFieldCoversControlToChecksum(encodedFrame);
        thenLengthFieldsAreIdentical(encodedFrame);
    }

    @Test
    public void testVariableFrameFormat_ChecksumCalculationValidation() {
        Iec101VariableFrame frame = givenFrameWithDoubleCommandAsdu(
            TEST_ADDRESS_2, FCV_ENABLED, FCB_SET
        );

        byte[] encodedFrame = whenFrameIsEncoded(frame);

        thenChecksumIsCorrect8BitSum(encodedFrame);
    }

    private static Iec101VariableFrame givenPrimaryFrameWithInterrogationAsdu(int linkAddress, boolean fcv, boolean fcb) {
        ASdu asdu = new ASdu(
            ASduType.C_IC_NA_1,
            false, // sequence of elements
            CauseOfTransmission.ACTIVATION,
            false, // test
            false, // negative confirm
            DEFAULT_ORIGINATOR_ADDRESS,
            DEFAULT_COMMON_ADDRESS,
            new InformationObject[] {
                new InformationObject(TEST_IOA_1, new IeQualifierOfInterrogation(QOI_STATION_INTERROGATION))
            }
        );

        return new Iec101VariableFrame(
            linkAddress,
            FunctionCode.USER_DATA_CONFIRMED,
            PRIMARY_STATION,
            fcv,
            fcb,
            ACD_CLEAR, // not used in primary frames
            DFC_CLEAR, // not used in primary frames
            asdu
        );
    }

    private static Iec101VariableFrame givenSecondaryFrameWithMinimalAsdu(int linkAddress, boolean acd, boolean dfc) {
        ASdu asdu = new ASdu(
            ASduType.C_TS_NA_1, // Test command (minimal)
            false,
            CauseOfTransmission.ACTIVATION,
            false, false,
            DEFAULT_ORIGINATOR_ADDRESS,
            DEFAULT_COMMON_ADDRESS,
            new InformationObject[] {
                new InformationObject(TEST_IOA_1) // No information elements
            }
        );

        return new Iec101VariableFrame(
            linkAddress,
            FunctionCode.USER_DATA_NO_REPLY,
            SECONDARY_STATION,
            FCV_DISABLED, // not used in secondary frames
            FCB_CLEAR,    // not used in secondary frames
            acd,
            dfc,
            asdu
        );
    }

    private static Iec101VariableFrame givenFrameWithSingleCommandAsdu(int linkAddress, boolean fcv, boolean fcb) {
        ASdu asdu = new ASdu(
            ASduType.C_SC_NA_1, // Single command
            false,
            CauseOfTransmission.ACTIVATION,
            false, false,
            DEFAULT_ORIGINATOR_ADDRESS,
            DEFAULT_COMMON_ADDRESS,
            new InformationObject(TEST_IOA_2)
        );

        return new Iec101VariableFrame(
            linkAddress,
            FunctionCode.USER_DATA_CONFIRMED,
            PRIMARY_STATION,
            fcv,
            fcb,
            ACD_CLEAR,
            DFC_CLEAR,
            asdu
        );
    }

    private static Iec101VariableFrame givenFrameWithDoubleCommandAsdu(int linkAddress, boolean fcv, boolean fcb) {
        ASdu asdu = new ASdu(
            ASduType.C_DC_NA_1, // Double command
            false,
            CauseOfTransmission.ACTIVATION,
            false, false,
            DEFAULT_ORIGINATOR_ADDRESS,
            DEFAULT_COMMON_ADDRESS,
            new InformationObject(TEST_IOA_3)
        );

        return new Iec101VariableFrame(
            linkAddress,
            FunctionCode.USER_DATA_CONFIRMED,
            PRIMARY_STATION,
            fcv,
            fcb,
            ACD_CLEAR,
            DFC_CLEAR,
            asdu
        );
    }

    private static byte[] whenFrameIsEncoded(Iec101VariableFrame frame) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int frameLength = frame.encode(buffer, SETTINGS);
        
        byte[] result = new byte[frameLength];
        System.arraycopy(buffer, 0, result, 0, frameLength);
        return result;
    }

    private static Iec101VariableFrame whenFrameIsEncodedAndDecoded(Iec101VariableFrame originalFrame) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = originalFrame.encode(buffer, SETTINGS);
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer, DECODE_OFFSET, length - DECODE_OFFSET);
        return Iec101VariableFrame.decode(inputStream, SETTINGS);
    }


    private static void thenFrameHasCorrectVariableFormat(byte[] frame) {
        assertByteEquals("Start byte should be 0x68", START_VARIABLE, frame, START_POSITION);
        assertByteEquals("Second start byte should be 0x68", START_VARIABLE, frame, VARIABLE_FRAME_START_2_BYTE_POSITION);
        assertByteEquals("End byte should be 0x16", END_FRAME, frame, frame.length - 1);
    }

    private static void thenFrameHasCorrectHeader(byte[] frame, int expectedAddress) {
        int length1 = getUnsignedByte(frame, VARIABLE_FRAME_LENGTH_1_BYTE_POSITION);
        int length2 = getUnsignedByte(frame, VARIABLE_FRAME_LENGTH_2_BYTE_POSITION);
        assertEquals("Length fields should be identical", length1, length2);

        assertByteEquals("Link address should match", expectedAddress, frame, VARIABLE_FRAME_ADDRESS_BYTE_POSITION);
    }

    private static void thenFrameHasValidAsdu(byte[] frame) {
        int lengthField = getUnsignedByte(frame, VARIABLE_FRAME_LENGTH_1_BYTE_POSITION);
        int asduLength = lengthField - MIN_LENGTH_VALUE; // control + address + checksum = 3 bytes

        assertTrue("ASDU should have positive length", asduLength > 0);
        assertTrue("Frame should contain complete ASDU", frame.length >= VARIABLE_FRAME_ASDU_START_POSITION + asduLength);
    }

    private static void thenFrameHasCorrectChecksum(byte[] frame) {
        int checksumPosition = frame.length - CHECKSUM_POSITION_OFFSET;
        int actualChecksum = getUnsignedByte(frame, checksumPosition);
        
        // Calculate expected checksum: sum of all bytes from control to before checksum
        int expectedSum = 0;
        for (int i = VARIABLE_FRAME_CONTROL_BYTE_POSITION; i < checksumPosition; i++) {
            expectedSum += getUnsignedByte(frame, i);
        }
        int expectedChecksum = expectedSum & BYTE_TO_UNSIGNED_MASK;
        
        assertEquals("Checksum should be 8-bit sum of control through ASDU", expectedChecksum, actualChecksum);
    }

    private static void thenFrameHasCorrectLengthField(byte[] frame) {
        int lengthField = getUnsignedByte(frame, VARIABLE_FRAME_LENGTH_1_BYTE_POSITION);
        // Length field represents bytes from control to checksum (inclusive)
        // Frame structure: START(1) + LENGTH(1) + LENGTH(1) + START(1) + [DATA] + END(1)
        // [DATA] = CONTROL(1) + ADDRESS(1) + ASDU(n) + CHECKSUM(1)
        int dataStartPosition = VARIABLE_FRAME_CONTROL_BYTE_POSITION;
        int checksumPosition = frame.length - CHECKSUM_POSITION_OFFSET; // Before end byte
        int actualDataLength = checksumPosition - dataStartPosition + CHECKSUM_INCLUSION_OFFSET; // Include checksum
        
        assertEquals("Length field should match control to checksum bytes", actualDataLength, lengthField);
    }

    private static void thenFrameHasMinimumSize(byte[] frame) {
        assertTrue("Minimal frame should meet minimum size requirement", 
                  frame.length >= MINIMUM_VARIABLE_FRAME_SIZE);
        
        int lengthField = getUnsignedByte(frame, VARIABLE_FRAME_LENGTH_1_BYTE_POSITION);
        assertTrue("Minimal frame length field should be >= 3", lengthField >= MIN_LENGTH_VALUE);
    }

    private static void thenFramePropertiesMatch(Iec101VariableFrame original, Iec101VariableFrame decoded) {
        assertEquals("Link address should match", original.getLinkAddress(), decoded.getLinkAddress());
        assertEquals("Function code should match", original.getFunctionCode(), decoded.getFunctionCode());
        assertEquals("PRM should match", original.getPrm(), decoded.getPrm());
        assertEquals("FCV should match", original.getFcv(), decoded.getFcv());
        assertEquals("FCB should match", original.getFcb(), decoded.getFcb());
        assertEquals("ACD should match", original.getAcd(), decoded.getAcd());
        assertEquals("DFC should match", original.getDfc(), decoded.getDfc());
    }

    private static void thenAsduPropertiesMatch(ASdu original, ASdu decoded) {
        assertNotNull("Decoded ASDU should not be null", decoded);
        assertEquals("ASDU type should match", original.getTypeIdentification(), decoded.getTypeIdentification());
        assertEquals("Cause of transmission should match", original.getCauseOfTransmission(), decoded.getCauseOfTransmission());
        assertEquals("Common address should match", original.getCommonAddress(), decoded.getCommonAddress());
        assertEquals("Originator address should match", original.getOriginatorAddress(), decoded.getOriginatorAddress());
    }

    private static void thenLengthFieldMatchesActualDataLength(byte[] frame) {
        int lengthField = frame[VARIABLE_FRAME_LENGTH_1_BYTE_POSITION] & BYTE_TO_UNSIGNED_MASK;
        int totalFrameLength = frame.length;
        int expectedDataLength = totalFrameLength - VARIABLE_FRAME_HEADER_SIZE - CHECKSUM_INCLUSION_OFFSET;
        
        assertEquals("Length field should match actual data length", expectedDataLength, lengthField);
    }

    private static void thenLengthFieldCoversControlToChecksum(byte[] frame) {
        int lengthField = frame[VARIABLE_FRAME_LENGTH_1_BYTE_POSITION] & BYTE_TO_UNSIGNED_MASK;
        int checksumPosition = frame.length - CHECKSUM_POSITION_OFFSET;
        int actualDataLength = checksumPosition - VARIABLE_FRAME_CONTROL_BYTE_POSITION + CHECKSUM_INCLUSION_OFFSET;
        
        assertEquals("Length should cover control to checksum inclusive", 
                    actualDataLength, lengthField);
    }

    private static void thenLengthFieldsAreIdentical(byte[] frame) {
        int length1 = frame[VARIABLE_FRAME_LENGTH_1_BYTE_POSITION] & BYTE_TO_UNSIGNED_MASK;
        int length2 = frame[VARIABLE_FRAME_LENGTH_2_BYTE_POSITION] & BYTE_TO_UNSIGNED_MASK;
        assertEquals("Both length fields should be identical", length1, length2);
    }

    private static void thenChecksumIsCorrect8BitSum(byte[] frame) {
        int checksumPosition = frame.length - CHECKSUM_POSITION_OFFSET;
        int actualChecksum = frame[checksumPosition] & BYTE_TO_UNSIGNED_MASK;
        
        // Manual calculation of 8-bit sum
        int manualSum = 0;
        for (int i = VARIABLE_FRAME_CONTROL_BYTE_POSITION; i < checksumPosition; i++) {
            manualSum += frame[i] & BYTE_TO_UNSIGNED_MASK;
        }
        int expectedChecksum = manualSum & BYTE_TO_UNSIGNED_MASK;
        
        assertEquals("Checksum should be 8-bit sum excluding start/stop bytes", 
                    expectedChecksum, actualChecksum);
    }

}
