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

import net.sympower.iec60870.common.IEC60870Settings;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base class for IEC 60870-5-101 FT1.2 frame format implementation.
 * 
 * This abstract class provides common functionality for all IEC-101 frame types:
 * - Variable length frames for data transmission (with ASDU)
 * - Fixed length frames for commands and short responses  
 * - Single character frames for acknowledgments
 * 
 * Each frame type has its own specific fields and behavior, with only
 * the frame type and encoding/decoding logic shared in this base class.
 * 
 * @since 2.0
 */
public abstract class Iec101Frame {

    // FT1.2 frame control characters
    protected static final byte START_VARIABLE = 0x68;
    protected static final byte START_FIXED = 0x10;
    protected static final byte END_FRAME = 0x16;
    
    // Single character acknowledgments
    public static final byte ACK = (byte) 0xE5;
    public static final byte NACK = (byte) 0xA2;

    public enum FrameType {
        VARIABLE_LENGTH,
        FIXED_LENGTH,
        SINGLE_CHARACTER
    }

    public enum FunctionCode {
        // Primary station functions (PRM=1)
        RESET_REMOTE_LINK(0),
        RESET_USER_PROCESS(1),
        TEST_FUNCTION_LINK(2),
        USER_DATA_CONFIRMED(3),
        USER_DATA_NO_REPLY(4),
        REQUEST_LINK_STATUS(9),
        REQUEST_CLASS_1_DATA(10),
        REQUEST_CLASS_2_DATA(11),
        
        // Secondary station functions (PRM=0)
        NACK_MESSAGE(1),
        STATUS_LINK_ACCESS_DEMAND(8),
        RESP_NACK_NO_DATA(9),
        STATUS_LINK(11),
        USER_DATA_RESPONSE(0),
        NACK_NOT_FUNCTIONING(14),
        NACK_NOT_IMPLEMENTED(15);
        
        private final int code;
        
        FunctionCode(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static FunctionCode fromCode(int code, boolean prm) {
            switch (code) {
                case 0:
                    return prm ? RESET_REMOTE_LINK : USER_DATA_RESPONSE;
                case 1:
                    return prm ? RESET_USER_PROCESS : NACK_MESSAGE;
                case 2:
                    return TEST_FUNCTION_LINK;
                case 3:
                    return USER_DATA_CONFIRMED;
                case 4:
                    return USER_DATA_NO_REPLY;
                case 8:
                    if (!prm) {
                        // Both STATUS_LINK_ACCESS_DEMAND and USER_DATA_RESPONSE use code 8
                        // Default to STATUS_LINK_ACCESS_DEMAND (original behavior)
                        return STATUS_LINK_ACCESS_DEMAND;
                    }
                    throw new IllegalArgumentException("Function code 8 not valid for primary station");
                case 9:
                    return prm ? REQUEST_LINK_STATUS : RESP_NACK_NO_DATA;
                case 10:
                    return REQUEST_CLASS_1_DATA;
                case 11:
                    return prm ? REQUEST_CLASS_2_DATA : STATUS_LINK;
                case 14:
                    return NACK_NOT_FUNCTIONING;
                case 15:
                    return NACK_NOT_IMPLEMENTED;
                default:
                    throw new IllegalArgumentException("Unknown function code: " + code);
            }
        }
    }

    protected final FrameType frameType;

    protected Iec101Frame(FrameType frameType) {
        this.frameType = frameType;
    }

    public static Iec101Frame decode(InputStream inputStream, IEC60870Settings settings) throws IOException {
        int firstByte = inputStream.read();
        if (firstByte == -1) {
            throw new IOException("Unexpected end of stream");
        }

        byte startByte = (byte) firstByte;
        
        if (startByte == ACK || startByte == NACK) {
            return new Iec101SingleCharFrame(startByte);
        }
        
        if (startByte == START_VARIABLE) {
            return Iec101VariableFrame.decode(inputStream, settings);
        }
        
        if (startByte == START_FIXED) {
            return Iec101FixedFrame.decode(inputStream, settings);
        }
        
        throw new IOException("Invalid frame start byte: 0x" + Integer.toHexString(startByte & 0xFF));
    }

    public abstract int encode(byte[] buffer, IEC60870Settings settings);

    protected static byte calculateChecksum(byte[] data, int start, int length) {
        int sum = 0;
        for (int i = start; i < start + length; i++) {
            sum += data[i] & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }

    protected static boolean verifyChecksum(byte[] data, byte expectedChecksum) {
        return calculateChecksum(data, 0, 2) == expectedChecksum;
    }

    public FrameType getFrameType() {
        return frameType;
    }
}
