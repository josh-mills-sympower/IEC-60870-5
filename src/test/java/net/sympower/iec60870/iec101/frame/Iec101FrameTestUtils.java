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
package net.sympower.iec60870.iec101.frame;

import net.sympower.iec60870.common.IEC60870Settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public final class Iec101FrameTestUtils {
    
    public static final byte START_FIXED_FRAME = 0x10;
    public static final byte START_VARIABLE_FRAME = 0x68;
    public static final byte END_FRAME = 0x16;

    public static final int START_BYTE = 0x10;
    public static final int STOP_BYTE = 0x16;
    public static final byte START_FIXED = 0x10;
    public static final byte START_VARIABLE = 0x68;
    
    // Single character frames
    public static final byte ACK_BYTE = (byte) 0xE5;
    public static final byte NACK_BYTE = (byte) 0xA2;
    
    // Bit manipulation masks
    public static final int BYTE_TO_UNSIGNED_MASK = 0xFF;
    public static final int FUNCTION_CODE_MASK = 0x0F;
    public static final int FCV_DFC_BIT_MASK = 0x10;
    public static final int FCB_ACD_BIT_MASK = 0x20;
    public static final int PRM_BIT_MASK = 0x40;
    public static final int RESERVED_BIT_MASK = 0x80;

    // Frame structure sizes
    public static final int FIXED_FRAME_LENGTH = 5;
    public static final int SINGLE_CHAR_FRAME_LENGTH = 1;
    public static final int MINIMUM_VARIABLE_FRAME_SIZE = 8;
    public static final int VARIABLE_FRAME_HEADER_SIZE = 4;
    
    // Common test values
    public static final int DEFAULT_LINK_ADDRESS = 85;
    public static final int DEFAULT_COMMON_ADDRESS = 1;
    public static final int DEFAULT_ORIGINATOR_ADDRESS = 0;
    public static final int QOI_STATION_INTERROGATION = 20;
    
    // Test addresses for variety
    public static final int TEST_ADDRESS_1 = 66;
    public static final int TEST_ADDRESS_2 = 42;
    public static final int TEST_ADDRESS_3 = 123;
    public static final int TEST_ADDRESS_4 = 77;
    public static final int[] TEST_ADDRESSES = {1, 42, 127, 255};
    
    // Byte value constants
    public static final int MIN_UNSIGNED_BYTE_VALUE = 0;
    public static final int MAX_UNSIGNED_BYTE_VALUE = 255;
    public static final int TEST_BUFFER_SIZE = 10;
    public static final int DECODE_OFFSET = 1;
    
    // Function codes
    public static final int RESET_REMOTE_LINK_CODE = 0;
    public static final int TEST_FUNCTION_LINK_CODE = 2;
    public static final int STATUS_LINK_ACCESS_DEMAND_CODE = 8;
    public static final int PRIMARY_FUNCTION_CODE_COUNT = 4;
    
    public static final int RESERVED_BIT_POS = 7;
    public static final int PRM_BIT_POS = 6;
    public static final int FCB_ACD_BIT_POS = 5;
    public static final int FCV_DFC_BIT_POS = 4;
    
    public static final boolean PRIMARY_STATION = true;
    public static final boolean SECONDARY_STATION = false;
    public static final boolean FCV_ENABLED = true;
    public static final boolean FCV_DISABLED = false;
    public static final boolean FCB_SET = true;
    public static final boolean FCB_CLEAR = false;
    public static final boolean ACD_SET = true;
    public static final boolean ACD_CLEAR = false;
    public static final boolean DFC_SET = true;
    public static final boolean DFC_CLEAR = false;
    
    public static final IEC60870Settings DEFAULT_SETTINGS = new IEC60870Settings();

    public static final int FIXED_FRAME_START_BYTE_POSITION = 0;
    public static final int FIXED_FRAME_CONTROL_BYTE_POSITION = 1;
    public static final int FIXED_FRAME_ADDRESS_BYTE_POSITION = 2;
    public static final int FIXED_FRAME_CHECKSUM_BYTE_POSITION = 3;
    public static final int FIXED_FRAME_STOP_BYTE_POSITION = 4;

    public static final int VARIABLE_FRAME_LENGTH_1_BYTE_POSITION = 1;
    public static final int VARIABLE_FRAME_LENGTH_2_BYTE_POSITION = 2;
    public static final int VARIABLE_FRAME_START_2_BYTE_POSITION = 3;
    public static final int VARIABLE_FRAME_CONTROL_BYTE_POSITION = 4;
    public static final int VARIABLE_FRAME_ADDRESS_BYTE_POSITION = 5;
    public static final int VARIABLE_FRAME_ASDU_START_POSITION = 6;

    public static final int BUFFER_POSITION_ZERO = 0;

    /**
     * Checks if a specific bit is set (1) in a byte.
     */
    public static boolean getBit(byte b, int bitPosition) {
        // Step 1: Create a mask with only the target bit set
        // Example: for bitPosition=3, (1 << 3) creates: 0000 1000
        int mask = 1 << bitPosition;
        
        // Step 2: Perform bitwise AND between the byte and the mask
        // This isolates the target bit - all other bits become 0
        // Example: if b=0101 1010 and mask=0000 1000
        //          Result: 0000 1000 (bit 3 is set)
        //          If bit 3 was clear: 0000 0000
        int result = b & mask;
        
        // Step 3: Check if result is non-zero
        // If the target bit was set, result will be non-zero (the mask value)
        // If the target bit was clear, result will be zero
        return result != 0;
    }
    
    public static boolean getBit(byte[] byteArray, int byteIndex, int bitPosition) {
        return getBit(byteArray[byteIndex], bitPosition);
    }
    
    public static void assertBitSet(byte[] byteArray, int byteIndex, int bitPosition, String message) {
        assertTrue(message, getBit(byteArray, byteIndex, bitPosition));
    }
    
    public static void assertBitClear(byte[] byteArray, int byteIndex, int bitPosition, String message) {
        assertFalse(message, getBit(byteArray, byteIndex, bitPosition));
    }

    public static int toUnsigned(byte b) {
        // Java bytes are signed (-128 to 127), but protocol values are unsigned (0-255)
        // The bitwise AND with 0xFF converts the signed byte to unsigned int
        // Example: byte -1 (0xFF) becomes int 255
        // Example: byte 127 (0x7F) stays int 127
        return b & BYTE_TO_UNSIGNED_MASK;
    }

    public static int getUnsignedByte(byte[] byteArray, int index) {
        return toUnsigned(byteArray[index]);
    }
    
    public static void assertByteEquals(String message, int expected, byte[] byteArray, int index) {
        assertEquals(message, expected, getUnsignedByte(byteArray, index));
    }

    public static void assertByteInRange(String message, byte[] byteArray, int index) {
        int value = getUnsignedByte(byteArray, index);
        assertTrue(message, value >= MIN_UNSIGNED_BYTE_VALUE && value <= MAX_UNSIGNED_BYTE_VALUE);
    }
}
