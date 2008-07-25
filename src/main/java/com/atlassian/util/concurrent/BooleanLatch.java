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
import java.util.concurrent.locks.Condition;

/**
 * A {@link BooleanLatch} is a reusable latch that resets after it is released
 * and waited on. It depends on a boolean condition of being released or not and
 * becomes unreleased when one thread successfully awaits it. It is useful for
 * rally like release-wait-release coordination, and as a replacement to waiting
 * on a {@link Condition} (it should be faster as the write thread does not need
 * to acquire a lock in order to signal.
 * <p>
 * This latch is suitable for SRSW coordination. MRSW is supported but has the
 * same semantics as {@link Condition#signal()}, that is to say that
 * {@link Condition#signalAll()} is not supported and if there are multiple
 * waiters then the thread that is released is arbitrary.
 */
public class BooleanLatch {
    private final Sync sync = new Sync();

    public void release() {
        sync.release(0);
    }

    public void await() throws InterruptedException {
        sync.acquireInterruptibly(0);
    }

    public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * Synchronization control For BooleanLatch. Uses AQS state to represent
     * released state.
     */
    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -3475411235403448115L;

        private static final int RELEASED = 0;
        private static final int UNAVAILABLE = -1;

        private Sync() {
            setState(UNAVAILABLE);
        }

        @Override
        protected boolean tryAcquire(final int ignore) {
            final boolean released = (getState() == RELEASED);
            if (released) {
                compareAndSetState(RELEASED, UNAVAILABLE);
            }
            return released;
        }

        @Override
        protected boolean tryRelease(final int ignore) {
            final int state = getState();
            if (state == UNAVAILABLE) {
                setState(RELEASED);
            }
            return true;
        }
    }
}