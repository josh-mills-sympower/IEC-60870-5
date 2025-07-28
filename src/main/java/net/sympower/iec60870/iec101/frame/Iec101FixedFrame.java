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

import net.sympower.iec60870.common.IEC60870Settings;

/**
 * IEC 60870-5-101 Fixed Length Frame implementation.
 * <p>
 * Byte Offset | Field        | Size    | Value/Description
 * -----------|--------------|---------|----------------------------------
 * 0          | Start        | 1 byte  | Always 0x10
 * 1          | Control      | 1 byte  | Control field bitfield
 * 2          | Link Address | 1 byte  | Device address
 * 3          | Checksum     | 1 byte  | 8-bit sum of control + address
 * 4          | Stop         | 1 byte  | Always 0x16
 * <p>
 * Used for commands and short responses without data.
 */
public class Iec101FixedFrame extends Iec101Frame {

    private static final int RESERVED_BIT_POS = 7;
    private static final int PRM_BIT_POS = 6;
    private static final int FCB_ACD_BIT_POS = 5;
    private static final int FCV_DFC_BIT_POS = 4;
    
    private static final int FUNCTION_CODE_MASK = 0x0F;

    protected final int linkAddress;
    protected final FunctionCode functionCode;
    protected final boolean prm;   // Primary bit: 1=primary station, 0=secondary station
    protected final boolean fcv;   // Frame Count Valid (only in primary frames)
    protected final boolean fcb;   // Frame Count Bit (only in primary frames)
    protected final boolean acd;   // Access Demand (only in secondary frames)
    protected final boolean dfc;   // Data Flow Control (only in secondary frames)

    public Iec101FixedFrame(int linkAddress, FunctionCode functionCode, boolean prm, 
                           boolean fcv, boolean fcb, boolean acd, boolean dfc) {
        super(FrameType.FIXED_LENGTH);
        this.linkAddress = linkAddress;
        this.functionCode = functionCode;
        this.prm = prm;
        this.fcv = fcv;
        this.fcb = fcb;
        this.acd = acd;
        this.dfc = dfc;
    }

    public static Iec101FixedFrame decode(InputStream inputStream, IEC60870Settings settings) throws IOException {
        int controlField = readControlField(inputStream);
        int addressField = readAddressField(inputStream, settings);
        int checksum = readChecksum(inputStream);
        readAndValidateEndCharacter(inputStream);
        
        ControlField controlInfo = readControlFieldBits(controlField);
        verifyChecksum(controlField, addressField, checksum, settings);

        return new Iec101FixedFrame(addressField, controlInfo.functionCode, controlInfo.prm,
                                    controlInfo.fcv, controlInfo.fcb, controlInfo.acd, controlInfo.dfc);
    }
    
    private static int readControlField(InputStream inputStream) throws IOException {
        int controlField = inputStream.read();
        if (controlField == -1) {
            throw new IOException("Unexpected end of stream reading control field");
        }
        return controlField;
    }
    
    private static int readAddressField(InputStream inputStream, IEC60870Settings settings) throws IOException {
        int addressLength = settings.getLinkAddressLength();
        byte[] addressBytes = new byte[addressLength];
        
        for (int i = 0; i < addressLength; i++) {
            int byteValue = inputStream.read();
            if (byteValue == -1) {
                throw new IOException("Unexpected end of stream reading address field");
            }
            addressBytes[i] = (byte) byteValue;
        }
        
        return BitUtils.readBytes(addressBytes, 0, addressLength);
    }
    
    private static int readChecksum(InputStream inputStream) throws IOException {
        int checksum = inputStream.read();
        if (checksum == -1) {
            throw new IOException("Unexpected end of stream reading checksum");
        }
        return checksum;
    }
    
    private static void readAndValidateEndCharacter(InputStream inputStream) throws IOException {
        int endChar = inputStream.read();
        if (endChar == -1) {
            throw new IOException("Unexpected end of stream reading end character");
        }
        if (endChar != END_FRAME) {
            throw new IOException("Frame doesn't end with 0x16: 0x" + Integer.toHexString(endChar));
        }
    }
    
    private static ControlField readControlFieldBits(int controlField) {
        ControlField info = new ControlField();
        
        // Extract all 8 bits according to IEC-101 specification using BitUtils
        info.res = BitUtils.getBit(controlField, RESERVED_BIT_POS);  // Bit 7: Reserved - should always be 0
        info.prm = BitUtils.getBit(controlField, PRM_BIT_POS);       // Bit 6: Primary bit - determines station type
        
        // Extract shared bits 5 and 4 - interpretation depends on station type
        boolean bit5 = BitUtils.getBit(controlField, FCB_ACD_BIT_POS);  // Bit 5
        boolean bit4 = BitUtils.getBit(controlField, FCV_DFC_BIT_POS);  // Bit 4
        
        info.fcb = info.prm && bit5;  // FCB only valid for primary stations
        info.fcv = info.prm && bit4;  // FCV only valid for primary stations
        info.acd = !info.prm && bit5; // ACD only valid for secondary stations
        info.dfc = !info.prm && bit4; // DFC only valid for secondary stations
        
        // Extract function code from bits 0-3 (lower 4 bits)
        int functionCode = BitUtils.extractBits(controlField, FUNCTION_CODE_MASK);
        info.functionCode = FunctionCode.fromCode(functionCode, info.prm);
        
        return info;
    }
    
    private static void verifyChecksum(int controlField, int addressField, int actualChecksum, IEC60870Settings settings) throws IOException {
        // Checksum is calculated over control field + address field
        int addressLength = settings.getLinkAddressLength();
        byte[] frameData = new byte[1 + addressLength];
        frameData[0] = (byte) controlField;
        BitUtils.writeBytes(frameData, 1, addressField, addressLength);
        
        if (!verifyChecksum(frameData, (byte) actualChecksum)) {
            throw new IOException("Invalid checksum in fixed frame");
        }
    }
    
    private static class ControlField {
        boolean res;           // Bit 7 (0x80): Reserved bit - always 0 per IEC-101 specification
        boolean prm;           // Bit 6 (0x40): Primary bit - true=primary station, false=secondary station
        // Primary station only
        boolean fcb;           // Bit 5 (0x20): Frame Count Bit - used by primary stations for duplicate detection
        boolean fcv;           // Bit 4 (0x10): Frame Count Valid - indicates FCB is valid (primary stations)
        // Secondary station only
        boolean acd;           // Bit 5 (0x20): Access Demand - secondary station requests data (reuses FCB bit)
        boolean dfc;           // Bit 4 (0x10): Data Flow Control - secondary station flow control (reuses FCV bit)
        FunctionCode functionCode;  // Bits 0-3 (0x0F): Function code defining frame purpose
    }

    @Override
    public int encode(byte[] buffer, IEC60870Settings settings) {
        int pos = 0;
        int addressLength = settings.getLinkAddressLength();

        buffer[pos++] = START_FIXED;

        int controlField = encodeControlField();
        buffer[pos++] = (byte) controlField;

        BitUtils.writeBytes(buffer, pos, linkAddress, addressLength);
        pos += addressLength;

        // Calculate checksum over control field + address field
        byte checksum = calculateChecksum(buffer, 1, 1 + addressLength);
        buffer[pos++] = checksum;

        buffer[pos++] = END_FRAME;

        return pos;
    }

    private int encodeControlField() {
        // Start with function code in bits 0-3 (lower 4 bits)
        int controlField = functionCode.getCode();
        
        // Bit 7: Reserved bit - always 0 (don't set it)
        // Bit 6: Primary bit - 1=primary station, 0=secondary station
        if (prm) {
            controlField = BitUtils.setBit(controlField, PRM_BIT_POS);
        }
        
        // Primary stations use FCB/FCV, secondary stations use ACD/DFC (same bit positions)
        if ((prm && fcb) || (!prm && acd)) {
            controlField = BitUtils.setBit(controlField, FCB_ACD_BIT_POS);  // Set bit 5
        }
        if ((prm && fcv) || (!prm && dfc)) {
            controlField = BitUtils.setBit(controlField, FCV_DFC_BIT_POS);  // Set bit 4
        }
        
        return controlField;
    }
    
    // Getters for frame-specific fields
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
}
