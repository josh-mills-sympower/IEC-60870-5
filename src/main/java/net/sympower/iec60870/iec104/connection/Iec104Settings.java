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

/**
 * IEC 60870-5-104 specific protocol settings including timeout and flow control parameters.
 * These settings are specific to TCP/IP communication and not applicable to IEC 60870-5-101.
 * 
 * @since 2.0
 */
public class Iec104Settings {
    
    // IEC 60870-5-104 Timeout Parameters
    private int maxTimeNoAckReceived;    // T1: Timeout for acknowledgment (default 15s)
    private int maxTimeNoAckSent;        // T2: Timeout for supervisory frame (default 10s) 
    private int maxIdleTime;             // T3: Test frame interval when idle (default 20s)
    
    // IEC 60870-5-104 Flow Control Parameters  
    private int maxUnconfirmedIPdusReceived; // w: Max unconfirmed I-PDUs received (default 8)
    private int maxNumOfOutstandingIPdus;    // k: Max outstanding I-PDUs sent (default 12)
    

    /**
     * Default constructor with standard IEC 60870-5-104 parameter values.
     */
    public Iec104Settings() {
        this.maxTimeNoAckReceived = 15000;
        this.maxTimeNoAckSent = 10000;
        this.maxIdleTime = 20000;
        this.maxUnconfirmedIPdusReceived = 8;
        this.maxNumOfOutstandingIPdus = 12;
    }

    /**
     * Constructor from server settings.
     */
    public Iec104Settings(Iec104ServerSettings serverSettings) {
        this.maxTimeNoAckReceived = serverSettings.getMaxTimeNoAckReceived();
        this.maxTimeNoAckSent = serverSettings.getMaxTimeNoAckSent();
        this.maxIdleTime = serverSettings.getMaxIdleTime();
        this.maxUnconfirmedIPdusReceived = serverSettings.getMaxUnconfirmedIPdusReceived();
        this.maxNumOfOutstandingIPdus = serverSettings.getMaxNumOfOutstandingIPdus();
    }

    /**
     * Constructor from client settings.
     */
    public Iec104Settings(Iec104ClientSettings clientSettings) {
        this.maxTimeNoAckReceived = clientSettings.getMaxTimeNoAckReceived();
        this.maxTimeNoAckSent = clientSettings.getMaxTimeNoAckSent();
        this.maxIdleTime = clientSettings.getMaxIdleTime();
        this.maxUnconfirmedIPdusReceived = clientSettings.getMaxUnconfirmedIPdusReceived();
        this.maxNumOfOutstandingIPdus = clientSettings.getMaxNumOfOutstandingIPdus();
    }

    // Getters

    public int getMaxTimeNoAckReceived() {
        return maxTimeNoAckReceived;
    }

    public int getMaxTimeNoAckSent() {
        return maxTimeNoAckSent;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public int getMaxUnconfirmedIPdusReceived() {
        return maxUnconfirmedIPdusReceived;
    }

    public int getMaxNumOfOutstandingIPdus() {
        return maxNumOfOutstandingIPdus;
    }


    // Setters

    public void setMaxTimeNoAckReceived(int maxTimeNoAckReceived) {
        this.maxTimeNoAckReceived = maxTimeNoAckReceived;
    }

    public void setMaxTimeNoAckSent(int maxTimeNoAckSent) {
        this.maxTimeNoAckSent = maxTimeNoAckSent;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setMaxUnconfirmedIPdusReceived(int maxUnconfirmedIPdusReceived) {
        this.maxUnconfirmedIPdusReceived = maxUnconfirmedIPdusReceived;
    }

    public void setMaxNumOfOutstandingIPdus(int maxNumOfOutstandingIPdus) {
        this.maxNumOfOutstandingIPdus = maxNumOfOutstandingIPdus;
    }

}
