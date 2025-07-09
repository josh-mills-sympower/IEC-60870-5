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
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.iec101.frame.Iec101SingleCharFrame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.ACK_BYTE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.BUFFER_POSITION_ZERO;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.BYTE_TO_UNSIGNED_MASK;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.DEFAULT_SETTINGS;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.NACK_BYTE;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.SINGLE_CHAR_FRAME_LENGTH;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.TEST_BUFFER_SIZE;

public class Iec101SingleCharFrameFormatTest {

    private static final IEC60870Settings SETTINGS = DEFAULT_SETTINGS;
    private static final byte ZERO_BYTE = 0x00;
    private static final byte RANDOM_INVALID_BYTE = 0x55;
    private static final byte VARIABLE_FRAME_START = 0x68;
    private static final byte FIXED_FRAME_START = 0x10;
    private static final byte FRAME_END = 0x16;

    @Test
    public void testSingleCharFrame_Ack() {
        Iec101SingleCharFrame ackFrame = givenFrameWithByte(ACK_BYTE);

        thenFrameIsValidAck(ackFrame);
    }

    @Test
    public void testSingleCharFrame_Nack() {
        Iec101SingleCharFrame nackFrame = givenFrameWithByte(NACK_BYTE);

        thenFrameIsValidNack(nackFrame);
    }

    @Test
    public void testSingleCharFrame_AckEncoding() {
        Iec101SingleCharFrame ackFrame = givenAckFrame();
        byte[] encodedFrame = whenFrameIsEncoded(ackFrame);

        thenEncodedFrameHasCorrectLength(encodedFrame, SINGLE_CHAR_FRAME_LENGTH);
        thenEncodedFrameHasByte(encodedFrame, BUFFER_POSITION_ZERO, ACK_BYTE);
        thenEncodedFrameHasNoExtraBytes(encodedFrame);
    }

    @Test
    public void testSingleCharFrame_NackEncoding() {
        Iec101SingleCharFrame nackFrame = givenNackFrame();
        byte[] encodedFrame = whenFrameIsEncoded(nackFrame);

        thenEncodedFrameHasCorrectLength(encodedFrame, SINGLE_CHAR_FRAME_LENGTH);
        thenEncodedFrameHasByte(encodedFrame, BUFFER_POSITION_ZERO, NACK_BYTE);
        thenEncodedFrameHasNoExtraBytes(encodedFrame);
    }

    @Test
    public void testSingleCharFrame_InvalidByteRejection() {
        byte[] invalidBytes = {
            ZERO_BYTE, RANDOM_INVALID_BYTE, VARIABLE_FRAME_START,
            FIXED_FRAME_START, FRAME_END
        };

        for (byte invalidByte : invalidBytes) {
            whenInvalidByteExpectsException(invalidByte);
        }
    }

    private static Iec101SingleCharFrame givenAckFrame() {
        return Iec101SingleCharFrame.createAck();
    }

    private static Iec101SingleCharFrame givenNackFrame() {
        return Iec101SingleCharFrame.createNack();
    }

    private static Iec101SingleCharFrame givenFrameWithByte(byte frameByte) {
        return new Iec101SingleCharFrame(frameByte);
    }

    private static byte[] whenFrameIsEncoded(Iec101SingleCharFrame frame) {
        byte[] buffer = new byte[TEST_BUFFER_SIZE];
        int frameLength = frame.encode(buffer, SETTINGS);
        
        byte[] result = new byte[frameLength];
        System.arraycopy(buffer, BUFFER_POSITION_ZERO, result, BUFFER_POSITION_ZERO, frameLength);
        return result;
    }

    private static void whenInvalidByteExpectsException(byte invalidByte) {
        try {
            new Iec101SingleCharFrame(invalidByte);
            // Convert signed byte to unsigned int for hex display
            fail("Expected IllegalArgumentException for byte 0x" + 
                 String.format("%02X", invalidByte & BYTE_TO_UNSIGNED_MASK));
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

    private static void thenFrameIsValidAck(Iec101SingleCharFrame frame) {
        assertTrue("Frame must be identified as ACK", frame.isAck());
    }

    private static void thenFrameIsValidNack(Iec101SingleCharFrame frame) {
        assertTrue("Frame must be identified as NACK", frame.isNack());
    }

    private static void thenEncodedFrameHasCorrectLength(byte[] encodedFrame, int expectedLength) {
        assertEquals("Encoded frame must have correct length",
                    expectedLength, encodedFrame.length);
    }

    private static void thenEncodedFrameHasByte(byte[] encodedFrame, int position, byte expectedByte) {
        assertEquals("Encoded frame must have correct byte at position " + position, expectedByte, encodedFrame[position]);
    }

    private static void thenEncodedFrameHasNoExtraBytes(byte[] encodedFrame) {
        assertEquals("Single character frame must be exactly 1 byte", SINGLE_CHAR_FRAME_LENGTH, encodedFrame.length);
    }
}
