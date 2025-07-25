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
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a double command (DCO) information element.
 */
public class IeDoubleCommand extends IeAbstractQualifierOfCommand {

    public enum DoubleCommandState {
        NOT_PERMITTED_A(0),
        OFF(1),
        ON(2),
        NOT_PERMITTED_B(3);

        private final int id;

        private static final Map<Integer, DoubleCommandState> idMap = new HashMap<>();

        static {
            for (DoubleCommandState enumInstance : DoubleCommandState.values()) {
                if (idMap.put(enumInstance.getId(), enumInstance) != null) {
                    throw new IllegalArgumentException("duplicate ID: " + enumInstance.getId());
                }
            }
        }

        private DoubleCommandState(int id) {
            this.id = id;
        }

        /**
         * Returns the ID of this DoubleCommandState.
         * 
         * @return the ID
         */
        public int getId() {
            return id;
        }

        /**
         * Returns the DoubleCommandState that corresponds to the given ID. Returns <code>null</code> if no
         * DoubleCommandState with the given ID exists.
         * 
         * @param id
         *            the ID
         * @return the DoubleCommandState that corresponds to the given ID
         */
        public static DoubleCommandState getInstance(int id) {
            return idMap.get(id);
        }

    }

    /**
     * Create the Double Command Information Element.
     * 
     * @param commandState
     *            the command state
     * @param qualifier
     *            the qualifier
     * @param select
     *            true if select, false if execute
     */
    public IeDoubleCommand(DoubleCommandState commandState, int qualifier, boolean select) {
        super(qualifier, select);

        value |= commandState.getId();
    }

    IeDoubleCommand(DataInputStream is) throws IOException {
        super(is);
    }

    public DoubleCommandState getCommandState() {
        return DoubleCommandState.getInstance(value & 0x03);
    }

    @Override
    public String toString() {
        return "Double Command state: " + getCommandState() + ", " + super.toString();
    }

}
