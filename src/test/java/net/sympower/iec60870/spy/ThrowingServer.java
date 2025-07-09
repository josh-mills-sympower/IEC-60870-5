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
package net.sympower.iec60870.spy;

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;
import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870EventListener;

import java.io.IOException;

public class ThrowingServer implements IEC60870EventListener, ServerSpy {

    private volatile boolean connectionReady = false;

    @Override
    public void onConnectionReady() {
        connectionReady = true;
    }

    @Override
    public void onAsduReceived(ASdu asdu) {
        // Always throw an exception to simulate frame handling failure
        throw new RuntimeException("Simulated exception during ASDU handling for testing NACK behavior");
    }

    @Override
    public void onConnectionLost(IOException cause) {
        // No action needed for test
    }

    @Override
    public boolean isConnectionReady() {
        return connectionReady;
    }

    @Override
    public boolean hasReceived(ASduType type) {
        // This listener always throws exceptions, so it never actually "receives" ASDUs
        return false;
    }

    @Override
    public boolean hasReceivedAnyCommand() {
        // This listener always throws exceptions, so it never actually receives commands
        return false;
    }

    @Override
    public void setServerConnection(IEC60870Connection connection) {
        // This listener doesn't need to track the connection
    }
}
