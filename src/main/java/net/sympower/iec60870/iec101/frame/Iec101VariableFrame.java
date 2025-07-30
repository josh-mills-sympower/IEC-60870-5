/*
 * Original work: Copyright 2014-20 Fraunhofer ISE (OpenMUC j60870)
 *
 * Modified work: Copyright 2025 Sympower
 *
 * This file is part of the enhanced IEC 60870 library.
 * Original project: https://github.com/openmuc/j60870
 * Enhanced version: https://github.com/josh-mills-sympower/IEC-60870-5
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.sympower.iec60870.iec101.frame;

import java.io.IOException;
import java.io.InputStream;

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.internal.ExtendedDataInputStream;

/**
 * IEC 60870-5-101 Variable Length Frame implementation.
 * <p>
 * Frame Format:
 * <table>
 * <tr><th>Byte</th><th>Field</th><th>Size</th><th>Value/Description</th></tr>
 * <tr><td>0</td><td>Start</td><td>1 byte</td><td>Always 0x68</td></tr>
 * <tr><td>1</td><td>Length</td><td>1 byte</td><td>Number of bytes from control to checksum</td></tr>
 * <tr><td>2</td><td>Length</td><td>1 byte</td><td>Repeated for error detection</td></tr>
 * <tr><td>3</td><td>Start</td><td>1 byte</td><td>Always 0x68</td></tr>
 * <tr><td>4</td><td>Control</td><td>1 byte</td><td>Control field bitfield</td></tr>
 * <tr><td>5</td><td>Address</td><td>1 byte</td><td>Link address</td></tr>
 * <tr><td>6+</td><td>ASDU</td><td>Variable</td><td>Application Service Data Unit</td></tr>
 * <tr><td>N-1</td><td>Checksum</td><td>1 byte</td><td>8-bit sum of control through ASDU</td></tr>
 * <tr><td>N</td><td>Stop</td><td>1 byte</td><td>Always 0x16</td></tr>
 * </table>
 * <p>
 * Variable frames are used to transmit application data (ASDUs) between primary and secondary stations.
 * The length field specifies the number of bytes from the control field to the checksum (inclusive).
 * 
 * @since 2.0
 */
public class Iec101VariableFrame extends Iec101Frame {

    // Frame structure constants
    private static final int MAX_VARIABLE_FRAME_LENGTH = 255;
    private static final int BYTE_TO_UNSIGNED_MASK = 0xFF;
    private static final int END_OF_STREAM = -1;

    // Control field bit positions
    private static final int PRM_BIT_POS = 6;
    private static final int FCB_ACD_BIT_POS = 5;
    private static final int FCV_DFC_BIT_POS = 4;
    
    // Control field masks
    private static final int FUNCTION_CODE_MASK = 0x0F;

    protected final int linkAddress;
    protected final FunctionCode functionCode;
    protected final boolean prm;   // Primary bit: 1=primary station, 0=secondary station
    protected final boolean fcv;   // Frame Count Valid
    protected final boolean fcb;   // Frame Count Bit
    protected final boolean acd;   // Access Demand (only in secondary frames)
    protected final boolean dfc;   // Data Flow Control (only in secondary frames)
    protected final ASdu asdu;     // Application Service Data Unit

    public Iec101VariableFrame(int linkAddress, FunctionCode functionCode, boolean prm, 
                              boolean fcv, boolean fcb, boolean acd, boolean dfc, ASdu asdu) {
        super(FrameType.VARIABLE_LENGTH);
        this.linkAddress = linkAddress;
        this.functionCode = functionCode;
        this.prm = prm;
        this.fcv = fcv;
        this.fcb = fcb;
        this.acd = acd;
        this.dfc = dfc;
        this.asdu = asdu;
    }

    public static Iec101VariableFrame decode(InputStream inputStream, IEC60870Settings settings) throws IOException {
        int frameLength = readAndValidateLengthFields(inputStream);
        readAndValidateSecondStartCharacter(inputStream);
        
        int controlField = readControlField(inputStream);
        int addressField = readAddressField(inputStream, settings);
        
        ControlFieldInfo controlInfo = readControlFieldBits(controlField);
        
        int asduLength = calculateAsduLength(frameLength, settings);
        ASdu asdu = readAsduIfPresent(inputStream, settings, asduLength);
        
        int checksum = readChecksum(inputStream);
        verifyChecksum(controlField, addressField, asdu, asduLength, checksum, settings);
        
        readAndValidateEndCharacter(inputStream);

        return new Iec101VariableFrame(addressField, controlInfo.functionCode, controlInfo.prm, 
                                      controlInfo.fcv, controlInfo.fcb, controlInfo.acd, controlInfo.dfc, asdu);
    }
    
    private static int readAndValidateLengthFields(InputStream inputStream) throws IOException {
        int length1 = inputStream.read();
        if (length1 == END_OF_STREAM) {
            throw new IOException("Unexpected end of stream reading first length field");
        }

        int length2 = inputStream.read();
        if (length2 == END_OF_STREAM) {
            throw new IOException("Unexpected end of stream reading second length field");
        }

        if (length1 != length2) {
            throw new IOException("Length fields don't match: " + length1 + " != " + length2);
        }

        if (length1 < 3 || length1 > MAX_VARIABLE_FRAME_LENGTH) {
            throw new IOException("Invalid frame length: " + length1);
        }
        
        return length1;
    }
    
    private static void readAndValidateSecondStartCharacter(InputStream inputStream) throws IOException {
        int secondStart = inputStream.read();
        if (secondStart == END_OF_STREAM) {
            throw new IOException("Unexpected end of stream reading second start character");
        }
        if (secondStart != START_VARIABLE) {
            // Convert signed byte to unsigned int for hex display
            throw new IOException("Second start character is not 0x68: 0x" + 
                Integer.toHexString(secondStart & BYTE_TO_UNSIGNED_MASK));
        }
    }
    
    private static int readControlField(InputStream inputStream) throws IOException {
        int controlField = inputStream.read();
        if (controlField == END_OF_STREAM) {
            throw new IOException("Unexpected end of stream reading control field");
        }
        return controlField;
    }
    
    private static int readAddressField(InputStream inputStream, IEC60870Settings settings) throws IOException {
        int addressLength = settings.getLinkAddressLength();
        byte[] addressBytes = new byte[addressLength];
        
        for (int i = 0; i < addressLength; i++) {
            int byteValue = inputStream.read();
            if (byteValue == END_OF_STREAM) {
                throw new IOException("Unexpected end of stream reading address field");
            }
            addressBytes[i] = (byte) byteValue;
        }
        
        return BitUtils.readBytes(addressBytes, 0, addressLength);
    }
    
    private static ControlFieldInfo readControlFieldBits(int controlField) {
        ControlFieldInfo info = new ControlFieldInfo();
        
        // Extract bit 6: PRM bit determines primary vs secondary station
        info.prm = BitUtils.getBit(controlField, PRM_BIT_POS);
        
        // Extract shared bits 5 and 4 - interpretation depends on station type
        boolean bit5 = BitUtils.getBit(controlField, FCB_ACD_BIT_POS);
        boolean bit4 = BitUtils.getBit(controlField, FCV_DFC_BIT_POS);
        
        // Bits 5 and 4 are context-dependent based on frame direction
        if (info.prm) {
            // Primary station frame - bits 5/4 are FCB/FCV
            info.fcb = bit5;
            info.fcv = bit4;
            info.acd = false;
            info.dfc = false;
        } else {
            // Secondary station frame - bits 5/4 are ACD/DFC
            info.fcb = false;
            info.fcv = false;
            info.acd = bit5;
            info.dfc = bit4;
        }
        
        // Extract bits 0-3: function code value
        int functionCode = BitUtils.extractBits(controlField, FUNCTION_CODE_MASK);
        info.functionCode = FunctionCode.fromCode(functionCode, info.prm);
        
        return info;
    }
    
    private static int calculateAsduLength(int frameLength, IEC60870Settings settings) {
        // ASDU length = total length - control - address - checksum
        int addressLength = settings.getLinkAddressLength();
        return frameLength - (1 + addressLength + 1); // control + address + checksum
    }
    
    private static ASdu readAsduIfPresent(InputStream inputStream, IEC60870Settings settings, int asduLength) throws IOException {
        if (asduLength > 0) {
            ExtendedDataInputStream extendedIs = new ExtendedDataInputStream(inputStream);
            return ASdu.decode(extendedIs, settings, asduLength);
        }
        return null;
    }
    
    private static int readChecksum(InputStream inputStream) throws IOException {
        int checksum = inputStream.read();
        if (checksum == END_OF_STREAM) {
            throw new IOException("Unexpected end of stream reading checksum");
        }
        return checksum;
    }
    
    private static void verifyChecksum(int controlField, int addressField, ASdu asdu, int asduLength, 
                                      int actualChecksum, IEC60870Settings settings) throws IOException {
        // Reconstruct the frame data to verify checksum
        int addressLength = settings.getLinkAddressLength();
        byte[] frameData = new byte[1 + addressLength + asduLength];
        frameData[0] = (byte) controlField;
        BitUtils.writeBytes(frameData, 1, addressField, addressLength);
        
        if (asdu != null && asduLength > 0) {
            // Re-encode ASDU to get raw bytes for checksum
            // Allocate extra space to handle potential encoding length mismatch
            byte[] asduBytes = new byte[asduLength + 10];
            try {
                int actualEncodedLength = asdu.encode(asduBytes, 0, settings);
                
                if (actualEncodedLength != asduLength) {
                    
                    // Create new frameData with actual ASDU length for accurate checksum
                    byte[] newFrameData = new byte[1 + addressLength + actualEncodedLength];
                    newFrameData[0] = (byte) controlField;
                    BitUtils.writeBytes(newFrameData, 1, addressField, addressLength);
                    System.arraycopy(asduBytes, 0, newFrameData, 1 + addressLength, actualEncodedLength);
                    frameData = newFrameData;
                } else {
                    // Use declared length as normal
                    System.arraycopy(asduBytes, 0, frameData, 1 + addressLength, asduLength);
                }
            } catch (Exception e) {
                throw e;
            }
        }
        
        
        byte expectedChecksum = calculateChecksum(frameData, 0, frameData.length);
        if ((byte) actualChecksum != expectedChecksum) {
            throw new IOException("Checksum verification failed. Expected: 0x" +
                Integer.toHexString(expectedChecksum & BYTE_TO_UNSIGNED_MASK) + ", got: 0x" + 
                Integer.toHexString(actualChecksum & BYTE_TO_UNSIGNED_MASK));
        }
    }
    
    private static void readAndValidateEndCharacter(InputStream inputStream) throws IOException {
        int endChar = inputStream.read();
        if (endChar == END_OF_STREAM) {
            throw new IOException("Unexpected end of stream reading end character");
        }
        if (endChar != END_FRAME) {
            // Convert signed byte to unsigned int for hex display
            throw new IOException("Frame doesn't end with 0x16: 0x" + 
                Integer.toHexString(endChar & BYTE_TO_UNSIGNED_MASK));
        }
    }
    
    private static class ControlFieldInfo {
        boolean prm;
        boolean fcb;
        boolean fcv;
        boolean acd;
        boolean dfc;
        FunctionCode functionCode;
    }

    @Override
    public int encode(byte[] buffer, IEC60870Settings settings) {
        int pos = 0;
        int addressLength = settings.getLinkAddressLength();

        // Encode ASDU first to know the length
        int asduLength = 0;
        int asduStartPos = 4 + 1 + addressLength; // start + length + length + start + control + address
        if (asdu != null) {
            asduLength = asdu.encode(buffer, asduStartPos, settings);
        }

        // Length = control + address + ASDU
        int lengthField = 1 + addressLength + asduLength;
        int controlFieldPos = 4;

        buffer[pos++] = START_VARIABLE;

        // Length fields (repeated) - number of bytes from control to checksum
        buffer[pos++] = (byte) lengthField;
        buffer[pos++] = (byte) lengthField;

        buffer[pos++] = START_VARIABLE;

        int controlField = getControlField();
        buffer[pos++] = (byte) controlField;

        BitUtils.writeBytes(buffer, pos, linkAddress, addressLength);
        pos += addressLength;

        // ASDU is already encoded at the correct position, so skip ahead
        pos += asduLength;

        // Calculate checksum for control + address + ASDU 
        int checksumDataLength = 1 + addressLength + asduLength;
        byte checksum = calculateChecksum(buffer, controlFieldPos, checksumDataLength);
        buffer[pos++] = checksum;
        buffer[pos++] = END_FRAME;

        return pos;
    }

    private int getControlField() {
        int controlField = functionCode.getCode();
        // Bit 7: Reserved and must always be 0 - don't set it
        // Bit 6: PRM bit determines primary vs secondary station
        if (prm) {
            controlField = BitUtils.setBit(controlField, PRM_BIT_POS); // PRM=1 when primary transmits
        }

        // Bits 5 and 4 are reused between primary and secondary stations
        if (prm) {
            // Primary station uses FCB/FCV in bits 5/4
            if (fcb) controlField = BitUtils.setBit(controlField, FCB_ACD_BIT_POS); // Bit 5: Frame Count Bit
            if (fcv) controlField = BitUtils.setBit(controlField, FCV_DFC_BIT_POS); // Bit 4: Frame Count Valid
        } else {
            // Secondary station uses ACD/DFC in bits 5/4
            if (acd) controlField = BitUtils.setBit(controlField, FCB_ACD_BIT_POS); // Bit 5: Access Demand
            if (dfc) controlField = BitUtils.setBit(controlField, FCV_DFC_BIT_POS); // Bit 4: Data Flow Control
        }
        return controlField;
    }

    public int getLinkAddress() {
        return linkAddress;
    }

    public FunctionCode getFunctionCode() {
        return functionCode;
    }

    public boolean getPrm() {
        return prm;
    }

    public boolean getFcv() {
        return fcv;
    }

    public boolean getFcb() {
        return fcb;
    }

    public boolean getAcd() {
        return acd;
    }

    public boolean getDfc() {
        return dfc;
    }

    public ASdu getAsdu() {
        return asdu;
    }
}
