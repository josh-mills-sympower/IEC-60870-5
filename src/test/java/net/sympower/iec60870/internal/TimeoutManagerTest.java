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
package net.sympower.iec60870.internal;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimeoutManagerTest {

    @Test
    public void testTaskIsCalledOnTimeout() {
        // given
        final int timeout = 200;
        final AtomicBoolean executeWasCalled = new AtomicBoolean(false);
        final AtomicLong executionTime = new AtomicLong(0);

        TimeoutManager tm = new TimeoutManager();
        
        final long startTime = System.currentTimeMillis();
        final TimeoutTask task = new TimeoutTask(timeout) {
            @Override
            protected void execute() {
                executeWasCalled.set(true);
                executionTime.set(System.currentTimeMillis() - startTime);
            }
        };

        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(tm);
        tm.addTimerTask(task);

        // when
        await().atMost(1, TimeUnit.SECONDS).until(task::isDone);

        // then
        assertTrue("Execute method should have been called", executeWasCalled.get());
        long actualTime = executionTime.get();
        assertEquals("Execution time should be approximately " + timeout + "ms", 
                     timeout, actualTime, 40.0);

        exec.shutdown();
    }

}
