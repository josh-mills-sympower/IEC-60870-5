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
 * Represents a length of file or section (LOF) information element.
 */
public class IeLengthOfFileOrSection extends InformationElement {

    private final int value;

    public IeLengthOfFileOrSection(int value) {
        this.value = value;
    }

    static IeLengthOfFileOrSection decode(DataInputStream is) throws IOException {
        int value = 0;
        for (int i = 0; i < 3; ++i) {
            value |= is.readUnsignedByte() << (i * 8);
        }
        return new IeLengthOfFileOrSection(value);
    }

    @Override
    int encode(byte[] buffer, int i) {

        buffer[i++] = (byte) value;
        buffer[i++] = (byte) (value >> 8);
        buffer[i] = (byte) (value >> 16);

        return 3;

    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Length of file or section: " + value;
    }
}
