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

import net.sympower.iec60870.app.common.SampleClientEventListener;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.elements.IeQualifierOfInterrogation;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.iec101.api.Iec101ClientBuilder;
import net.sympower.iec60870.iec101.connection.Iec101ClientConnection;

import java.io.IOException;
import java.util.Scanner;

import static com.fazecast.jSerialComm.SerialPort.getCommPorts;

public class SampleIEC101Client {

    private static final String INTERROGATION_ACTION_KEY = "i";
    private static final String CLOCK_SYNC_ACTION_KEY = "c";
    private static final String SINGLE_COMMAND_SELECT = "s";
    private static final String SINGLE_COMMAND_EXECUTE = "e";
    private static final String SEND_STOPDT = "p";
    private static final String SEND_STARTDT = "t";

    public static void main(String[] args) throws IOException {
        String portName = args.length > 0 ? args[0] : "/dev/ttys009";
        int baudRate = args.length > 1 ? Integer.parseInt(args[1]) : 9600;
        int commonAddress = args.length > 2 ? Integer.parseInt(args[2]) : 1;

        Iec101ClientConnection connection;
        try {
            connection = new Iec101ClientBuilder(portName)
                    .baudRate(baudRate)
                    .dataBits(8)
                    .stopBits(1)
                    .parity(1)
                    .commonAddress(commonAddress)
                    .build();
        } catch (IOException e) {
            System.err.println("Failed to open serial port: " + e.getMessage());
            System.err.println("Make sure the port exists and you have permission to access it.");
            System.err.println("On Linux, you may need to run: sudo chmod 666 " + portName);
            return;
        }

        connection.startDataTransfer(new SampleClientEventListener());

        Scanner scanner = new Scanner(System.in);
        String input;
        
        while (true) {
            printMenu();
            input = scanner.nextLine();
            if (input.equals("q")) {
                break;
            }
            try {
                switch (input.toLowerCase()) {
                    case INTERROGATION_ACTION_KEY:
                        System.out.println("** Sending general interrogation command.");
                        connection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
                        break;
                    case CLOCK_SYNC_ACTION_KEY:
                        System.out.println("** Sending synchronize clocks command.");
                        connection.synchronizeClocks(commonAddress, new IeTime56(System.currentTimeMillis()));
                        break;
                    case SINGLE_COMMAND_SELECT:
                        System.out.println("** Sending single command select.");
                        connection.singleCommand(commonAddress, CauseOfTransmission.ACTIVATION, 5000, new IeSingleCommand(true, 0, true));
                        break;
                    case SINGLE_COMMAND_EXECUTE:
                        System.out.println("** Sending single command execute.");
                        connection.singleCommand(commonAddress, CauseOfTransmission.ACTIVATION, 5000, new IeSingleCommand(true, 0, false));
                        break;
                    case SEND_STOPDT:
                        System.out.println("** Sending STOPDT");
                        connection.stopDataTransfer();
                        break;
                    case SEND_STARTDT:
                        System.out.println("** Sending STARTDT");
                        connection.startDataTransfer(new SampleClientEventListener());
                        break;
                    default:
                        System.out.println("Unknown command: " + input);
                        break;
                }
                
                // Give time for any server responses to be printed before showing menu again
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                System.err.println("Error sending command: " + e.getMessage());
                if (e.getMessage().contains("closed")) {
                    System.err.println("Connection appears to be closed. Exiting.");
                    break;
                }
            }
        }

        connection.close();
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\nAvailable commands:");
        System.out.println("  i - interrogation (C_IC_NA_1)");
        System.out.println("  c - clock synchronization (C_CS_NA_1)");
        System.out.println("  s - single command select (C_SC_NA_1)");
        System.out.println("  e - single command execute (C_SC_NA_1)");
        System.out.println("  p - stop data transfer (STOPDT)");
        System.out.println("  t - start data transfer (STARTDT)");
        System.out.println("  q - quit");
        System.out.print("> ");
    }

}
