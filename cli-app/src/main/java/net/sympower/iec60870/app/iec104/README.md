# IEC 60870-5-104 Sample Applications

This directory contains sample client and server applications for IEC 60870-5-104 (TCP/IP) communication.

## SampleIEC104Server

A demonstration IEC 60870-5-104 server that simulates an RTU (Remote Terminal Unit) or controlled station. The server accepts connections from control centers and responds to standard IEC commands with simulated measurement data.

### Running the Server

#### Using Gradle:
```bash
./gradlew :cli-app:run -PmainClass=net.sympower.iec60870.app.iec104.SampleIEC104Server
```

#### Using the Run Script:
```bash
cd run-scripts
./iec104-console-server
```

### Server Configuration

The server starts with the following default settings:
- **Port**: 2404 (standard IEC 60870-5-104 port)
- **Max Connections**: 10 concurrent client connections
- **Max Idle Time**: 30 seconds before sending test frames

The server will:
1. Listen for incoming connections on port 2404
2. Accept connections from IEC 60870-5-104 clients
3. Respond to interrogation commands with simulated measurements
4. Acknowledge control commands and simulate their execution
5. Send spontaneous data updates periodically

To stop the server, press Enter in the console where it's running.

## SampleIEC104Client

A demonstration IEC 60870-5-104 client that simulates a control center or controlling station. The client connects to an IEC server and demonstrates sending various commands and receiving measurement data.

### Running the Client

#### Using Gradle:
```bash
./gradlew :cli-app:run -PmainClass=net.sympower.iec60870.app.iec104.SampleIEC104Client
```

#### Using the Run Script:
```bash
cd run-scripts
./iec104-console-client
```
