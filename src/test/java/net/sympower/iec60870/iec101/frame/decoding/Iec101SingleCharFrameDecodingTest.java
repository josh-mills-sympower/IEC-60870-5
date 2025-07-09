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
import net.sympower.iec60870.iec101.frame.Iec101SingleCharFrame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static net.sympower.iec60870.iec101.frame.Iec101FrameTestUtils.*;

public class Iec101SingleCharFrameDecodingTest {


    @Test
    public void testDecodeSingleCharFrame_AckCharacter() {
        Iec101SingleCharFrame frame = whenSingleCharFrameIsCreated(ACK_BYTE);

        thenSingleCharFrameIsAck(frame);
    }

    @Test
    public void testDecodeSingleCharFrame_NackCharacter() {
        Iec101SingleCharFrame frame = whenSingleCharFrameIsCreated(NACK_BYTE);

        thenSingleCharFrameIsNack(frame);
    }

    private static Iec101SingleCharFrame whenSingleCharFrameIsCreated(byte charByte) {
        return new Iec101SingleCharFrame(charByte);
    }

    private static void thenSingleCharFrameIsAck(Iec101SingleCharFrame frame) {
        assertTrue("Should be identified as ACK", frame.isAck());
    }

    private static void thenSingleCharFrameIsNack(Iec101SingleCharFrame frame) {
        assertTrue("Should be identified as NACK", frame.isNack());
    }
}
