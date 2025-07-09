/*
 * This file is part of the enhanced IEC 60870 library.
 * Original project: https://github.com/openmuc/j60870
 * Enhanced version: https://github.com/josh-mills-sympower/IEC-60870-5
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package net.sympower.iec60870.app.common;

import java.io.IOException;

import net.sympower.iec60870.common.api.IEC60870Connection;
import net.sympower.iec60870.common.api.IEC60870ServerListener;

public class SampleServerListener implements IEC60870ServerListener {

    @Override
    public void onConnectionAccepted(IEC60870Connection connection) {
        System.out.println("New client connected");

        try {
            // Start data transfer with simplified event handling
            connection.startDataTransfer(new SampleServerEventListener(connection));
        } catch (IOException e) {
            System.err.println("Failed to start data transfer: " + e.getMessage());
            connection.close();
        }
    }
}
