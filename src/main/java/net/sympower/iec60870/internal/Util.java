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
package net.sympower.iec60870.internal;

/**
 * Class offering static utility functions.
 */
public class Util {

    /**
     * Returns the Information Object Address (IOA) calculated from the given bytes. The first byte is the least
     * significant byte of the IOA.
     * 
     * @param byte1
     *            the first byte
     * @param byte2
     *            the second byte
     * @param byte3
     *            the third byte
     * @return the IOA
     */
    public static int convertToInformationObjectAddress(int byte1, int byte2, int byte3) {
        return byte1 + (byte2 << 8) + (byte3 << 16);
    }

    /**
     * Returns the Common Address (CA) calculated from the given bytes. The first byte is the least significant byte of
     * the CA.
     * 
     * @param byte1
     *            the first byte
     * @param byte2
     *            the second byte
     * @return the CA
     */
    public static int convertToCommonAddress(int byte1, int byte2) {
        return byte1 + (byte2 << 8);
    }

    private Util() {
    }

}
