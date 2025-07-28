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
    private int commonAddressFieldLength;
    private int ioaFieldLength;
    private int linkAddressLength;

    private int connectionTimeout;
    private int interFrameDelayMs;

    public IEC60870Settings() {
        this.messageFragmentTimeout = 3_000;

        this.cotFieldLength = 2;
        this.commonAddressFieldLength = 2;
        this.ioaFieldLength = 3;
        this.linkAddressLength = 2;

        this.connectionTimeout = 6_000;
        this.interFrameDelayMs = 250;
    }

    public IEC60870Settings(IEC60870Settings settings) {

        messageFragmentTimeout = settings.messageFragmentTimeout;

        cotFieldLength = settings.cotFieldLength;
        commonAddressFieldLength = settings.commonAddressFieldLength;
        ioaFieldLength = settings.ioaFieldLength;
        linkAddressLength = settings.linkAddressLength;

        connectionTimeout = settings.connectionTimeout;
        interFrameDelayMs = settings.interFrameDelayMs;
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

    public int getLinkAddressLength() {
        return linkAddressLength;
    }

    public void setLinkAddressLength(int linkAddressLength) {
        if (linkAddressLength < 1 || linkAddressLength > 2) {
            throw new IllegalArgumentException("Link address length must be 1 or 2 bytes");
        }
        this.linkAddressLength = linkAddressLength;
    }

    public int getInterFrameDelayMs() {
        return interFrameDelayMs;
    }

    public void setInterFrameDelayMs(int interFrameDelayMs) {
        if (interFrameDelayMs < 0) {
            throw new IllegalArgumentException("Inter-frame delay must be non-negative");
        }
        this.interFrameDelayMs = interFrameDelayMs;
    }

}
