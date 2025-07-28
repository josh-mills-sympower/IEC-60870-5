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

public final class BitUtils {

    /**
     * Checks if a specific bit is set in a value.
     * <p>
     * Uses bit shifting to create a mask with only the target bit set,
     * then performs bitwise AND to test if that bit is set in the original value.
     * <p>
     * Example: getBit(0xC5, 6) returns true
     * - 0xC5 = 11000101 binary
     * - bit position 6 = mask 01000000 (1 << 6 = 64 = 0x40)
     * - 11000101 & 01000000 = 01000000 (non-zero, so bit is set)
     * 
     * @param value the value to check
     * @param bitPosition the bit position (0-7 for byte operations)
     * @return true if the bit is set, false otherwise
     */
    public static boolean getBit(int value, int bitPosition) {
        int mask = 1 << bitPosition; // Create mask: shift 1 left by bitPosition
        return (value & mask) != 0;  // Test bit: AND with mask, check if non-zero
    }
    
    /**
     * Sets a specific bit in a value.
     * <p>
     * Uses bit shifting to create a mask with only the target bit set,
     * then performs bitwise OR to set that bit in the original value.
     * <p>
     * Example: setBit(0x01, 4) returns 0x11
     * - 0x01 = 00000001 binary
     * - bit position 4 = mask 00010000 (1 << 4 = 16 = 0x10)  
     * - 00000001 | 00010000 = 00010001 = 0x11
     * 
     * @param value the original value
     * @param bitPosition the bit position to set (0-7 for byte operations)
     * @return the value with the specified bit set
     */
    public static int setBit(int value, int bitPosition) {
        int mask = 1 << bitPosition; // Create mask: shift 1 left by bitPosition
        return value | mask;         // Set bit: OR with mask
    }
    
    /**
     * Clears a specific bit in a value.
     * <p>
     * Uses bit shifting to create a mask, then inverts it to have all bits set except
     * the target bit, then performs bitwise AND to clear that specific bit.
     * <p>
     * Example: clearBit(0xFF, 4) returns 0xEF
     * - 0xFF = 11111111 binary
     * - bit position 4 = mask 00010000 (1 << 4 = 16 = 0x10)
     * - inverted mask = 11101111 (~0x10 = 0xEF)
     * - 11111111 & 11101111 = 11101111 = 0xEF
     * 
     * @param value the original value
     * @param bitPosition the bit position to clear (0-7 for byte operations)
     * @return the value with the specified bit cleared
     */
    public static int clearBit(int value, int bitPosition) {
        int mask = ~(1 << bitPosition); // Create inverted mask: NOT of (1 shifted left)
        return value & mask;            // Clear bit: AND with inverted mask
    }
    
    /**
     * Extracts specific bits from a value using a mask.
     * <p>
     * Performs bitwise AND to keep only the bits that are set in the mask,
     * effectively filtering out unwanted bits while preserving their positions.
     * <p>
     * Example: extractBits(0xAB, 0x0F) returns 0x0B
     * - 0xAB = 10101011 binary
     * - 0x0F = 00001111 binary (mask for lower 4 bits)
     * - 10101011 & 00001111 = 00001011 = 0x0B
     * 
     * @param value the value to extract bits from
     * @param mask the bit mask to apply
     * @return the extracted bits (with original bit positions preserved)
     */
    public static int extractBits(int value, int mask) {
        return value & mask; // AND operation keeps only bits set in both value and mask
    }
    
    /**
     * Converts a signed byte to unsigned int representation.
     * <p>
     * Java bytes are signed (-128 to 127), but we often need to treat them as
     * unsigned (0 to 255) for protocol processing. The bitwise AND with 0xFF
     * masks off sign extension when converting to int.
     * <p>
     * Example: toUnsigned((byte)0xFF) returns 255
     * - (byte)0xFF = -1 in signed representation
     * - When cast to int: 0xFFFFFFFF (sign-extended)
     * - 0xFFFFFFFF & 0xFF = 0x000000FF = 255
     * 
     * @param b the signed byte
     * @return unsigned int value (0-255)
     */
    public static int toUnsigned(byte b) {
        return b & 0xFF; // Mask with 0xFF to remove sign extension
    }
    
    /**
     * Converts a byte array to a hexadecimal string representation.
     * <p>
     * Each byte is converted to a two-digit hexadecimal value with a space separator.
     * Useful for logging and debugging protocol frames.
     * <p>
     * Example: bytesToHex(new byte[]{0x10, 0x49, 0x01, (byte)0xFF}) returns "10 49 01 FF"
     * 
     * @param bytes the byte array to convert
     * @return hexadecimal string representation with space-separated bytes
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b & 0xFF));
        }
        return result.toString().trim();
    }
    
    /**
     * Reads a multi-byte value from a byte array in little-endian format.
     * <p>
     * Reads the specified number of bytes starting at the given offset and combines
     * them into a single integer value with the least significant byte first.
     * <p>
     * Example: readBytes(new byte[]{0x34, 0x12, 0x56}, 0, 2) returns 0x1234
     * - First byte (0x34) is LSB
     * - Second byte (0x12) is MSB
     * - Result: 0x34 | (0x12 << 8) = 0x1234
     * 
     * @param buffer the byte array to read from
     * @param offset the starting position in the buffer
     * @param byteLength the number of bytes to read (1-4)
     * @return the multi-byte value as an integer
     * @throws IllegalArgumentException if byteLength is not between 1 and 4
     */
    public static int readBytes(byte[] buffer, int offset, int byteLength) {
        if (byteLength < 1 || byteLength > 4) {
            throw new IllegalArgumentException("Byte length must be between 1 and 4");
        }
        
        int value = 0;
        for (int i = 0; i < byteLength; i++) {
            value |= (toUnsigned(buffer[offset + i]) << (8 * i)); // LSB first
        }
        return value;
    }
    
    /**
     * Writes a multi-byte value to a byte array in little-endian format.
     * <p>
     * Writes the specified number of bytes starting at the given offset, with the
     * least significant byte first.
     * <p>
     * Example: writeBytes(buffer, 0, 0x1234, 2) writes [0x34, 0x12]
     * - First byte: 0x1234 & 0xFF = 0x34 (LSB)
     * - Second byte: (0x1234 >> 8) & 0xFF = 0x12 (MSB)
     * 
     * @param buffer the byte array to write to
     * @param offset the starting position in the buffer
     * @param value the value to write
     * @param byteLength the number of bytes to write (1-4)
     * @throws IllegalArgumentException if byteLength is not between 1 and 4
     */
    public static void writeBytes(byte[] buffer, int offset, int value, int byteLength) {
        if (byteLength < 1 || byteLength > 4) {
            throw new IllegalArgumentException("Byte length must be between 1 and 4");
        }
        
        for (int i = 0; i < byteLength; i++) {
            buffer[offset + i] = (byte) ((value >> (8 * i)) & 0xFF); // LSB first
        }
    }
}
