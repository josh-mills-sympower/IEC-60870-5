# IEC 60870-5-101 Protocol Basic Information

This library provides a Java implementation of the IEC 60870-5-101 protocol, a standardized communication protocol used in industrial automation and SCADA (Supervisory Control And Data Acquisition) systems for serial communication between controlling stations and controlled stations.

## Protocol Overview

IEC 60870-5-101 defines a serial communication protocol that enables reliable data exchange between:
- **Controlling Station** (also known as Client/Master) - initiates communication and sends commands
- **Controlled Station** (also known as Server/Slave) - responds to commands and provides data

This library supports **unbalanced mode only**, meaning the controlling station always initiates communication while the controlled station responds. This library does not support balanced mode.

## Communication Architecture

```
┌─────────────────────┐    Serial Link    ┌─────────────────────┐
│   Controlling       │    (RS-232/485)   │   Controlled        │
│   Station           │◄──────────────────►│   Station           │
│   (Client/Master)   │                   │   (Server/Slave)    │
└─────────────────────┘                   └─────────────────────┘
          │                                         │
          │ ┌─ Initiates all communication          │
          │ ├─ Sends commands and requests          │
          │ └─Manages Frame Count Bit (FCB)         │
          │                                         │
          │                                         │
          └─ Application Data ────────────────────► │
          ◄──────────────── Responses & Data ──────┘
                            ◄──── ACK/NACK ─────────┘
                                                    │
                            ┌─ Responds to commands │
                            ├─ Provides measurement data
                            ├─ Handles duplicate detection
                            ├─ Sends spontaneous data
                            └─ Sends ACK/NACK for received frames
```

## Protocol Stack

The IEC-101 protocol consists of two main layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │    ASDU     │ │ Information │ │     Data Types          │ │
│  │   Format    │ │  Objects    │ │  (Commands, Measurements│ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                      Link Layer                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │ Frame Types │ │    FCB      │ │   Error Detection       │ │
│  │   (FT1.2)   │ │ Management  │ │   & Correction          │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                   Physical Layer                            │
│              Serial Communication (RS-232/485)             │
└─────────────────────────────────────────────────────────────┘
```

## Link Layer (Layer 2)

The Link Layer implements the FT1.2 frame format and manages frame transmission, error detection, and duplicate prevention.

### Frame Types

IEC-101 uses three distinct frame types based on the FT1.2 standard:

#### 1. Variable Length Frame
Used for transmitting application data (ASDUs) between stations.

```
┌────┬───┬───┬────┬───┬───┬────────┬────┬────┐
│ 68h│ L │ L │68h │ C │ A │  ASDU  │ CS │16h │
└────┴───┴───┴────┴───┴───┴────────┴────┴────┘
  │   │   │   │    │   │      │      │    │
  │   │   │   │    │   │      │      │    └── End character
  │   │   │   │    │   │      │      └─────── Checksum
  │   │   │   │    │   │      └────────────── Application data
  │   │   │   │    │   └───────────────────── Address field
  │   │   │   │    └─────────────────────── Control field
  │   │   │   └───────────────────────────── Start character (repeated)
  │   │   └─────────────────────────────── Length field (repeated for error detection)
  │   └───────────────────────────────── Length field
  └─────────────────────────────────── Start character

Length (L): Number of octets between second start and checksum (inclusive)
Range: 5-255 octets (minimum includes C + A + CS + END + minimal ASDU)
```

#### 2. Fixed Length Frame
Used for control functions and acknowledgments without application data.

```
┌────┬───┬───┬────┬────┐
│10h │ C │ A │ CS │16h │
└────┴───┴───┴────┴────┘
  │   │   │   │    │
  │   │   │   │    └── End character
  │   │   │   └─────── Checksum
  │   │   └───────────── Address field
  │   └─────────────── Control field
  └─────────────────── Start character

Total length: Always 5 octets
```

#### 3. Single Character Frame
Used for simple acknowledgments and flow control.

```
┌────┐
│ E5h│  ←── ACK (Positive acknowledgment)
└────┘

┌────┐
│A2h │  ←── NACK (Negative acknowledgment)  
└────┘

Single octet frames for immediate response
```

### Control Field Structure

The control field (C) contains information for frame processing and is interpreted differently based on the station role:

```
Control Field Bit Layout:
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ 7 │ 6 │ 5 │ 4 │ 3 │ 2 │ 1 │ 0 │
└───┴───┴───┴───┴───┴───┴───┴───┘
  │   │   │   │   └───┴───┴───┴─── Function Code (4 bits)
  │   │   │   └─────────────────── (Function dependent)
  │   │   └─────────────────────── (Function dependent)  
  │   └─────────────────────────── (Function dependent)
  └─────────────────────────────── RES (Reserved, always 0)
```

#### For Controlling Station (Primary) Frames:
```
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ 0 │PRM│FCB│FCV│ Function Code │
└───┴───┴───┴───┴───┴───┴───┴───┘
      │   │   │
      │   │   └─── FCV: Frame Count Valid
      │   └─────── FCB: Frame Count Bit (alternates 0/1)
      └─────────── PRM: 1 (Primary station)

PRM = 1: Indicates controlling station transmission
FCB: Alternates between 0 and 1 for each new transmission
FCV: Set to 1 when FCB is meaningful, 0 when not
```

#### For Controlled Station (Secondary) Frames:
```
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ 0 │PRM│ACD│DFC│ Function Code │
└───┴───┴───┴───┴───┴───┴───┴───┘
      │   │   │
      │   │   └─── DFC: Data Flow Control (buffer full indication)
      │   └─────── ACD: Access Demand (has data to transmit)
      └─────────── PRM: 0 (Secondary station)

PRM = 0: Indicates controlled station transmission  
ACD: Set to 1 when controlled station has data to send
DFC: Set to 1 when controlled station buffer is full
```

### Function Codes

The function code (bits 0-3) defines the purpose of the frame:

#### Controlling Station Function Codes:
- **0**: Reset Remote Link - Initialize communication
- **3**: Send/Confirm User Data - Confirmed data transfer
- **4**: Send/No Reply User Data - Unconfirmed data transfer  
- **9**: Request Link Status - Check link status
- **10**: Request User Data Class 1 - Request priority data
- **11**: Request User Data Class 2 - Request normal data

#### Controlled Station Function Codes:
- **0**: ACK (Positive Acknowledgment)
- **1**: NACK (Negative Acknowledgment)
- **8**: User Data No Reply - Send data without confirmation
- **9**: NACK (Data not available)
- **11**: Status of Link or Access Demand - Link status response

### Frame Count Bit (FCB) Management

The FCB mechanism prevents duplicate frame processing:

```
Transmission Sequence:
┌─────────────────┬─────┬─────┬──────────────────────┐
│   Frame Type    │ FCB │ FCV │      Purpose         │
├─────────────────┼─────┼─────┼──────────────────────┤
│ First Command   │  0  │  1  │ Initial transmission │
│ Retry Same      │  0  │  1  │ Retransmit if no ACK │
│ Second Command  │  1  │  1  │ New transmission     │
│ Third Command   │  0  │  1  │ FCB alternates       │
│ Control Frame   │  X  │  0  │ FCB not meaningful   │
└─────────────────┴─────┴─────┴──────────────────────┘

Controlled Station Processing:
1. Receive frame with FCB=N
2. Process if FCB different from last accepted
3. Store FCB value for duplicate detection  
4. Send ACK/NACK
5. If duplicate detected (same FCB), send ACK but don't reprocess
```

### Address Field

The address field (A) identifies the controlled station:

```
Address Range: 1-254 (0x01-0xFE)
├─ 0x00: Reserved
├─ 0x01-0xFD: Individual station addresses
├─ 0xFE: Broadcast address (all stations)
└─ 0xFF: No station addressed
```

### Error Detection and Recovery

#### Checksum Calculation
All frames include a checksum for error detection:

```
Checksum = (Sum of all octets from C to end of ASDU) mod 256

For Variable Frame: CS = (C + A + ASDU_bytes) mod 256
For Fixed Frame:    CS = (C + A) mod 256
```

#### Error Recovery Mechanisms
1. **Checksum Errors**: Frame discarded, no acknowledgment sent
2. **Timeout**: Controlling station retransmits after timeout
3. **NACK Response**: Indicates controlled station detected error
4. **Sequence Errors**: FCB mechanism prevents duplicate processing

### Link Layer Initialization

Before application data exchange, the link layer must be initialized:

```
Controlling Station                    Controlled Station
       │                                      │
       │ ──── RESET REMOTE LINK ────────────► │
       │      (Function Code 0, FCB=0)       │
       │                                      │
       │ ◄──── ACK (E5h) ──────────────────── │
       │                                      │
       │ ──── STATUS REQUEST ───────────────► │  
       │      (Function Code 9, FCB=1)       │
       │                                      │
       │ ◄──── STATUS RESPONSE ────────────── │
       │      (Function Code 11)             │
       │                                      │
     Ready for application data exchange
```

### Supported Operation Mode

This library implements **unbalanced mode only**:

- **Controlling station** initiates all communication and never sends ACK/NACK
- **Controlled station** only responds to requests and sends ACK/NACK for received frames
- **Controlled station** queues responses and data for the **controlling station** to poll

**Balanced mode (where both stations can initiate communication) is not currently supported.**

## Application Layer (Layer 7)

The Application Layer defines the structure and meaning of application data using ASDU (Application Service Data Unit) formatting.

### ASDU Structure

Application Service Data Units carry the actual control and monitoring data:

```
ASDU Format:
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Type ID     │ Variable    │ Cause of    │ Information │
│ (1 byte)    │ Structure   │ Transmission│ Objects     │
│             │ Qualifier   │ (2 bytes)   │ (Variable)  │
│             │ (1 byte)    │             │             │
└─────────────┴─────────────┴─────────────┴─────────────┘
```

### Information Objects

Information Objects contain the actual data values and are structured as:

```
Information Object:
┌─────────────┬─────────────┬─────────────┐
│ Address     │ Information │ Time Tag    │
│ (3 bytes)   │ Elements    │ (Optional)  │
│             │ (Variable)  │             │
└─────────────┴─────────────┴─────────────┘
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

### Typical Data Exchange Flow

The IEC-101 implementation uses automatic polling where the client continuously polls the server for Class 1 (high priority) and Class 2 (normal priority) data. The server uses the ACD (Access Demand) bit to signal when Class 1 data is available, which is requested immediately by the client when detected.

#### Example 1: Idle Connection - Normal Polling Behavior

```
Controlling Station                    Controlled Station
       │                                      │
       │ ──── REQUEST CLASS 1 DATA ─────────► │
       │      (Function Code 10, FCB=0)       │
       │                                      │
       │ ◄──── RESP_NACK_NO_DATA ────────────── │
       │      (Function Code 9, ACD=0)        │
       │                                      │
       │ ──── REQUEST CLASS 2 DATA ─────────► │
       │      (Function Code 11, FCB=1)       │
       │                                      │
       │ ◄──── RESP_NACK_NO_DATA ────────────── │
       │      (Function Code 9, ACD=0)        │
       │                                      │
       │ ──── REQUEST CLASS 1 DATA ─────────► │
       │      (Function Code 10, FCB=0)       │
       │                                      │
       │      ... alternating polling continues ...
```

#### Example 2: Interrogation Command with Automatic Response Retrieval

```
Controlling Station                    Controlled Station
       │                                      │
       │ ──── INTERROGATION COMMAND ────────► │
       │      (C_IC_NA_1, FCB=0, FCV=1)      │
       │                                      │
       │ ◄──── ACK (E5h) ─────────────────────── │
       │                                      │
       │  (Server queues: Confirmation + Data + Termination)
       │                                      │
       │ ──── REQUEST CLASS 1 DATA ─────────► │ ◄─ Automatic polling
       │      (Function Code 10, FCB=1)       │
       │                                      │
       │ ◄──── CONFIRMATION ─────────────────── │
       │      (C_IC_NA_1, ACTIVATION_CON, ACD=1) │
       │                                      │
       │ ──── REQUEST CLASS 2 DATA ─────────► │ ◄─ Automatic polling  
       │      (Function Code 11, FCB=0)       │
       │                                      │
       │ ◄──── SINGLE POINT DATA ────────────── │
       │      (M_SP_NA_1, ACD=1)              │
       │                                      │
       │ ──── REQUEST CLASS 2 DATA ─────────► │
       │      (Function Code 11, FCB=1)       │
       │                                      │
       │ ◄──── MEASUREMENT DATA ─────────────── │
       │      (M_ME_NB_1, ACD=1)              │
       │                                      │
       │ ──── REQUEST CLASS 1 DATA ─────────► │ ◄─ ACD=1 triggers immediate polling
       │      (Function Code 10, FCB=0)       │
       │                                      │
       │ ◄──── TERMINATION ──────────────────── │
       │      (C_IC_NA_1, ACTIVATION_TERM, ACD=0) │
       │                                      │
       │      ... normal alternating polling resumes ...
```

#### Example 3: Single Command with Immediate Response via Polling

```
Controlling Station                    Controlled Station
       │                                      │
       │ ──── SINGLE COMMAND ───────────────► │
       │      (C_SC_NA_1, FCB=0, FCV=1)      │
       │                                      │
       │ ◄──── ACK (E5h) ─────────────────────── │
       │                                      │
       │  (Server queues Class 1 confirmation, sets ACD=1)
       │                                      │
       │ ──── REQUEST CLASS 1 DATA ─────────► │ ◄─ Automatic polling detects ACD=1
       │      (Function Code 10, FCB=1)       │
       │                                      │
       │ ◄──── COMMAND CONFIRMATION ───────── │
       │      (C_SC_NA_1, ACTIVATION_CON, ACD=0) │
       │                                      │
       │      ... normal alternating polling resumes ...
```

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

This implementation has several limitations compared to the full IEC 60870-5-101 specification:

### Protocol Limitations
- **Unbalanced mode only**: Balanced mode (where both stations can initiate communication) is not supported
- **Point-to-point communication only**: Multi-drop configurations with multiple controlled stations on the same serial line are not supported
- **Limited ASDU types**: Not all ASDU data types defined in the standard are implemented - only the most commonly used types for basic SCADA operations. File transfer operations (ASDU types 120-126) are not supported

### Link Layer Limitations  
- **Data Flow Control (DFC) bit**: Basic implementation without advanced flow control mechanisms
- **Single connection per server**: Each server instance can handle only one client connection at a time
- **Serial communication only**: Implementation is coupled to serial communication via jSerialComm library

### Application Layer Limitations
- **File transfer**: File transfer operations (ASDU types 120-126) are not supported
- **Parameter commands**: Limited support for parameter setting and activation commands
- **Time synchronization**: Basic time synchronization only, without advanced time management features
- **Background scan**: Limited implementation of background scanning mechanisms


These limitations make this implementation suitable for basic SCADA applications and industrial automation scenarios, but may not be appropriate for complex or mission-critical systems requiring full protocol compliance.
