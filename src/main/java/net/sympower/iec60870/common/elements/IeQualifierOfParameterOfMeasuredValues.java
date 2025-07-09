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
 * Represents a qualifier of parameter of measured values (QPM) information element.
 */
public class IeQualifierOfParameterOfMeasuredValues extends InformationElement {

    private final int kindOfParameter;
    private final boolean change;
    private final boolean notInOperation;

    public IeQualifierOfParameterOfMeasuredValues(int kindOfParameter, boolean change, boolean notInOperation) {
        this.kindOfParameter = kindOfParameter;
        this.change = change;
        this.notInOperation = notInOperation;
    }

    IeQualifierOfParameterOfMeasuredValues(DataInputStream is) throws IOException {
        int b1 = (is.readByte() & 0xff);
        kindOfParameter = b1 & 0x3f;
        change = ((b1 & 0x40) == 0x40);
        notInOperation = ((b1 & 0x80) == 0x80);
    }

    @Override
    int encode(byte[] buffer, int i) {
        buffer[i] = (byte) kindOfParameter;
        if (change) {
            buffer[i] |= 0x40;
        }
        if (notInOperation) {
            buffer[i] |= 0x80;
        }
        return 1;
    }

    public int getKindOfParameter() {
        return kindOfParameter;
    }

    public boolean isChange() {
        return change;
    }

    public boolean isNotInOperation() {
        return notInOperation;
    }

    @Override
    public String toString() {
        return "Qualifier of parameter of measured values, kind of parameter: " + kindOfParameter + ", change: "
                + change + ", not in operation: " + notInOperation;
    }
}
