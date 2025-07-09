# IEC 60870-5 Library for Java

This is a **limited Java implementation** of the IEC 60870-5 protocol standard, designed for telecontrol applications in industrial automation and SCADA systems. This library provides client and server capabilities for both TCP/IP (IEC 60870-5-104) and serial communication (IEC 60870-5-101) variants of the protocol.

This library is a fork of the j60870 library by OpenMUC, see [Attribution](ATTRIBUTION.md) for details.

## Important Notice

**This is a limited implementation of the IEC 60870-5 protocol.** It supports only the most commonly used features and ASDU types required for basic SCADA operations. Full protocol complience may be support in the future, it may not.

See the [Implementation Status](#implementation-status) section below for detailed information about supported features and limitations.

## About the IEC 60870-5 Protocol

The IEC 60870-5 series is a standard for telecontrol and telecommunications for electric power systems. It defines a communication protocol used in power system automation, allowing control centers to monitor and control remote substations and devices.

The protocol operates on a master-slave architecture where a control center (master/client) communicates with remote terminal units or intelligent electronic devices (slaves/servers). The communication involves structured data units called Application Service Data Units (ASDUs) that carry various types of information including measurements, commands, and status updates.

## Library Architecture

The library handles the low-level protocol details, allowing you to focus on application logic. Once the client and server connection is initialized, the general flow is:

### Incoming Data Flow
```
Raw Bytes → Frame/APDU Decoding → ASDU Extraction → User Defined Callback
```

1. **Byte Reception**: The library receives raw bytes from TCP socket (IEC-104) or serial port (IEC-101)
2. **Frame Decoding**: Bytes are decoded into protocol frames (APDUs for IEC-104, Fixed/Variable frames for IEC-101)
3. **ASDU Extraction**: Application Service Data Units are extracted from the frames
4. **User Define Callback**: Your `IEC60870EventListener.onAsduReceived()` method is called with the decoded ASDU

### Outgoing Data Flow
```
User Define Callback → Frame/APDU/ASDU Encoding → Raw Bytes
```

1. **User Define Callback**: You create response ASDUs using the library's API
2. **Frame/APDU Encoding**: The library encodes ASDUs into appropriate protocol frames/APDU-s
3. **Byte Transmission**: Encoded bytes are sent over the transport layer


The library handles all protocol-specific details including:
- Frame structure and checksums
- Flow control and acknowledgments
- Timeout management
- Connection lifecycle

## Gradle Dependency

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/josh-mills-sympower/IEC-60870-5")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation 'net.sympower:iec60870:0.1'
}
```

## Usage Examples

### IEC 60870-5-104 (TCP/IP) Server

```java
import net.sympower.iec60870.common.api.*;
import net.sympower.iec60870.common.*;
import net.sympower.iec60870.common.elements.*;
import net.sympower.iec60870.iec104.api.*;

public class IEC104Server {
    public static void main(String[] args) throws Exception {
        // Create server listening on standard IEC 60870-5-104 port
        IEC60870Server server = new Iec104ServerBuilder(2404)  // Standard port
            .maxConnections(5)                                  // Allow 5 concurrent connections
            .connectionTimeout(30000)                           // 30 second timeout
            .build();
        
        // Start server with connection handler
        server.start(new IEC60870ServerListener() {
            @Override
            public void onConnectionAccepted(IEC60870Connection connection) {
                try {
                    connection.startDataTransfer(new ServerEventHandler(connection));
                    System.out.println("Client connected: " + connection);
                } catch (Exception e) {
                    System.err.println("Failed to start data transfer: " + e.getMessage());
                }
            }
        });
        
        System.out.println("IEC 60870-5-104 server started on port 2404");
    }
}

class ServerEventHandler implements IEC60870EventListener {
    private final IEC60870Connection connection;
    
    public ServerEventHandler(IEC60870Connection connection) {
        this.connection = connection;
    }
    
    @Override
    public void onConnectionReady() {
        System.out.println("Connection ready for data transfer");
    }
    
    @Override
    public void onAsduReceived(ASdu asdu) {
        try {
            System.out.println("Received: " + asdu.getTypeIdentification());
            
            switch (asdu.getTypeIdentification()) {
                case C_IC_NA_1: // Interrogation command
                    connection.sendConfirmation(asdu);
                    sendMeasurementData();
                    break;
                case C_SC_NA_1: // Single command
                    connection.sendConfirmation(asdu);
                    System.out.println("Executing single command");
                    break;
                // Handle other command types...
            }
        } catch (Exception e) {
            System.err.println("Error processing ASDU: " + e.getMessage());
        }
    }
    
    @Override
    public void onConnectionLost(Exception cause) {
        System.out.println("Connection lost: " + 
                         (cause != null ? cause.getMessage() : "Unknown"));
    }
    
    private void sendMeasurementData() throws Exception {
        // Create measurement ASDU
        ASdu measurementAsdu = new ASdu(
            ASduType.M_ME_NB_1,  // Measured value, scaled value
            false,               // Not sequence of elements
            CauseOfTransmission.SPONTANEOUS,
            false, false, 0, 1,  // Test=false, Negative=false, OA=0, CA=1
            new InformationObject[] {
                new InformationObject(100, new InformationElement[][] {
                    { new IeScaledValue(1250), new IeQuality(false, false, false, false, false) }
                })
            }
        );
        connection.send(measurementAsdu);
    }
}
```

### IEC 60870-5-104 (TCP/IP) Client

```java
import net.sympower.iec60870.common.api.*;
import net.sympower.iec60870.common.*;
import net.sympower.iec60870.common.elements.*;
import net.sympower.iec60870.iec104.connection.*;

public class IEC104Client {
    public static void main(String[] args) throws Exception {
        // Connect to IEC 60870-5-104 server
        Iec104ClientConnection client = new Iec104ClientBuilder("192.168.1.100", 2404)  // Server IP and port
            .connectionTimeout(5000)          // 5 second connection timeout
            .maxTimeNoAckReceived(15000)      // T1: 15 seconds
            .maxTimeNoAckSent(10000)          // T2: 10 seconds
            .maxIdleTime(20000)               // T3: 20 seconds
            .maxNumOfOutstandingIPdus(12)     // k parameter
            .maxUnconfirmedIPdusReceived(8)   // w parameter
            .build();
        
        // Start data transfer with event handler
        client.startDataTransfer(new ClientEventHandler());
        
        // Send general interrogation to get all current values
        client.interrogation(1, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20)); // Station interrogation
        
        // Send a single command
        client.singleCommand(1, CauseOfTransmission.ACTIVATION, 1001, new IeSingleCommand(true, 0, false)); // Execute command
        
        // Close connection
        client.close();
    }
}

class ClientEventHandler implements IEC60870EventListener {
    @Override
    public void onConnectionReady() {
        System.out.println("Client connected and ready");
    }
    
    @Override
    public void onAsduReceived(ASdu asdu) {
        System.out.println("Received ASDU: " + asdu.getTypeIdentification());
        
        // Process received data
        for (InformationObject infoObj : asdu.getInformationObjects()) {
            System.out.println("  IOA: " + infoObj.getInformationObjectAddress());
            // Process information elements...
        }
    }
    
    @Override
    public void onConnectionLost(Exception cause) {
        System.err.println("Connection lost: " + 
                          (cause != null ? cause.getMessage() : "Unknown"));
    }
}
```

### IEC 60870-5-101 (Serial) Client

```java
import net.sympower.iec60870.common.api.*;
import net.sympower.iec60870.common.*;
import net.sympower.iec60870.common.elements.*;
import net.sympower.iec60870.iec101.connection.*;
import com.fazecast.jSerialComm.SerialPort;

public class IEC101Client {
    public static void main(String[] args) throws Exception {
        // Connect to IEC 60870-5-101 server via serial port
        Iec101ClientConnection client = new Iec101ClientBuilder("/dev/ttyUSB0")  // Serial port
            .baudRate(9600)                   // Baud rate
            .parity(SerialPort.EVEN_PARITY)   // Even parity
            .dataBits(8)                      // 8 data bits
            .stopBits(1)                      // 1 stop bit
            .linkAddress(1)                   // Remote station address
            .initializationTimeoutMs(5000)    // Initialization timeout
            .build();
        
        // Start data transfer
        client.startDataTransfer(new ClientEventHandler());
        
        // Send commands
        client.interrogation(1, CauseOfTransmission.ACTIVATION,
                           new IeQualifierOfInterrogation(20));
        
        client.close();
    }
}
```

### IEC 60870-5-101 (Serial) Server

```java
import net.sympower.iec60870.common.api.*;
import net.sympower.iec60870.common.*;
import net.sympower.iec60870.iec101.api.*;
import com.fazecast.jSerialComm.SerialPort;

public class IEC101Server {
    public static void main(String[] args) throws Exception {
        // Create serial server
        IEC60870Server server = new Iec101ServerBuilder("/dev/ttyUSB0")  // Serial port
            .baudRate(9600)                   // Baud rate
            .parity(SerialPort.EVEN_PARITY)   // Even parity
            .linkAddress(1)                   // Local station address
            .build();
        
        // Start server
        server.start(new IEC60870ServerListener() {
            @Override
            public void onConnectionAccepted(IEC60870Connection connection) {
                try {
                    connection.startDataTransfer(new ServerEventHandler(connection));
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        });
    }
}
```

## Supported ASDU Types

The library supports a **limited subset** of ASDU types defined in the IEC 60870-5 specification. Only the most commonly used types for basic SCADA operations are implemented:

### Monitoring (Process Information)
- `M_SP_NA_1` - Single-point information
- `M_DP_NA_1` - Double-point information  
- `M_ME_NA_1` - Measured value, normalized value
- `M_ME_NB_1` - Measured value, scaled value
- `M_ME_NC_1` - Measured value, short floating point number
- `M_IT_NA_1` - Integrated totals

### Control (Command Information)
- `C_SC_NA_1` - Single command
- `C_DC_NA_1` - Double command
- `C_RC_NA_1` - Regulating step command
- `C_SE_NA_1` - Set point command, normalized value
- `C_SE_NB_1` - Set point command, scaled value
- `C_SE_NC_1` - Set point command, short floating point

### System Information
- `C_IC_NA_1` - Interrogation command
- `C_CI_NA_1` - Counter interrogation command
- `C_RD_NA_1` - Read command
- `C_CS_NA_1` - Clock synchronization command
- `C_TS_NA_1` - Test command

**Note**: Many other ASDU types defined in the IEC 60870-5 standard are not implemented, including file transfer operations, advanced parameter commands, and specialized protection equipment data types.

## Configuration Examples

### IEC 104 Flow Control Parameters

```java
Iec104ClientConnection connection = new Iec104ClientBuilder("192.168.1.100", 2404)
    .maxTimeNoAckReceived(15000)      // T1: Max time to wait for ACK (15s)
    .maxTimeNoAckSent(10000)          // T2: Max time to defer sending ACK (10s)
    .maxIdleTime(20000)               // T3: Test frame interval when idle (20s)
    .maxNumOfOutstandingIPdus(12)     // k: Max unacknowledged sent I-PDUs
    .maxUnconfirmedIPdusReceived(8)   // w: Max unconfirmed received I-PDUs
    .build();
```

### IEC 101 Serial Configuration

```java
Iec101ClientConnection connection = new Iec101ClientBuilder("/dev/ttyUSB0")  // Serial port device
    .baudRate(9600)                   // 1200, 2400, 4800, 9600, 19200, 38400
    .dataBits(8)                      // 7 or 8 data bits
    .parity(SerialPort.EVEN_PARITY)   // NO_PARITY, EVEN_PARITY, ODD_PARITY
    .stopBits(1)                      // 1 or 2 stop bits
    .linkAddress(1)                   // Station address (1-254)
    .initializationTimeoutMs(5000)    // Initialization timeout (ms)
    .build();
```

## Sample Applications

The library includes sample applications demonstrating both client and server usage:

```bash
# IEC 104 samples
./run-scripts/iec104-console-client
./run-scripts/iec104-console-server

# IEC 101 samples  
./run-scripts/iec101-console-client
./run-scripts/iec101-console-server
```

## Implementation Status

### Limitations

#### Protocol Compliance
- **Limited ASDU coverage**: Only standard ASDU types are implemented. ASDU types 120-126 (file and directory related) are not supported.

#### IEC 60870-5-101 (Serial) Limitations
- **Unbalanced mode only**: Balanced mode not supported
- **Point-to-point only**: One client, one server. No multi-drop configurations
- **Basic link layer**: Access Demand (ACD) bit is hard-coded
- **Limited flow control**: Data Flow Control (DFC) bit is hard-coded

#### IEC 60870-5-104 (TCP/IP) Limitations
- **Unbalanced mode only**: Balanced mode not supported
- **Basic flow control**: Standard k/w parameters
- **No redundancy**: No support for redundant communication paths
- **Limited test frames**: Basic test frame handling only

See protocol-specific documentation ([IEC 104](README_IEC104.md) | [IEC 101](README_IEC101.md)) for detailed limitations and supported features.

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](license/gpl-3.0.txt) file for details.
