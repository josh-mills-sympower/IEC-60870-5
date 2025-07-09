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
 * Represents a qualifier of counter interrogation (QCC) information element.
 */
public class IeQualifierOfCounterInterrogation extends InformationElement {

    private final int request;
    private final int freeze;

    public IeQualifierOfCounterInterrogation(int request, int freeze) {
        this.request = request;
        this.freeze = freeze;
    }

    IeQualifierOfCounterInterrogation(DataInputStream is) throws IOException {
        int b1 = (is.readByte() & 0xff);
        request = b1 & 0x3f;
        freeze = (b1 >> 6) & 0x03;
    }

    @Override
    int encode(byte[] buffer, int i) {
        buffer[i] = (byte) (request | (freeze << 6));
        return 1;
    }

    public int getRequest() {
        return request;
    }

    public int getFreeze() {
        return freeze;
    }

    @Override
    public String toString() {
        return "Qualifier of counter interrogation, request: " + request + ", freeze: " + freeze;
    }
}
