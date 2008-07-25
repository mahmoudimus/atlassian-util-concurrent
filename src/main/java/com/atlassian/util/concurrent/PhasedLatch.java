/**
 * Copyright 2008 Atlassian Pty Ltd 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.atlassian.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A {@link PhasedLatch} is a latch that resets after it is released and can be
 * reused. A potentially waiting thread can test the current phase before
 * performing an action. The action is then guarded by that phase and can await
 * that phase.
 * <p>
 * Implementation note: currently only {@link Integer#MIN_VALUE} to
 * {@link Integer#MAX_VALUE} phases are currently supported.
 */
public class PhasedLatch {
    private final Sync sync = new Sync();

    public void release() {
        sync.releaseShared(1);
    }

    /**
     * Await the current phase.
     * 
     * @throws InterruptedException if interrupted
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(getPhase());
    }

    /**
     * Await the current phase for the specified period.
     * 
     * @param long the period of time
     * @param unit of time to measure the period in
     * @return true if the phase was passed, false otherwise
     * @throws InterruptedException if interrupted
     */
    public boolean await(final long period, final TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(getPhase(), unit.toNanos(period));
    }

    /**
     * Await the specified phase.
     * 
     * @param phase the phase to wait for
     * @throws InterruptedException if interrupted
     */
    public void awaitPhase(final int phase) throws InterruptedException {
        sync.acquireSharedInterruptibly(phase);
    }

    /**
     * Await the specified phase for the specified period.
     * 
     * @param phase the phase to wait for
     * @param period the period of time to wait for, as specified by:
     * @param unit of time to measure the period in
     * @return true if the phase was passed, false otherwise
     * @throws InterruptedException if interrupted
     */
    public boolean awaitPhase(final int phase, final long period, final TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(phase, unit.toNanos(period));
    }

    public int getPhase() {
        return sync.getCurrentPhase();
    }

    /**
     * This sync implements Phasing. The state represents the current phase as
     * an integer that continually increases. Will fail once
     * {@link Integer#MAX_VALUE} is reached.
     */
    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -7753362916930221487L;

        Sync() {
            setState(Integer.MIN_VALUE);
        }

        public int getCurrentPhase() {
            return getState();
        }

        @Override
        protected int tryAcquireShared(final int phase) {
            return (getState() > phase) ? 0 : -1;
        }

        @Override
        protected boolean tryReleaseShared(final int ignore) {
            while (true) {
                final int state = getState();
                if (compareAndSetState(state, state + 1)) {
                    return true;
                }
            }
        }
    }
}