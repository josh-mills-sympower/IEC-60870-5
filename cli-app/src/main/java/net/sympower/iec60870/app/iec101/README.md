# IEC 60870-5-101 Sample Applications

This directory contains sample client and server applications for IEC 60870-5-101 (Serial) communication.

## SampleIEC101Server

A demonstration IEC 60870-5-101 server that simulates an RTU (Remote Terminal Unit) communicating over a serial interface.

### Running the Server

```bash
cd run-scripts
./iec101-console-server [port] [baudrate]
```

### Command Line Arguments

- **port** (optional): Serial port device path (default: `/dev/ttyUSB0`)
- **baudrate** (optional): Communication speed in bps (default: `9600`)

### Server Configuration

The server starts with the following default settings:
- **Serial Port**: `/dev/ttyUSB0` (Linux) or specify COM port on Windows
- **Baud Rate**: 9600 bps
- **Data Bits**: 8
- **Stop Bits**: 1  
- **Parity**: Even parity
- **Max Idle Time**: 30 seconds

The server will:
1. Open the specified serial port
2. Listen for incoming IEC 60870-5-101 frames
3. Respond to interrogation commands with simulated measurements
4. Acknowledge control commands and simulate their execution
5. Send spontaneous data updates when values change

To stop the server, press Enter in the console where it's running.

## SampleIEC101Client

A demonstration IEC 60870-5-101 client that simulates a control center communicating over a serial interface. The client connects to an IEC server via serial communication and demonstrates the same protocol operations as the TCP client.

### Running the Client

```bash
cd run-scripts
./iec101-console-client [port] [baudrate]
```

### Command Line Arguments

- **port** (optional): Serial port device path (default: `/dev/ttyUSB1`)
- **baudrate** (optional): Communication speed in bps (default: `9600`)

### Client Usage

The client will:
1. Open the specified serial port
2. Perform IEC 60870-5-101 link layer initialization
3. Send an interrogation command to request all current data
4. Display received measurement responses
5. Send various control commands to demonstrate different ASDU types
6. Show command confirmations from the server

## Serial Port Setup

### Using socat for Virtual Serial Ports

For testing you can create virtual serial port pairs using `socat`:

#### Installation

**Ubuntu/Debian:**
```bash
sudo apt-get install socat
```

**macOS:**
```bash
brew install socat
```

#### Creating Virtual Serial Port Pairs

1. **Create a bidirectional pipe between two virtual serial ports:**
   ```bash
   socat -d -d pty,raw,echo=0 pty,raw,echo=0
   ```

   This command will output something like:
   ```
   2023/01/01 10:00:00 socat[12345] N PTY is /dev/pts/1
   2023/01/01 10:00:00 socat[12345] N PTY is /dev/pts/2
   ```

2. **Use the created virtual ports in your applications:**
   ```bash
   # Terminal 1: Start server on first virtual port
   ./iec101-console-server /dev/pts/1 9600
   
   # Terminal 2: Start client on second virtual port  
   ./iec101-console-client /dev/pts/2 9600
   ```
