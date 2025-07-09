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
package net.sympower.iec60870.iec104.connection;

import net.sympower.iec60870.common.IEC60870Settings;

/**
 * Settings for TCP server configuration.
 * 
 * @since 2.0
 */
public class Iec104ServerSettings extends IEC60870Settings {
    
    private int port = 2404;
    private int maxConnections = 100;
    private String bindAddress = null;
    
    // IEC 60870-5-104 Specific Settings
    private final Iec104Settings iec104Settings = new Iec104Settings();

    public Iec104ServerSettings() {
        super();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    // IEC 60870-5-104 Specific Parameter Delegation

    public int getMaxTimeNoAckReceived() {
        return iec104Settings.getMaxTimeNoAckReceived();
    }

    public void setMaxTimeNoAckReceived(int maxTimeNoAckReceived) {
        this.iec104Settings.setMaxTimeNoAckReceived(maxTimeNoAckReceived);
    }

    public int getMaxTimeNoAckSent() {
        return iec104Settings.getMaxTimeNoAckSent();
    }

    public void setMaxTimeNoAckSent(int maxTimeNoAckSent) {
        this.iec104Settings.setMaxTimeNoAckSent(maxTimeNoAckSent);
    }

    public int getMaxIdleTime() {
        return iec104Settings.getMaxIdleTime();
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.iec104Settings.setMaxIdleTime(maxIdleTime);
    }

    public int getMaxUnconfirmedIPdusReceived() {
        return iec104Settings.getMaxUnconfirmedIPdusReceived();
    }

    public void setMaxUnconfirmedIPdusReceived(int maxUnconfirmedIPdusReceived) {
        this.iec104Settings.setMaxUnconfirmedIPdusReceived(maxUnconfirmedIPdusReceived);
    }

    public int getMaxNumOfOutstandingIPdus() {
        return iec104Settings.getMaxNumOfOutstandingIPdus();
    }

    public void setMaxNumOfOutstandingIPdus(int maxNumOfOutstandingIPdus) {
        this.iec104Settings.setMaxNumOfOutstandingIPdus(maxNumOfOutstandingIPdus);
    }

    
    /**
     * Gets the IEC-104 specific settings object.
     * 
     * @return the IEC-104 settings
     */
    public Iec104Settings getIec104Settings() {
        return iec104Settings;
    }
}
