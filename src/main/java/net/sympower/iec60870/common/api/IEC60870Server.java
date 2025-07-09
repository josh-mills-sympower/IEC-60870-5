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

import java.io.IOException;

/**
 * Represents an IEC 60870 server that accepts client connections.
 * Implementations provide TCP or serial transport.
 * 
 * @since 2.0
 */
public interface IEC60870Server {

    /**
     * Starts the server and begins accepting client connections.
     * 
     * @param listener the listener to handle new connections
     * @throws IOException if the server cannot be started
     */
    void start(IEC60870ServerListener listener) throws IOException;

    /**
     * Stops the server and closes all active connections.
     */
    void stop();

    /**
     * Returns the port number this server is bound to.
     * For serial servers, returns -1.
     * 
     * @return the port number, or -1 for serial servers
     */
    int getPort();
}