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
 * Represents a select and call qualifier (SCQ) information element.
 */
public class IeSelectAndCallQualifier extends InformationElement {

    private final int action;
    private final int notice;

    public IeSelectAndCallQualifier(int action, int notice) {
        this.action = action;
        this.notice = notice;
    }

    static IeSelectAndCallQualifier decode(DataInputStream is) throws IOException {
        int b1 = is.readUnsignedByte();

        int action = b1 & 0x0f;
        int notice = (b1 >> 4) & 0x0f;
        return new IeSelectAndCallQualifier(action, notice);
    }

    @Override
    int encode(byte[] buffer, int i) {
        buffer[i] = (byte) (action | (notice << 4));
        return 1;
    }

    public int getRequest() {
        return action;
    }

    public int getFreeze() {
        return notice;
    }

    @Override
    public String toString() {
        return "Select and call qualifier, action: " + action + ", notice: " + notice;
    }
}
