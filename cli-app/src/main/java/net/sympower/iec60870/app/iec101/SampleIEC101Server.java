/*
 * This file is part of the enhanced IEC 60870 library.
 * Original project: https://github.com/openmuc/j60870
 * Enhanced version: https://github.com/josh-mills-sympower/IEC-60870-5
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package net.sympower.iec60870.app.iec101;

import java.io.IOException;

import net.sympower.iec60870.app.common.SampleServerListener;
import net.sympower.iec60870.iec101.api.Iec101Server;
import net.sympower.iec60870.iec101.api.Iec101ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SampleIEC101Server {

    private static final Logger logger = LoggerFactory.getLogger(SampleIEC101Server.class);

    public static void main(String[] args) throws IOException {
        String portName = args.length > 0 ? args[0] : "/dev/ttys006";
        int baudRate = args.length > 1 ? Integer.parseInt(args[1]) : 9600;

        Iec101Server server = new Iec101ServerBuilder(portName)
                .baudRate(baudRate)
                .dataBits(8)
                .stopBits(1)
                .parity(1) // TODO check this is sensible
                .build();

        server.start(new SampleServerListener());

        logger.info("Serial server started on port {}", portName);
        System.out.println("Press Enter to stop...");
        System.in.read();

        server.stop();
    }

}
