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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Represents the segment of a file as transferred by ASDUs of type F_SG_NA_1 (125).
 */
public class IeFileSegment extends InformationElement {

    private final byte[] segment;
    private final int offset;
    private final int length;

    public IeFileSegment(byte[] segment, int offset, int length) {
        this.segment = segment;
        this.offset = offset;
        this.length = length;
    }

    IeFileSegment(DataInputStream is) throws IOException {

        length = (is.readByte() & 0xff);
        segment = new byte[length];

        is.readFully(segment);
        offset = 0;
    }

    @Override
    int encode(byte[] buffer, int i) {

        buffer[i++] = (byte) length;

        System.arraycopy(segment, offset, buffer, i, length);

        return length + 1;

    }

    public byte[] getSegment() {
        return segment;
    }

    @Override
    public String toString() {
        return "File segment of length: " + length;
    }
}
