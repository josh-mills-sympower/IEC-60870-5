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
 * Represents a cause of initialization (COI) information element.
 */
public class IeCauseOfInitialization extends InformationElement {

    private final int value;
    private final boolean initAfterParameterChange;

    /**
     * Creates a COI (cause of initialization) information element.
     * 
     * @param value
     *            value between 0 and 127
     * @param initAfterParameterChange
     *            true if initialization after change of local parameters and false if initialization with unchanged
     *            local parameters
     */
    public IeCauseOfInitialization(int value, boolean initAfterParameterChange) {

        if (value < 0 || value > 127) {
            throw new IllegalArgumentException("Value has to be in the range 0..127");
        }

        this.value = value;
        this.initAfterParameterChange = initAfterParameterChange;

    }

    IeCauseOfInitialization(DataInputStream is) throws IOException {
        int b1 = (is.readByte() & 0xff);

        initAfterParameterChange = ((b1 & 0x80) == 0x80);

        value = b1 & 0x7f;

    }

    @Override
    int encode(byte[] buffer, int i) {

        if (initAfterParameterChange) {
            buffer[i] = (byte) (value | 0x80);
        }
        else {
            buffer[i] = (byte) value;
        }

        return 1;

    }

    public int getValue() {
        return value;
    }

    public boolean isInitAfterParameterChange() {
        return initAfterParameterChange;
    }

    @Override
    public String toString() {
        return "Cause of initialization: " + value + ", init after parameter change: " + initAfterParameterChange;
    }
}
