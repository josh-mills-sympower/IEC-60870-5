/*
 * This file is part of the enhanced IEC 60870 library.
 * Original project: https://github.com/openmuc/j60870
 * Enhanced version: https://github.com/josh-mills-sympower/IEC-60870-5
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package net.sympower.iec60870.app.iec104;

import java.io.IOException;

import net.sympower.iec60870.app.common.SampleServerListener;
import net.sympower.iec60870.iec104.api.Iec104Server;
import net.sympower.iec60870.iec104.api.Iec104ServerBuilder;

public class SampleIEC104Server {

    public static void main(String[] args) throws IOException {
        Iec104Server server = new Iec104ServerBuilder(2404)
                .maxConnections(10)
                .maxIdleTime(30000)
                .build();

        server.start(new SampleServerListener());

        System.out.println("Server started on port " + server.getPort());
        System.out.println("Press Enter to stop...");
        System.in.read();

        server.stop();
    }

}
