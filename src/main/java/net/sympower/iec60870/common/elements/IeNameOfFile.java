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
 * Represents a name of file (NOF) information element.
 */
public class IeNameOfFile extends InformationElement {

    private final int value;

    public IeNameOfFile(int value) {
        this.value = value;
    }

    static IeNameOfFile decode(DataInputStream is) throws IOException {
        int value = is.readUnsignedByte() | (is.readUnsignedByte() << 8);
        return new IeNameOfFile(value);
    }

    @Override
    int encode(byte[] buffer, int i) {
        buffer[i++] = (byte) value;
        buffer[i] = (byte) (value >> 8);

        return 2;

    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Name of file: " + value;
    }
}
