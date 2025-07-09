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

public class Iec104ClientSettings extends IEC60870Settings {
    
    private String hostname;
    private int port = 2404;
    
    private final Iec104Settings iec104Settings = new Iec104Settings();

    public Iec104ClientSettings() {
        super();
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

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
    
    public Iec104Settings getIec104Settings() {
        return iec104Settings;
    }
}
