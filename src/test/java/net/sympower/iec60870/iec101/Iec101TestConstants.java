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
package net.sympower.iec60870.iec101;

import java.util.concurrent.TimeUnit;

public final class Iec101TestConstants {
    
    public static final int COMMON_ADDRESS = 1;
    public static final int LINK_ADDRESS = 1;
    public static final long TIMEOUT_SECONDS = 3;
    public static final long EXTENDED_TIMEOUT_SECONDS = 5;
    public static final long CONNECTION_TIMEOUT_SECONDS = 10;
    public static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;
    
    public static final int SINGLE_COMMAND_ADDRESS = 5000;
    public static final int DOUBLE_COMMAND_ADDRESS = 8000;
    public static final int SET_POINT_ADDRESS = 6000;
    public static final int SCALED_SET_POINT_ADDRESS = 6001;
    public static final int SHORT_FLOAT_ADDRESS = 6002;
    public static final int READ_COMMAND_ADDRESS = 7000;
    public static final int REGULATING_STEP_ADDRESS = 9000;
    public static final int PARAMETER_ACTIVATION_ADDRESS = 12000;
    public static final int TIMETAG_SINGLE_COMMAND_ADDRESS = 13000;
    public static final int TIMETAG_DOUBLE_COMMAND_ADDRESS = 14000;
    public static final int TIMETAG_REGULATING_STEP_ADDRESS = 15000;
    public static final int TIMETAG_NORMALIZED_SETPOINT_ADDRESS = 16000;
    public static final int TIMETAG_SCALED_SETPOINT_ADDRESS = 17000;
    public static final int TIMETAG_FLOAT_SETPOINT_ADDRESS = 18000;
    public static final int BITSTRING_COMMAND_ADDRESS = 19000;
    public static final int TIMETAG_BITSTRING_COMMAND_ADDRESS = 20000;
    public static final int PARAMETER_NORMALIZED_ADDRESS = 21000;
    public static final int PARAMETER_SCALED_ADDRESS = 22000;
    public static final int PARAMETER_FLOAT_ADDRESS = 23000;
    
    public static final int MEASUREMENT_ADDRESS = 100;
    public static final int COUNTER_ADDRESS = 200;
    
    public static final int SCALED_VALUE = 12345;
    public static final int COUNTER_VALUE_1 = 54321;
    public static final int COUNTER_VALUE_2 = 98765;
    public static final int TEST_SEQUENCE_VALUE = 0x5555;
    public static final int BITSTRING_VALUE_1 = 0xABCDEF12;

    public static final double NORMALIZED_TOLERANCE = 0.001;
    public static final long TIME_TOLERANCE_MS = 5000;
    
}
