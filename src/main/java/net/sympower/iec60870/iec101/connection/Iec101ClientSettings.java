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
package net.sympower.iec60870.iec101.connection;

import net.sympower.iec60870.common.IEC60870Settings;

public class Iec101ClientSettings extends IEC60870Settings {
    
    private int maxRetries = 3;
    private long ackTimeoutMs = 200;
    private long initializationTimeoutMs = 5000;
    private long handshakePollIntervalMs = 1000;
    private long pollingIntervalMs = 1000;

    public Iec101ClientSettings() {
        super();
    }


    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getAckTimeoutMs() {
        return ackTimeoutMs;
    }

    public void setAckTimeoutMs(long ackTimeoutMs) {
        this.ackTimeoutMs = ackTimeoutMs;
    }

    public long getInitializationTimeoutMs() {
        return initializationTimeoutMs;
    }

    public void setInitializationTimeoutMs(long initializationTimeoutMs) {
        this.initializationTimeoutMs = initializationTimeoutMs;
    }

    public long getHandshakePollIntervalMs() {
        return handshakePollIntervalMs;
    }

    public void setHandshakePollIntervalMs(long handshakePollIntervalMs) {
        this.handshakePollIntervalMs = handshakePollIntervalMs;
    }
    
    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }
    
    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }
}
