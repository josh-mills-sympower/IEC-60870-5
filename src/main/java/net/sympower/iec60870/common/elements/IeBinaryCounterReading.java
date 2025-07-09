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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import net.sympower.iec60870.internal.ExtendedDataInputStream;

/**
 * Represents a binary counter reading (BCR) information element.
 */
public class IeBinaryCounterReading extends InformationElement {

    private final int counterReading;
    private final int sequenceNumber;

    private final Set<Flag> flags;

    public enum Flag {
        CARRY(0x20),
        COUNTER_ADJUSTED(0x40),
        INVALID(0x80);

        private int mask;

        private Flag(int mask) {
            this.mask = mask;
        }

        private static Set<Flag> flagsFor(byte b) {
            EnumSet<Flag> s = EnumSet.allOf(Flag.class);

            Iterator<Flag> iter = s.iterator();

            while (iter.hasNext()) {
                int mask2 = iter.next().mask;
                if ((mask2 & b) != mask2) {
                    iter.remove();
                }
            }

            return s;
        }

    }

    public IeBinaryCounterReading(int counterReading, int sequenceNumber, Set<Flag> flags) {
        this.counterReading = counterReading;
        this.sequenceNumber = sequenceNumber;
        this.flags = flags;
    }

    public IeBinaryCounterReading(int counterReading, int sequenceNumber) {
        this(counterReading, sequenceNumber, EnumSet.noneOf(Flag.class));
    }

    public IeBinaryCounterReading(int counterReading, int sequenceNumber, Flag firstFlag, Flag... flag) {
        this(counterReading, sequenceNumber, EnumSet.of(firstFlag, flag));
    }

    static IeBinaryCounterReading decode(ExtendedDataInputStream is) throws IOException {

        int counterReading = is.readLittleEndianInt();

        byte b0 = is.readByte();

        int sequenceNumber = b0 & 0x1f;

        Set<Flag> flags = Flag.flagsFor(b0);
        return new IeBinaryCounterReading(counterReading, sequenceNumber, flags);

    }

    @Override
    int encode(byte[] buffer, int i) {

        ByteBuffer buf = ByteBuffer.wrap(buffer, i, buffer.length - i);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(counterReading).put(seq());

        return buf.position() - i;

    }

    private byte seq() {
        byte v = (byte) sequenceNumber;
        for (Flag flag : flags) {
            v |= flag.mask;
        }

        return v;
    }

    public int getCounterReading() {
        return counterReading;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Set<Flag> getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        return "Binary counter reading: " + counterReading + ", seq num: " + sequenceNumber + ", flags: " + flags;
    }
}
