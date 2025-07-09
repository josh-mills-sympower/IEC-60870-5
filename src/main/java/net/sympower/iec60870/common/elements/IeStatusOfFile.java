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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a status of file (SOF) information element.
 */
public class IeStatusOfFile extends InformationElement {

    private final int status;
    private final Set<Flag> flags;

    public enum Flag {
        LAST_FILE_OF_DIRECTORY(0x20),
        NAME_DEFINES_DIRECTORY(0x40),
        TRANSFER_IS_ACTIVE(0x80);

        private int mask;

        private Flag(int mask) {
            this.mask = mask;
        }

        private static Set<Flag> flagsFor(int b) {
            HashSet<Flag> res = new HashSet<>();
            for (Flag v : values()) {
                if ((v.mask & b) != v.mask) {
                    continue;
                }
                res.add(v);
            }
            return res;
        }

    }

    public IeStatusOfFile(int status, Flag... flags) {
        this(status, new HashSet<>(Arrays.asList(flags)));
    }

    public IeStatusOfFile(int status, Set<Flag> flags) {
        this.status = status;
        this.flags = flags;
    }

    static IeStatusOfFile decode(DataInputStream is) throws IOException {
        int b1 = is.readUnsignedByte();
        int status = b1 & 0x1f;

        Set<Flag> flags = Flag.flagsFor(b1);

        return new IeStatusOfFile(status, flags);
    }

    @Override
    int encode(byte[] buffer, int i) {
        buffer[i] = (byte) status;
        for (Flag f : flags) {
            buffer[i] |= (byte) f.mask;
        }
        return 1;
    }

    public int getStatus() {
        return status;
    }

    public Set<Flag> getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        return "Status of file: " + status + ", last file of directory: " + flags.contains(Flag.LAST_FILE_OF_DIRECTORY)
                + ", name defines directory: " + flags.contains(Flag.NAME_DEFINES_DIRECTORY) + ", transfer is active: "
                + flags.contains(Flag.TRANSFER_IS_ACTIVE);
    }
}
