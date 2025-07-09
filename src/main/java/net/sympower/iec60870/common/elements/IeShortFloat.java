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
 * Represents a short floating point number (R32-IEEE STD 754) information element.
 */
public class IeShortFloat extends InformationElement {

    private final float value;

    public IeShortFloat(float value) {
        this.value = value;
    }

    IeShortFloat(DataInputStream is) throws IOException {
        value = Float.intBitsToFloat((is.readByte() & 0xff) | ((is.readByte() & 0xff) << 8)
                | ((is.readByte() & 0xff) << 16) | ((is.readByte() & 0xff) << 24));
    }

    @Override
    int encode(byte[] buffer, int i) {

        int tempVal = Float.floatToIntBits(value);
        buffer[i++] = (byte) tempVal;
        buffer[i++] = (byte) (tempVal >> 8);
        buffer[i++] = (byte) (tempVal >> 16);
        buffer[i] = (byte) (tempVal >> 24);

        return 4;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Short float value: " + value;
    }
}
