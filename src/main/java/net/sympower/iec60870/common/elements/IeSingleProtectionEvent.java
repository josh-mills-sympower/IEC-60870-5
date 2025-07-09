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
 * Represents a single event of protection equipment (SEP) information element.
 */
public class IeSingleProtectionEvent extends InformationElement {

    private int value;

    public enum EventState {
        INDETERMINATE,
        OFF,
        ON;
    }

    public IeSingleProtectionEvent(EventState eventState, boolean elapsedTimeInvalid, boolean blocked,
            boolean substituted, boolean notTopical, boolean eventInvalid) {

        value = 0;

        switch (eventState) {
        case OFF:
            value |= 0x01;
            break;
        case ON:
            value |= 0x02;
            break;
        default:
            break;
        }

        if (elapsedTimeInvalid) {
            value |= 0x08;
        }
        if (blocked) {
            value |= 0x10;
        }
        if (substituted) {
            value |= 0x20;
        }
        if (notTopical) {
            value |= 0x40;
        }
        if (eventInvalid) {
            value |= 0x80;
        }
    }

    IeSingleProtectionEvent(DataInputStream is) throws IOException {
        value = (is.readByte() & 0xff);
    }

    @Override
    int encode(byte[] buffer, int i) {
        buffer[i] = (byte) value;
        return 1;
    }

    public EventState getEventState() {
        switch (value & 0x03) {
        case 1:
            return EventState.OFF;
        case 2:
            return EventState.ON;
        default:
            return EventState.INDETERMINATE;
        }
    }

    public boolean isElapsedTimeInvalid() {
        return (value & 0x08) == 0x08;
    }

    public boolean isBlocked() {
        return (value & 0x10) == 0x10;
    }

    public boolean isSubstituted() {
        return (value & 0x20) == 0x20;
    }

    public boolean isNotTopical() {
        return (value & 0x40) == 0x40;
    }

    public boolean isEventInvalid() {
        return (value & 0x80) == 0x80;
    }

    @Override
    public String toString() {
        return "Single protection event, elapsed time invalid: " + isElapsedTimeInvalid() + ", blocked: " + isBlocked()
                + ", substituted: " + isSubstituted() + ", not topical: " + isNotTopical() + ", event invalid: "
                + isEventInvalid();
    }

}
