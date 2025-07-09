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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for BitUtils utility class.
 * Verifies that bit manipulation operations work correctly for IEC-101 frame processing.
 */
public class BitUtilsTest {

    @Test
    public void testGetBit_shouldReturnCorrectBitStates() {
        // Test with value 0xC5 = 11000101 binary
        int value = 0xC5;
        
        assertTrue("Bit 0 should be set", BitUtils.getBit(value, 0));
        assertFalse("Bit 1 should be clear", BitUtils.getBit(value, 1));
        assertTrue("Bit 2 should be set", BitUtils.getBit(value, 2));
        assertFalse("Bit 3 should be clear", BitUtils.getBit(value, 3));
        assertFalse("Bit 4 should be clear", BitUtils.getBit(value, 4));
        assertFalse("Bit 5 should be clear", BitUtils.getBit(value, 5));
        assertTrue("Bit 6 should be set", BitUtils.getBit(value, 6));
        assertTrue("Bit 7 should be set", BitUtils.getBit(value, 7));
    }

    @Test
    public void testSetBit_shouldSetSpecificBits() {
        int value = 0x00; // 00000000 binary - Start with all bits clear
        
        value = BitUtils.setBit(value, 0);
        assertEquals("Setting bit 0 should result in 0x01", 0x01, value); // 00000001 binary
        
        value = BitUtils.setBit(value, 4);
        assertEquals("Setting bit 4 should result in 0x11", 0x11, value); // 00010001 binary
        
        value = BitUtils.setBit(value, 7);
        assertEquals("Setting bit 7 should result in 0x91", 0x91, value); // 10010001 binary
    }

    @Test
    public void testClearBit_shouldClearSpecificBits() {
        int value = 0xFF; // 11111111 binary - Start with all bits set
        
        value = BitUtils.clearBit(value, 0);
        assertEquals("Clearing bit 0 should result in 0xFE", 0xFE, value); // 11111110 binary
        
        value = BitUtils.clearBit(value, 4);
        assertEquals("Clearing bit 4 should result in 0xEE", 0xEE, value); // 11101110 binary
        
        value = BitUtils.clearBit(value, 7);
        assertEquals("Clearing bit 7 should result in 0x6E", 0x6E, value); // 01101110 binary
    }

    @Test
    public void testExtractBits_shouldApplyMaskCorrectly() {
        int value = 0xAB; // 10101011 binary
        
        // Test lower 4 bits
        int lowerFourBits = BitUtils.extractBits(value, 0x0F); // 00001111 binary mask
        assertEquals("Function code should be 0x0B", 0x0B, lowerFourBits); // 00001011 binary
        
        // Test upper four bits
        int upperFourBits = BitUtils.extractBits(value, 0xF0); // 11110000 binary mask
        assertEquals("Upper nibble should be 0xA0", 0xA0, upperFourBits); // 10100000 binary
    }

    @Test
    public void testToUnsigned_shouldConvertSignedByteToUnsigned() {
        byte signedByte = (byte) 0xFF; // 11111111 binary - -1 as signed byte
        int unsigned = BitUtils.toUnsigned(signedByte);
        assertEquals("0xFF byte should convert to 255 unsigned", 255, unsigned);
        
        byte positiveByte = 0x7F; // 01111111 binary - 127 as signed byte  
        unsigned = BitUtils.toUnsigned(positiveByte);
        assertEquals("0x7F byte should convert to 127 unsigned", 127, unsigned);
    }
}
