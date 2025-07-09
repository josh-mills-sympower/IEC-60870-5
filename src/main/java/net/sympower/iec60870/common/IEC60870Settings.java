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
package net.sympower.iec60870.common;

public class IEC60870Settings {

    private int messageFragmentTimeout;

    private int cotFieldLength;
    private int commonAddressFieldLength; //
    private int ioaFieldLength;

    private int connectionTimeout;

    public IEC60870Settings() {
        this.messageFragmentTimeout = 300_000; // 5 minutes instead of 5 seconds

        this.cotFieldLength = 2;
        this.commonAddressFieldLength = 2;
        this.ioaFieldLength = 3;

        this.connectionTimeout = 600_000; // 10 minutes instead of 30 seconds
    }

    public IEC60870Settings(IEC60870Settings settings) {

        messageFragmentTimeout = settings.messageFragmentTimeout;

        cotFieldLength = settings.cotFieldLength;
        commonAddressFieldLength = settings.commonAddressFieldLength;
        ioaFieldLength = settings.ioaFieldLength;

        connectionTimeout = settings.connectionTimeout;
    }

    public int getMessageFragmentTimeout() {
        return messageFragmentTimeout;
    }

    public int getCotFieldLength() {
        return cotFieldLength;
    }

    public int getCommonAddressFieldLength() {
        return commonAddressFieldLength;
    }

    public int getIoaFieldLength() {
        return ioaFieldLength;
    }

    public void setMessageFragmentTimeout(int messageFragmentTimeout) {
        this.messageFragmentTimeout = messageFragmentTimeout;
    }

    public void setCotFieldLength(int cotFieldLength) {
        this.cotFieldLength = cotFieldLength;
    }

    public void setCommonAddressFieldLength(int commonAddressFieldLength) {
        this.commonAddressFieldLength = commonAddressFieldLength;
    }

    public void setIoaFieldLength(int ioaFieldLength) {
        this.ioaFieldLength = ioaFieldLength;
    }


    public void setConnectionTimeout(int time) {
        this.connectionTimeout = time;

    }

}
