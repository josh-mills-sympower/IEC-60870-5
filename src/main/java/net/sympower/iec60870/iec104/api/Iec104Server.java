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

import net.sympower.iec60870.common.api.IEC60870Server;
import net.sympower.iec60870.common.api.IEC60870ServerListener;
import net.sympower.iec60870.iec104.connection.Iec104ServerConnection;
import net.sympower.iec60870.iec104.connection.Iec104ServerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Iec104Server implements IEC60870Server {

    private static final Logger logger = LoggerFactory.getLogger(Iec104Server.class);
    
    private final Iec104ServerSettings settings;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, Iec104ServerConnection> activeConnections = new ConcurrentHashMap<>();
    
    private ServerSocket serverSocket;
    private ExecutorService serverExecutor;
    private volatile IEC60870ServerListener serverListener;

    public Iec104Server(Iec104ServerSettings settings) {
        this.settings = settings;
    }

    @Override
    public void start(IEC60870ServerListener listener) throws IOException {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Server is already running");
        }

        this.serverListener = listener;
        
        serverSocket = new ServerSocket();
        if (settings.getBindAddress() != null) {
            serverSocket.bind(new InetSocketAddress(settings.getBindAddress(), settings.getPort()));
        } else {
            serverSocket.bind(new InetSocketAddress(settings.getPort()));
        }
        
        serverSocket.setSoTimeout(1000);
        
        serverExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        
        serverExecutor.submit(this::serverLoop);
    }

    @Override
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        for (Iec104ServerConnection connection : activeConnections.values()) {
            connection.close();
        }
        activeConnections.clear();

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        if (serverExecutor != null) {
            serverExecutor.shutdown();
        }
    }

    @Override
    public int getPort() {
        if (serverSocket != null) {
            return serverSocket.getLocalPort();
        }
        return settings.getPort();
    }

    private void serverLoop() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleNewConnection(clientSocket);
            } catch (SocketTimeoutException e) {
                // Normal timeout, continue loop to check running flag
            } catch (IOException e) {
                if (running.get()) {
                    logger.error("Error accepting connection: {}", e.getMessage(), e);
                }
                break;
            }
        }
    }

    private void handleNewConnection(Socket clientSocket) {
        if (connectionCount.get() >= settings.getMaxConnections()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            return;
        }

        try {
            Iec104ServerSettings connectionSettings = new Iec104ServerSettings();
            connectionSettings.setMessageFragmentTimeout(settings.getMessageFragmentTimeout());
            connectionSettings.setCotFieldLength(settings.getCotFieldLength());
            connectionSettings.setIoaFieldLength(settings.getIoaFieldLength());
            connectionSettings.setMaxTimeNoAckReceived(settings.getMaxTimeNoAckReceived());
            connectionSettings.setMaxTimeNoAckSent(settings.getMaxTimeNoAckSent());
            connectionSettings.setMaxIdleTime(settings.getMaxIdleTime());
            connectionSettings.setMaxUnconfirmedIPdusReceived(settings.getMaxUnconfirmedIPdusReceived());
            connectionSettings.setMaxNumOfOutstandingIPdus(settings.getMaxNumOfOutstandingIPdus());

            Iec104ServerConnection connection = new Iec104ServerConnection(clientSocket, connectionSettings);
            
            int connectionId = connectionCount.incrementAndGet();
            activeConnections.put(connectionId, connection);
            
            connection.setConnectionCloseListener(() -> {
                activeConnections.remove(connectionId);
                connectionCount.decrementAndGet();
            });

            if (serverListener != null) {
                serverListener.onConnectionAccepted(connection);
            }

        } catch (IOException e) {
            try {
                clientSocket.close();
            } catch (IOException closeException) {
                // Ignore
            }
        }
    }
}
