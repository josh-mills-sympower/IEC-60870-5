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

/**
 * IEC 60870-5-101 Single Character Frame implementation.
 *
 * <p>
 * Frame Format:
 * <table>
 * <tr><th>Byte</th><th>Value</th><th>Description</th></tr>
 * <tr><td>0</td><td>0xE5</td><td>ACK - Positive acknowledgment</td></tr>
 * <tr><td>0</td><td>0xA2</td><td>NACK - Negative acknowledgment</td></tr>
 * </table>
 * <p>
 *
 * Single character frames are the simplest acknowledgment mechanism in IEC-101,
 * consisting of just one byte. They are used by secondary stations to acknowledge
 * the receipt of frames from primary stations.
 * <p>
 *
 * Usage in protocol:
 * <ul>
 * <li>ACK (0xE5): Frame received correctly and processed</li>
 * <li>NACK (0xA2): Frame received with errors or rejected</li>
 * </ul>
 * 
 */
public class Iec101SingleCharFrame extends Iec101Frame {

    private static final int SINGLE_CHAR_FRAME_LENGTH = 1;
    private static final int BYTE_TO_UNSIGNED_MASK = 0xFF;
    private static final int BUFFER_POSITION_ZERO = 0;
    
    private final byte character;

    public Iec101SingleCharFrame(byte character) {
        super(FrameType.SINGLE_CHARACTER);
        this.character = character;
        
        if (character != ACK && character != NACK) {
            // Convert signed byte to unsigned int for hex display
            throw new IllegalArgumentException("Invalid single character: 0x" + 
                Integer.toHexString(character & BYTE_TO_UNSIGNED_MASK));
        }
    }

    public static Iec101SingleCharFrame createAck() {
        return new Iec101SingleCharFrame(ACK);
    }

    public static Iec101SingleCharFrame createNack() {
        return new Iec101SingleCharFrame(NACK);
    }

    @Override
    public int encode(byte[] buffer, IEC60870Settings settings) {
        buffer[BUFFER_POSITION_ZERO] = character;
        return SINGLE_CHAR_FRAME_LENGTH;
    }

    public byte getCharacter() {
        return character;
    }

    public boolean isAck() {
        return character == ACK;
    }

    public boolean isNack() {
        return character == NACK;
    }
}
