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

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.api.IEC60870EventListener;

import java.io.IOException;


public class SampleClientEventListener implements IEC60870EventListener {

    @Override
    public void onConnectionReady() {
        System.out.println("Data transfer started successfully");
    }

    @Override
    public void onAsduReceived(ASdu asdu) {
        System.out.println("Received ASDU: " + asdu.toString());
    }

    @Override
    public void onConnectionLost(IOException cause) {
        if (cause != null) {
            System.err.println("Connection lost: " + cause.getMessage());
        } else {
            System.out.println("Connection closed");
        }
    }
}
