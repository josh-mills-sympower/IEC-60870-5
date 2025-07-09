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

import org.junit.After;
import net.sympower.iec60870.iec101.connection.Iec101ClientConnection;
import net.sympower.iec60870.iec101.connection.Iec101ServerConnection;
import net.sympower.iec60870.spy.ClientSpy;
import net.sympower.iec60870.spy.ServerSpy;

import static org.awaitility.Awaitility.await;
import static net.sympower.iec60870.iec101.Iec101TestConstants.EXTENDED_TIMEOUT_SECONDS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMEOUT_SECONDS;
import static net.sympower.iec60870.iec101.Iec101TestConstants.TIMEOUT_UNIT;

public abstract class Iec101IntegrationTest {

    protected Iec101ServerConnection server;
    protected Iec101ClientConnection client;

    protected ServerSpy spyServerEventListener;
    protected ClientSpy spyClientListener;

    @After
    public void tearDown() {
        if (client != null && !client.isClosed()) {
            client.close();
        }
        if (server != null && !server.isClosed()) {
            server.close();
        }
    }

    protected void waitForServerToReceive(net.sympower.iec60870.common.ASduType asduType) {
        await().atMost(TIMEOUT_SECONDS, TIMEOUT_UNIT)
               .until(() -> spyServerEventListener.hasReceived(asduType));
    }

    protected void waitForClientResponse(net.sympower.iec60870.common.ASduType asduType) {
        await().atMost(EXTENDED_TIMEOUT_SECONDS, TIMEOUT_UNIT)
               .until(() -> spyClientListener.getLastReceivedAsduOfType(asduType) != null);
    }

}
