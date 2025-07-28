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
package net.sympower.iec60870.iec101.api;

import com.fazecast.jSerialComm.SerialPort;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.iec101.connection.Iec101ClientConnection;
import net.sympower.iec60870.iec101.connection.Iec101ClientSettings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Iec101ClientBuilder {

    private final String portName;
    
    private int baudRate = 9600;
    private int dataBits = 8;
    private int stopBits = SerialPort.ONE_STOP_BIT;
    private int parity = SerialPort.NO_PARITY;
    private int messageFragmentTimeout = 3000;
    
    private int linkAddress = 1; // Link address for IEC-101
    private int linkAddressLength = 2; // Link address length (1-2 bytes)
    private int cotFieldLength = 2; // Cause of Transmission field length (1 or 2 bytes)
    private int ioaFieldLength = 3; // Information Object Address field length (1, 2, or 3 bytes)

    private int commonAddress = 1; // Identifies the station/RTU in the network

    private int maxRetries = 3;
    private long ackTimeoutMs = 5000; // Increased from 200ms to 5 seconds
    private long initializationTimeoutMs = 30000; // Increased from 5 to 30 seconds
    private long handshakePollIntervalMs = 1000;
    private int interFrameDelayMs = 0;
    
    private long pollingIntervalMs = 1000;

    public Iec101ClientBuilder(String portName) {
        this.portName = portName;
    }

    public Iec101ClientBuilder baudRate(int baudRate) {
        this.baudRate = baudRate;
        return this;
    }

    public Iec101ClientBuilder dataBits(int dataBits) {
        this.dataBits = dataBits;
        return this;
    }

    public Iec101ClientBuilder stopBits(int stopBits) {
        this.stopBits = stopBits;
        return this;
    }

    public Iec101ClientBuilder parity(int parity) {
        this.parity = parity;
        return this;
    }

    public Iec101ClientBuilder messageFragmentTimeout(int timeoutMs) {
        this.messageFragmentTimeout = timeoutMs;
        return this;
    }

    public Iec101ClientBuilder commonAddress(int commonAddress) {
        this.commonAddress = commonAddress;
        return this;
    }

    public Iec101ClientBuilder linkAddress(int linkAddress) {
        this.linkAddress = linkAddress;
        return this;
    }

    public Iec101ClientBuilder linkAddressLength(int linkAddressLength) {
        if (linkAddressLength < 1 || linkAddressLength > 2) {
            throw new IllegalArgumentException("Link address length must be 1 or 2 bytes");
        }
        this.linkAddressLength = linkAddressLength;
        return this;
    }

    public Iec101ClientBuilder cotFieldLength(int length) {
        if (length != 1 && length != 2) {
            throw new IllegalArgumentException("COT field length must be 1 or 2");
        }
        this.cotFieldLength = length;
        return this;
    }

    public Iec101ClientBuilder ioaFieldLength(int length) {
        if (length < 1 || length > 3) {
            throw new IllegalArgumentException("IOA field length must be 1, 2, or 3");
        }
        this.ioaFieldLength = length;
        return this;
    }

    public Iec101ClientBuilder maxRetries(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries must be non-negative");
        }
        this.maxRetries = maxRetries;
        return this;
    }

    public Iec101ClientBuilder ackTimeoutMs(long ackTimeoutMs) {
        if (ackTimeoutMs <= 0) {
            throw new IllegalArgumentException("ACK timeout must be positive");
        }
        this.ackTimeoutMs = ackTimeoutMs;
        return this;
    }

    public Iec101ClientBuilder initializationTimeoutMs(long initializationTimeoutMs) {
        if (initializationTimeoutMs <= 0) {
            throw new IllegalArgumentException("Initialization timeout must be positive");
        }
        this.initializationTimeoutMs = initializationTimeoutMs;
        return this;
    }

    public Iec101ClientBuilder handshakePollIntervalMs(long handshakePollIntervalMs) {
        if (handshakePollIntervalMs <= 0) {
            throw new IllegalArgumentException("Handshake poll interval must be positive");
        }
        this.handshakePollIntervalMs = handshakePollIntervalMs;
        return this;
    }

    public Iec101ClientBuilder interFrameDelayMs(int interFrameDelayMs) {
        if (interFrameDelayMs < 0) {
            throw new IllegalArgumentException("Inter-frame delay must be non-negative");
        }
        this.interFrameDelayMs = interFrameDelayMs;
        return this;
    }
    
    public Iec101ClientBuilder pollingIntervalMs(long pollingIntervalMs) {
        if (pollingIntervalMs <= 0) {
            throw new IllegalArgumentException("Polling interval must be positive");
        }
        this.pollingIntervalMs = pollingIntervalMs;
        return this;
    }


    public Iec101ClientConnection build() throws IOException {
        SerialPort serialPort = openSerialPort();
        IEC60870Settings settings = createConnectionSettings();
        Iec101ClientSettings clientSettings = createClientSettings();
        
        return createConnection(serialPort, settings, clientSettings);
    }
    
    private SerialPort openSerialPort() throws IOException {
        SerialPort serialPort = SerialPort.getCommPort(portName);
        configureSerialPort(serialPort);
        
        if (!serialPort.openPort()) {
            throw new IOException("Failed to open serial port: " + portName);
        }
        
        return serialPort;
    }
    
    private void configureSerialPort(SerialPort serialPort) {
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 300000, 0); // 5 minutes
    }
    
    private IEC60870Settings createConnectionSettings() {
        IEC60870Settings settings = new IEC60870Settings();
        settings.setMessageFragmentTimeout(messageFragmentTimeout);
        settings.setCotFieldLength(cotFieldLength);
        settings.setIoaFieldLength(ioaFieldLength);
        settings.setLinkAddressLength(linkAddressLength);
        settings.setInterFrameDelayMs(interFrameDelayMs);
        return settings;
    }
    
    private Iec101ClientSettings createClientSettings() {
        Iec101ClientSettings clientSettings = new Iec101ClientSettings();
        clientSettings.setMaxRetries(maxRetries);
        clientSettings.setAckTimeoutMs(ackTimeoutMs);
        clientSettings.setInitializationTimeoutMs(initializationTimeoutMs);
        clientSettings.setHandshakePollIntervalMs(handshakePollIntervalMs);
        clientSettings.setPollingIntervalMs(pollingIntervalMs);
        return clientSettings;
    }
    
    private Iec101ClientConnection createConnection(SerialPort serialPort, IEC60870Settings settings, Iec101ClientSettings clientSettings) {
        return new Iec101ClientConnection(
            new DataInputStream(serialPort.getInputStream()),
            new DataOutputStream(serialPort.getOutputStream()),
            settings, 
            linkAddress,
            clientSettings);
    }
}
