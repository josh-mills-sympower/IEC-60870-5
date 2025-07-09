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
package net.sympower.iec60870.common.api;

import net.sympower.iec60870.common.ASdu;

import java.io.IOException;

/**
 * Listener interface for IEC 60870 connection events.
 * Implementations of this interface handle incoming ASDUs and connection state changes.
 * 
 * @since 2.0
 */
public interface IEC60870EventListener {

    /**
     * Called when an ASDU is received from the remote station.
     * 
     * @param asdu the received ASDU
     */
    void onAsduReceived(ASdu asdu);

    /**
     * Called when the connection is ready for data transfer.
     * For TCP connections, this is called after STARTDT confirmation is received.
     */
    void onConnectionReady();

    /**
     * Called when the connection is lost or closed.
     * 
     * @param cause the exception that caused the connection loss, or null if closed normally
     */
    void onConnectionLost(IOException cause);
}
