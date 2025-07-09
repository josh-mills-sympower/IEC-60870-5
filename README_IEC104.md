# IEC 60870-5-104 Protocol  Basic Information

This library provides a Java implementation of the IEC 60870-5-104 protocol, a standardized communication protocol used in industrial automation and SCADA (Supervisory Control And Data Acquisition) systems for TCP/IP network communication between controlling stations and controlled stations.

## Protocol Overview

IEC 60870-5-104 defines a TCP/IP-based communication protocol that enables reliable data exchange between:
- **Controlling Station** (also known as Client/Master) - initiates communication and sends commands
- **Controlled Station** (also known as Server/Slave) - responds to commands and provides data

This library supports **unbalanced mode only**, meaning the controlling station always initiates communication while the controlled station responds. This library does not support balanced mode.

## Communication Architecture

```
┌─────────────────────┐    TCP/IP Network    ┌─────────────────────┐
│   Controlling       │    (Port 2404)       │   Controlled        │
│   Station           │◄──────────────────────►│   Station           │
│   (Client/Master)   │                       │   (Server/Slave)    │
└─────────────────────┘                       └─────────────────────┘
          │                                           │
          │ ┌─ Initiates TCP connections              │
          │ ├─ Sends STARTDT_ACT to begin data       │
          │ ├─ Manages sequence numbers (k/w params) │
          │ ├─ Sends commands and interrogations     │
          │ └─ Handles flow control and timeouts     │
          │                                           │
          │────── I-Format APDUs (Data) ────────────►│
          │◄───── S-Format APDUs (ACK) ──────────────│
          │◄───── I-Format APDUs (Responses) ────────│
          │                                           │
                                                      │
                            ┌─ Accepts TCP connections
                            ├─ Responds with STARTDT_CON
                            ├─ Processes commands and requests
                            ├─ Sends measurement data
                            ├─ Manages connection lifecycle
                            └─ Handles multiple client connections
```

## Protocol Stack

The IEC-104 protocol operates above the TCP/IP layer.

### Application Protocol Layer (APCI + ASDU)
- APCI (Application Protocol Control Information) manages application-level flow control
- ASDU (Application Service Data Unit) contains the actual industrial automation data
- Implements sequence numbering for additional reliability
- Provides connection lifecycle management

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │    ASDU     │ │ Information │ │     Data Types          │ │
│  │   Format    │ │  Objects    │ │  (Commands, Measurements│ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                Application Protocol Layer                   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │    APCI     │ │  Sequence   │ │     Flow Control        │ │
│  │   Header    │ │  Numbers    │ │    (k/w Parameters)     │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                  Transport Layer (TCP)                     │
│              Reliable Stream Communication                 │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Network Layer (IP)                      │
│                 Packet Routing and Delivery                │
└─────────────────────────────────────────────────────────────┘
```

## APDU Frame Formats

IEC-104 uses three distinct APDU (Application Protocol Data Unit) formats for different communication purposes:

### 1. I-Format (Information Transfer)
Used for transmitting application data (ASDUs) between stations.

```
┌────┬────┬──────────────────┬──────────────────┬─────────────┐
│68h │ L  │ Control Field 1  │ Control Field 2  │    ASDU     │
│    │    │ Control Field 3  │ Control Field 4  │             │
└────┴────┴──────────────────┴──────────────────┴─────────────┘
  │   │         │                    │               │
  │   │         │                    │               └── Application data
  │   │         │                    └─────────────────── Receive sequence number (15 bits)
  │   │         └───────────────────────────────────── Send sequence number (15 bits)
  │   └──────────────────────────────────────────── Length (4-253 octets)
  └────────────────────────────────────────────── Start byte (fixed)

Send Sequence Number: 15-bit counter (0-32767) for sent I-format frames
Receive Sequence Number: 15-bit counter for acknowledging received I-format frames
Always contains ASDU with application data
```

### 2. S-Format (Supervisory Functions)
Used for acknowledging received I-format frames without sending data.

```
┌────┬────┬────┬────┬──────────────────┬──────────────────┐
│68h │ 04h│ 01h│ 00h│ Control Field 3  │ Control Field 4  │
└────┴────┴────┴────┴──────────────────┴──────────────────┘
  │   │   │   │             │
  │   │   │   │             └─────────────────── Receive sequence number (15 bits)
  │   │   │   └───────────────────────────────── Reserved (0x00)
  │   │   └─────────────────────────────────── S-format identifier (0x01)
  │   └───────────────────────────────────── Fixed length (0x04)
  └─────────────────────────────────────── Start byte (fixed)

Fixed length: 6 octets total
Used only for acknowledgment of received I-format frames
No ASDU data included
```

### 3. U-Format (Unnumbered Control Functions)
Used for connection control and test functions.

```
┌────┬────┬────────────┬────┬────┬────┐
│68h │ 04h│ Command    │ 00h│ 00h│ 00h│
└────┴────┴────────────┴────┴────┴────┘
  │   │         │       │   │   │
  │   │         │       │   │   └── Reserved (0x00)
  │   │         │       │   └────── Reserved (0x00)
  │   │         │       └────────── Reserved (0x00)
  │   │         └─────────────────── Function code
  │   └─────────────────────────── Fixed length (0x04)
  └───────────────────────────── Start byte (fixed)

Function Codes:
- STARTDT_ACT (0x07): Start data transfer activation
- STARTDT_CON (0x0B): Start data transfer confirmation
- STOPDT_ACT (0x13): Stop data transfer activation
- STOPDT_CON (0x23): Stop data transfer confirmation
- TESTFR_ACT (0x43): Test frame activation
- TESTFR_CON (0x83): Test frame confirmation
```

## Connection Lifecycle

### Connection Establishment
Before application data can be exchanged, a proper connection sequence must be completed:

```
Controlling Station                    Controlled Station
       │                                      │
       │ ══════ TCP Connection ══════════════►│
       │                                      │
       │ ──── STARTDT_ACT (U-Format) ───────►│
       │                                      │
       │ ◄─── STARTDT_CON (U-Format) ────────│
       │                                      │
     Ready for I-Format data exchange
```

### Data Transfer Phase
Once connection is established, I-format and S-format frames handle data and acknowledgments:

```
Controlling Station                    Controlled Station
       │                                      │
       │ ──── I-Format (Send Seq=0) ────────►│
       │      [C_IC_NA_1 Interrogation]      │
       │                                      │
       │ ◄─── S-Format (Recv Seq=1) ─────────│
       │      [Acknowledge frame 0]          │
       │                                      │
       │ ◄─── I-Format (Send Seq=0) ─────────│
       │      [M_ME_NB_1 Measurement]        │
       │                                      │
       │ ──── S-Format (Recv Seq=1) ────────►│
       │      [Acknowledge frame 0]          │
       │                                      │
```

### Connection Termination
Graceful connection shutdown using STOPDT sequence:

```
Controlling Station                    Controlled Station
       │                                      │
       │ ──── STOPDT_ACT (U-Format) ────────►│
       │                                      │
       │ ◄─── STOPDT_CON (U-Format) ─────────│
       │                                      │
       │ ══════ TCP Close ══════════════════►│
       │                                      │
```

## Flow Control Mechanism

IEC-104 implements flow control using two key parameters:

### k Parameter (Maximum Outstanding I-PDUs)
Controls how many I-format frames can be sent without receiving acknowledgment:

```
Default: k = 12

Flow Control Logic:
┌─────────────────┬──────────────────┬────────────────────┐
│ Outstanding     │ Action           │ Result             │
│ I-PDUs Sent     │                  │                    │
├─────────────────┼──────────────────┼────────────────────┤
│ 0 - 11          │ Send I-format    │ Normal operation   │
│ 12 (k limit)    │ Stop sending     │ Wait for S-format  │
│ > 12            │ Error condition  │ Close connection   │
└─────────────────┴──────────────────┴────────────────────┘
```

### w Parameter (Maximum Unconfirmed I-PDUs)
Controls when S-format acknowledgments must be sent:

```
Default: w = 8

Acknowledgment Logic:
┌─────────────────┬──────────────────┬────────────────────┐
│ Unconfirmed     │ Action           │ Result             │
│ I-PDUs Received │                  │                    │
├─────────────────┼──────────────────┼────────────────────┤
│ 0 - 7           │ Optional S-format│ Normal operation   │
│ 8 (w limit)     │ Send S-format    │ Acknowledge frames │
│ > 8             │ Error condition  │ Close connection   │
└─────────────────┴──────────────────┴────────────────────┘
```

## Timeout Management

The protocol includes three timeouts for maintaining connection reliability:

### T1 - Acknowledgment Timeout (Default: 15 seconds)
Maximum time to wait for acknowledgment of sent I-format frames:

```
Timer T1 Management:
┌─────────────────┬────────────────────────────────────────┐
│ Trigger Event   │ I-format frame sent                    │
│ Reset Event     │ S-format or I-format acknowledgment    │
│ Timeout Action  │ Close connection immediately           │
│ Retry Logic     │ None - connection terminates           │
└─────────────────┴────────────────────────────────────────┘
```

### T2 - Supervisory Frame Timeout (Default: 10 seconds)
Maximum time to defer sending S-format acknowledgment:

```
Timer T2 Management:
┌─────────────────┬────────────────────────────────────────┐
│ Trigger Event   │ I-format frame received                │
│ Reset Event     │ S-format or I-format sent              │
│ Timeout Action  │ Send S-format acknowledgment           │
│ Purpose         │ Ensure timely acknowledgments          │
└─────────────────┴────────────────────────────────────────┘
```

### T3 - Test Frame Timeout (Default: 20 seconds)
Maximum time of inactivity before sending test frame:

```
Timer T3 Management:
┌─────────────────┬────────────────────────────────────────┐
│ Trigger Event   │ No frame activity in either direction  │
│ Reset Event     │ Any frame sent or received             │
│ Timeout Action  │ Send TESTFR_ACT frame                  │
│ Purpose         │ Verify connection integrity            │
└─────────────────┴────────────────────────────────────────┘

Test Frame Sequence:
Station A ──── TESTFR_ACT ────► Station B
Station A ◄─── TESTFR_CON ───── Station B
```

## Sequence Number Management

IEC-104 uses 15-bit sequence numbers for I-format frames to ensure reliable delivery:

### Send Sequence Number (VS)
Tracks I-format frames sent by this station:

```
Sequence Number Lifecycle:
┌─────────────────┬────────────────────────────────────────┐
│ Initial Value   │ 0                                      │
│ Increment       │ +1 for each I-format frame sent       │
│ Range           │ 0 to 32767 (15 bits)                  │
│ Wraparound      │ 32767 → 0                             │
│ Purpose         │ Unique identification of sent frames   │
└─────────────────┴────────────────────────────────────────┘
```

### Receive Sequence Number (VR)
Tracks I-format frames received and acknowledged:

```
Acknowledgment Process:
┌─────────────────┬────────────────────────────────────────┐
│ Frame Reception │ I-format frame received                │
│ Validation      │ Check if sequence number is expected   │
│ Update VR       │ VR = received sequence number + 1     │
│ Send ACK        │ Include VR in next S-format or I-format│
│ Duplicate Check │ Discard frames with old sequence numbers│
└─────────────────┴────────────────────────────────────────┘
```


### Data Types and Commands

The application layer supports various standardized data types:

#### Monitoring Commands:
- **C_IC_NA_1**: General Interrogation Command
- **C_CI_NA_1**: Counter Interrogation Command
- **C_RD_NA_1**: Read Command
- **C_CS_NA_1**: Clock Synchronization Command
- **C_TS_NA_1**: Test Command

#### Control Commands:
- **C_SC_NA_1**: Single Command
- **C_DC_NA_1**: Double Command
- **C_RC_NA_1**: Regulating Step Command
- **C_SE_NA_1**: Set Point Command (Normalized Value)
- **C_SE_NB_1**: Set Point Command (Scaled Value)
- **C_SE_NC_1**: Set Point Command (Short Float)

#### Monitoring Data:
- **M_SP_NA_1**: Single Point Information
- **M_DP_NA_1**: Double Point Information
- **M_ME_NA_1**: Measured Value (Normalized)
- **M_ME_NB_1**: Measured Value (Scaled)
- **M_ME_NC_1**: Measured Value (Short Float)
- **M_IT_NA_1**: Integrated Totals

### Cause of Transmission

Indicates why the ASDU was transmitted:

- **ACTIVATION**: Command activation
- **ACTIVATION_CON**: Activation confirmation
- **ACTIVATION_TERM**: Activation termination
- **SPONTANEOUS**: Spontaneous data transmission
- **REQUEST**: Requested data
- **BACKGROUND_SCAN**: Background scan

### Time Tagging

Information can be tagged with timestamps for event correlation:

- **CP24Time2a**: 3-byte time (minutes + milliseconds)
- **CP56Time2a**: 7-byte time (complete date/time with subsecond precision)

### Quality Information

Data values include quality indicators:

- **Invalid/Valid**: Data validity flag
- **Not Topical/Topical**: Data freshness indicator
- **Substituted/Original**: Source of data
- **Blocked/Not Blocked**: Value update status
- **Overflow**: Value range exceeded

## Implementation Limitations

This implementation has several limitations compared to the full IEC 60870-5-104 specification:

### Protocol Limitations
- **Unbalanced mode only**: Only client-initiated communication is supported
- **Limited ASDU types**: Not all ASDU data types defined in the standard are implemented - only the most commonly used types for basic SCADA operations. File transfer operations (ASDU types 120-126) are not supported
- **No redundancy**: No support for redundant communication paths or server redundancy
