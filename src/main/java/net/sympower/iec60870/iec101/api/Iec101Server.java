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
import net.sympower.iec60870.common.api.IEC60870Server;
import net.sympower.iec60870.common.api.IEC60870ServerListener;
import net.sympower.iec60870.iec101.connection.Iec101ServerConnection;
import net.sympower.iec60870.iec101.connection.Iec101ServerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class Iec101Server implements IEC60870Server {

    private static final Logger logger = LoggerFactory.getLogger(Iec101Server.class);
    
    private final Iec101ServerSettings settings;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private SerialPort serialPort;
    private Iec101ServerConnection activeConnection;
    private volatile IEC60870ServerListener serverListener;

    public Iec101Server(Iec101ServerSettings settings) {
        this.settings = settings;
    }

    @Override
    public void start(IEC60870ServerListener listener) throws IOException {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Server is already running");
        }

        this.serverListener = listener;
        
        openSerialPort();
        createServerConnection();
        notifyConnectionAccepted();
    }
    
    private void openSerialPort() throws IOException {
        logger.info("Opening serial port: {}", settings.getPortName());
        serialPort = SerialPort.getCommPort(settings.getPortName());
        configureSerialPort();
        
        if (!serialPort.openPort()) {
            throw new IOException("Failed to open serial port: " + settings.getPortName());
        }
        logger.info("Serial port opened successfully: {}", settings.getPortName());
    }
    
    private void configureSerialPort() {
        serialPort.setBaudRate(settings.getBaudRate());
        serialPort.setNumDataBits(settings.getDataBits());
        serialPort.setNumStopBits(settings.getStopBits());
        serialPort.setParity(settings.getParity());
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 300000, 0); // 5 minutes
    }
    
    private void createServerConnection() {
        IEC60870Settings connectionSettings = createConnectionSettings();
        
        activeConnection = new Iec101ServerConnection(
            new DataInputStream(serialPort.getInputStream()),
            new DataOutputStream(serialPort.getOutputStream()),
            connectionSettings,
            settings.getLinkAddress()
        );
        
        activeConnection.setConnectionCloseListener(() -> running.set(false));
    }
    
    private IEC60870Settings createConnectionSettings() {
        IEC60870Settings connectionSettings = new IEC60870Settings();
        connectionSettings.setMessageFragmentTimeout(settings.getMessageFragmentTimeout());
        connectionSettings.setCotFieldLength(settings.getCotFieldLength());
        connectionSettings.setIoaFieldLength(settings.getIoaFieldLength());
        return connectionSettings;
    }
    
    private void notifyConnectionAccepted() {
        if (serverListener != null) {
            serverListener.onConnectionAccepted(activeConnection);
        }
    }

    @Override
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (activeConnection != null) {
            activeConnection.close();
            activeConnection = null;
        }

        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            serialPort = null;
        }
    }

    @Override
    public int getPort() {
        return -1;
    }
}
