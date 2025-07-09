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

import java.util.concurrent.PriorityBlockingQueue;

public class TimeoutManager implements Runnable {

    private final PriorityBlockingQueue<TimeoutTask> queue;

    private final Object guadedLock;

    boolean canceled;

    public TimeoutManager() {
        this.queue = new PriorityBlockingQueue<>(4);
        this.guadedLock = new Object();
    }

    public void addTimerTask(TimeoutTask task) {
        task.updateDueTime();
        removeDuplicates(task);
        this.queue.add(task);
        synchronized (this.guadedLock) {
            this.guadedLock.notifyAll();
        }
    }

    private void removeDuplicates(TimeoutTask task) {
        while (queue.remove(task)) {
            ;
        }
    }

    public void cancel() {
        this.canceled = true;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("TimeoutManager");
        TimeoutTask currTask;
        while (!canceled) {
            try {
                long sleepMillis;
                currTask = queue.take();

                while ((sleepMillis = currTask.sleepTimeFromDueTime()) > 0) {
                    queue.put(currTask);

                    synchronized (this.guadedLock) {
                        this.guadedLock.wait(sleepMillis);
                    }
                    currTask = queue.take();
                }
                if (!canceled) {
                    currTask.manExec();
                }
            } catch (InterruptedException e) {
                // Restore interrupted state...
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
