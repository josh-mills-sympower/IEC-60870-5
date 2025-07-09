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
package net.sympower.iec60870.iec104.api;

import net.sympower.iec60870.iec104.connection.Iec104ServerSettings;
import net.sympower.iec60870.iec104.connection.Iec104Settings;

import java.io.IOException;

public class Iec104ServerBuilder {

    private final int port;
    private int maxConnections = 100;
    private String bindAddress = null; // null = bind to all interfaces
    private int connectionTimeout = 30000;
    private int messageFragmentTimeout = 5000;
    private int cotFieldLength = 2;
    private int ioaFieldLength = 3;
    
    private final Iec104Settings iec104Settings = new Iec104Settings();

    public Iec104ServerBuilder(int port) {
        this.port = port;
    }


    public Iec104ServerBuilder maxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public Iec104ServerBuilder bindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
        return this;
    }

    public Iec104ServerBuilder connectionTimeout(int timeoutMs) {
        this.connectionTimeout = timeoutMs;
        return this;
    }

    public Iec104ServerBuilder messageFragmentTimeout(int timeoutMs) {
        this.messageFragmentTimeout = timeoutMs;
        return this;
    }

    public Iec104ServerBuilder cotFieldLength(int length) {
        if (length != 1 && length != 2) {
            throw new IllegalArgumentException("COT field length must be 1 or 2");
        }
        this.cotFieldLength = length;
        return this;
    }

    public Iec104ServerBuilder ioaFieldLength(int length) {
        if (length < 1 || length > 3) {
            throw new IllegalArgumentException("IOA field length must be 1, 2, or 3");
        }
        this.ioaFieldLength = length;
        return this;
    }

    /**
     * Sets the maximum time with no acknowledgment received (T1 parameter).
     *
     * @param timeMs time in milliseconds (default: 15000)
     * @return this builder for method chaining
     */
    public Iec104ServerBuilder maxTimeNoAckReceived(int timeMs) {
        this.iec104Settings.setMaxTimeNoAckReceived(timeMs);
        return this;
    }

    /**
     * Sets the maximum time with no acknowledgment sent (T2 parameter).
     *
     * @param timeMs time in milliseconds (default: 10000)
     * @return this builder for method chaining
     */
    public Iec104ServerBuilder maxTimeNoAckSent(int timeMs) {
        this.iec104Settings.setMaxTimeNoAckSent(timeMs);
        return this;
    }

    /**
     * Sets the maximum idle time for test frames (T3 parameter).
     * 
     * @param timeMs time in milliseconds (default: 20000)
     * @return this builder for method chaining
     */
    public Iec104ServerBuilder maxIdleTime(int timeMs) {
        this.iec104Settings.setMaxIdleTime(timeMs);
        return this;
    }

    /**
     * Sets the maximum number of unconfirmed I-PDUs received (w parameter).
     * 
     * @param count maximum count (default: 8)
     * @return this builder for method chaining
     */
    public Iec104ServerBuilder maxUnconfirmedIPdusReceived(int count) {
        this.iec104Settings.setMaxUnconfirmedIPdusReceived(count);
        return this;
    }

    /**
     * Sets the maximum number of outstanding I-PDUs sent (k parameter).
     * 
     * @param count maximum count (default: 12)
     * @return this builder for method chaining
     */
    public Iec104ServerBuilder maxNumOfOutstandingIPdus(int count) {
        this.iec104Settings.setMaxNumOfOutstandingIPdus(count);
        return this;
    }


    public Iec104Server build() throws IOException {
        Iec104ServerSettings settings = new Iec104ServerSettings();
        settings.setPort(port);
        settings.setMaxConnections(maxConnections);
        settings.setBindAddress(bindAddress);
        settings.setConnectionTimeout(connectionTimeout);
        settings.setMessageFragmentTimeout(messageFragmentTimeout);
        settings.setCotFieldLength(cotFieldLength);
        settings.setIoaFieldLength(ioaFieldLength);
        
        settings.setMaxTimeNoAckReceived(iec104Settings.getMaxTimeNoAckReceived());
        settings.setMaxTimeNoAckSent(iec104Settings.getMaxTimeNoAckSent());
        settings.setMaxIdleTime(iec104Settings.getMaxIdleTime());
        settings.setMaxUnconfirmedIPdusReceived(iec104Settings.getMaxUnconfirmedIPdusReceived());
        settings.setMaxNumOfOutstandingIPdus(iec104Settings.getMaxNumOfOutstandingIPdus());

        return new Iec104Server(settings);
    }
}
