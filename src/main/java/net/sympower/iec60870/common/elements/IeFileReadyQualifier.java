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
 * Represents a file ready qualifier (FRQ) information element.
 */
public class IeFileReadyQualifier extends InformationElement {

    private final int value;
    private final boolean negativeConfirm;

    public IeFileReadyQualifier(int value, boolean negativeConfirm) {
        this.value = value;
        this.negativeConfirm = negativeConfirm;
    }

    static InformationElement decode(DataInputStream is) throws IOException {
        int b1 = is.readUnsignedByte();
        int value = b1 & 0x7f;
        boolean negativeConfirm = ((b1 & 0x80) == 0x80);

        return new IeFileReadyQualifier(value, negativeConfirm);
    }

    @Override
    int encode(byte[] buffer, int i) {
        buffer[i] = (byte) value;
        if (negativeConfirm) {
            buffer[i] |= 0x80;
        }
        return 1;
    }

    public int getValue() {
        return value;
    }

    public boolean isNegativeConfirm() {
        return negativeConfirm;
    }

    @Override
    public String toString() {
        return "File ready qualifier: " + value + ", negative confirm: " + negativeConfirm;
    }

}
