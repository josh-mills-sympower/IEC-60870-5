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

import net.sympower.iec60870.iec101.connection.Iec101ServerSettings;

import java.io.IOException;

public class Iec101ServerBuilder {

    private final String portName;
    
    private int baudRate = 9600;
    private int dataBits = 8;
    private int stopBits = 1; // SerialPort.ONE_STOP_BIT
    private int parity = 0; // SerialPort.NO_PARITY
    private int messageFragmentTimeout = 300000; // 5 minutes instead of 5 seconds
    
    private int linkAddress = 1;
    private int cotFieldLength = 2;
    private int ioaFieldLength = 3;

    public Iec101ServerBuilder(String portName) {
        this.portName = portName;
    }

    public Iec101ServerBuilder baudRate(int baudRate) {
        this.baudRate = baudRate;
        return this;
    }

    public Iec101ServerBuilder dataBits(int dataBits) {
        this.dataBits = dataBits;
        return this;
    }

    public Iec101ServerBuilder stopBits(int stopBits) {
        this.stopBits = stopBits;
        return this;
    }

    public Iec101ServerBuilder parity(int parity) {
        this.parity = parity;
        return this;
    }

    public Iec101ServerBuilder messageFragmentTimeout(int timeoutMs) {
        this.messageFragmentTimeout = timeoutMs;
        return this;
    }

    public Iec101ServerBuilder linkAddress(int linkAddress) {
        this.linkAddress = linkAddress;
        return this;
    }

    public Iec101ServerBuilder cotFieldLength(int length) {
        if (length != 1 && length != 2) {
            throw new IllegalArgumentException("COT field length must be 1 or 2");
        }
        this.cotFieldLength = length;
        return this;
    }

    public Iec101ServerBuilder ioaFieldLength(int length) {
        if (length < 1 || length > 3) {
            throw new IllegalArgumentException("IOA field length must be 1, 2, or 3");
        }
        this.ioaFieldLength = length;
        return this;
    }

    public Iec101Server build() throws IOException {
        Iec101ServerSettings settings = createServerSettings();
        return new Iec101Server(settings);
    }
    
    private Iec101ServerSettings createServerSettings() {
        Iec101ServerSettings settings = new Iec101ServerSettings();
        configureSerialPortSettings(settings);
        configureProtocolSettings(settings);
        return settings;
    }
    
    private void configureSerialPortSettings(Iec101ServerSettings settings) {
        settings.setPortName(portName);
        settings.setBaudRate(baudRate);
        settings.setDataBits(dataBits);
        settings.setStopBits(stopBits);
        settings.setParity(parity);
        settings.setMessageFragmentTimeout(messageFragmentTimeout);
    }
    
    private void configureProtocolSettings(Iec101ServerSettings settings) {
        settings.setCotFieldLength(cotFieldLength);
        settings.setIoaFieldLength(ioaFieldLength);
        settings.setLinkAddress(linkAddress);
    }

}
