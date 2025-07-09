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
 * Represents a double-point information with quality descriptor (DIQ) information element.
 */
public class IeDoublePointWithQuality extends IeAbstractQuality {

    public enum DoublePointInformation {
        INDETERMINATE_OR_INTERMEDIATE,
        OFF,
        ON,
        INDETERMINATE;
    }

    public IeDoublePointWithQuality(DoublePointInformation dpi, boolean blocked, boolean substituted,
            boolean notTopical, boolean invalid) {
        super(blocked, substituted, notTopical, invalid);

        switch (dpi) {
        case INDETERMINATE_OR_INTERMEDIATE:
            break;
        case OFF:
            value |= 0x01;
            break;
        case ON:
            value |= 0x02;
            break;
        case INDETERMINATE:
            value |= 0x03;
            break;
        }
    }

    IeDoublePointWithQuality(DataInputStream is) throws IOException {
        super(is);
    }

    public DoublePointInformation getDoublePointInformation() {
        switch (value & 0x03) {
        case 0:
            return DoublePointInformation.INDETERMINATE_OR_INTERMEDIATE;
        case 1:
            return DoublePointInformation.OFF;
        case 2:
            return DoublePointInformation.ON;
        default:
            return DoublePointInformation.INDETERMINATE;
        }
    }

    @Override
    public String toString() {
        return "Double Point: " + getDoublePointInformation() + ", " + super.toString();
    }
}
