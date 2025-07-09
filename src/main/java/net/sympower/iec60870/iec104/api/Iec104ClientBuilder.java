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

import net.sympower.iec60870.iec104.connection.Iec104ClientConnection;
import net.sympower.iec60870.iec104.connection.Iec104ClientSettings;
import net.sympower.iec60870.iec104.connection.Iec104Settings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;


public class Iec104ClientBuilder {

    private final String hostname;
    private final int port;
    
    private int connectionTimeout = 30000; // 30 seconds
    private int messageFragmentTimeout = 5000; // 5 seconds
    private int cotFieldLength = 2;
    private int ioaFieldLength = 3;
    private int commonAddress = 1;
    
    private final Iec104Settings iec104Settings = new Iec104Settings();
    
    private int localPort = 0;
    private String localAddress = null;

    public Iec104ClientBuilder(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public Iec104ClientBuilder connectionTimeout(int timeoutMs) {
        this.connectionTimeout = timeoutMs;
        return this;
    }

    public Iec104ClientBuilder messageFragmentTimeout(int timeoutMs) {
        this.messageFragmentTimeout = timeoutMs;
        return this;
    }

    public Iec104ClientBuilder commonAddress(int commonAddress) {
        this.commonAddress = commonAddress;
        return this;
    }

    public Iec104ClientBuilder cotFieldLength(int length) {
        if (length != 1 && length != 2) {
            throw new IllegalArgumentException("COT field length must be 1 or 2");
        }
        this.cotFieldLength = length;
        return this;
    }

    public Iec104ClientBuilder ioaFieldLength(int length) {
        if (length < 1 || length > 3) {
            throw new IllegalArgumentException("IOA field length must be 1, 2, or 3");
        }
        this.ioaFieldLength = length;
        return this;
    }

    public Iec104ClientBuilder maxTimeNoAckReceived(int timeMs) {
        this.iec104Settings.setMaxTimeNoAckReceived(timeMs);
        return this;
    }

    public Iec104ClientBuilder maxTimeNoAckSent(int timeMs) {
        this.iec104Settings.setMaxTimeNoAckSent(timeMs);
        return this;
    }

    public Iec104ClientBuilder maxIdleTime(int timeMs) {
        this.iec104Settings.setMaxIdleTime(timeMs);
        return this;
    }

    public Iec104ClientBuilder maxUnconfirmedIPdusReceived(int count) {
        this.iec104Settings.setMaxUnconfirmedIPdusReceived(count);
        return this;
    }

    public Iec104ClientBuilder maxNumOfOutstandingIPdus(int count) {
        this.iec104Settings.setMaxNumOfOutstandingIPdus(count);
        return this;
    }

    public Iec104ClientBuilder localPort(int port) {
        this.localPort = port;
        return this;
    }

    public Iec104ClientBuilder localAddress(String address) {
        this.localAddress = address;
        return this;
    }

    public Iec104ClientConnection build() throws IOException {
        Socket socket = new Socket();

        InetSocketAddress localAddr = localAddress != null ? 
            new InetSocketAddress(localAddress, localPort) : 
            new InetSocketAddress(localPort);
        socket.bind(localAddr);

        InetSocketAddress remoteAddr = new InetSocketAddress(hostname, port);
        socket.connect(remoteAddr, connectionTimeout);
        
        Iec104ClientSettings settings = new Iec104ClientSettings();
        settings.setMessageFragmentTimeout(messageFragmentTimeout);
        settings.setCotFieldLength(cotFieldLength);
        settings.setIoaFieldLength(ioaFieldLength);
        settings.setConnectionTimeout(connectionTimeout);
        settings.setHostname(hostname);
        settings.setPort(port);
        
        settings.setMaxTimeNoAckReceived(iec104Settings.getMaxTimeNoAckReceived());
        settings.setMaxTimeNoAckSent(iec104Settings.getMaxTimeNoAckSent());
        settings.setMaxIdleTime(iec104Settings.getMaxIdleTime());
        settings.setMaxUnconfirmedIPdusReceived(iec104Settings.getMaxUnconfirmedIPdusReceived());
        settings.setMaxNumOfOutstandingIPdus(iec104Settings.getMaxNumOfOutstandingIPdus());
        
        return new Iec104ClientConnection(socket, settings);
    }
}
