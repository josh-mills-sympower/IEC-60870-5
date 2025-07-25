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
package net.sympower.iec60870.common.elements;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import net.sympower.iec60870.common.elements.IeBinaryCounterReading.Flag;
import net.sympower.iec60870.internal.ExtendedDataInputStream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class IeBinaryCounterReadingTest {

    @Test
    public void t1_initialization() throws Exception {
        int counterReading = -300;
        int sequenceNumber = 4;
        IeBinaryCounterReading binaryCounterReading = new IeBinaryCounterReading(counterReading, sequenceNumber,
                                                                                 Flag.CARRY, Flag.INVALID);

        assertEquals(counterReading, binaryCounterReading.getCounterReading());
        assertEquals(sequenceNumber, binaryCounterReading.getSequenceNumber());

        assertTrue(binaryCounterReading.getFlags().contains(Flag.CARRY));
        assertTrue(binaryCounterReading.getFlags().contains(Flag.INVALID));
        assertFalse(binaryCounterReading.getFlags().contains(Flag.COUNTER_ADJUSTED));
    }

    @Test
    public void t2_encoding() throws Exception {
        int counterReading = -300;
        int sequenceNumber = 4;
        IeBinaryCounterReading binaryCounterReading = new IeBinaryCounterReading(counterReading, sequenceNumber,
                Flag.CARRY, Flag.INVALID);

        byte[] buffer = new byte[5];
        int length = binaryCounterReading.encode(buffer, 0);

        assertEquals(5, length);
        assertArrayEquals(new byte[] { -44, -2, -1, -1, -92 }, buffer);
    }

    @Test
    public void t3_encoding_symmetry() throws Exception {
        int counterReading = -300;
        int sequenceNumber = 4;
        IeBinaryCounterReading binaryCounterReading = new IeBinaryCounterReading(counterReading, sequenceNumber,
                Flag.CARRY, Flag.INVALID);

        byte[] buffer = new byte[5];
        int length = binaryCounterReading.encode(buffer, 0);
        assertEquals(5, length);

        IeBinaryCounterReading binaryCounterReadingD = decode(buffer);

        assertEquals(counterReading, binaryCounterReadingD.getCounterReading());
        assertEquals(sequenceNumber, binaryCounterReadingD.getSequenceNumber());

        assertTrue(binaryCounterReadingD.getFlags().contains(Flag.CARRY));
        assertTrue(binaryCounterReadingD.getFlags().contains(Flag.INVALID));
        assertFalse(binaryCounterReadingD.getFlags().contains(Flag.COUNTER_ADJUSTED));
    }

    public Object t4_data() {
        Object[] p1 = { 0x80, Flag.INVALID };
        Object[] p2 = { 0x40, Flag.COUNTER_ADJUSTED };
        Object[] p3 = { 0x20, Flag.CARRY };
        return new Object[][] { p1, p2, p3 };
    }

    @Test
    @Parameters(method = "t4_data")
    public void t4(int flagTag, Flag f) throws Exception {
        byte[] buffer = { -44, -2, -1, -1, (byte) flagTag };
        IeBinaryCounterReading d = decode(buffer);

        EnumSet<Flag> es = EnumSet.allOf(Flag.class);
        assertTrue("Flag is somehow not there..?", es.remove(f));
        assertTrue("Flag was not represented..", d.getFlags().contains(f));

        assertFalse("Also contained flag, which should not have been there.", es.removeAll(d.getFlags()));
    }

    private static IeBinaryCounterReading decode(byte[] buffer) throws IOException {
        IeBinaryCounterReading binaryCounterReadingD;
        try (ExtendedDataInputStream is = new ExtendedDataInputStream(new ByteArrayInputStream(buffer));) {
            binaryCounterReadingD = IeBinaryCounterReading.decode(is);
        }
        return binaryCounterReadingD;
    }

}
