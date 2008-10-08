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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Latch-like structure where threads jump in a queue and are let off rally-style, one after
 * the other.
 */
interface LatchQueue {
    /**
     * await until a {@link #release()} is called
     */
    void await();

    /**
     * cause any threads backed up in {@link #await()} to go
     */
    void release();

    /**
     * How many threads are waiting.
     * 
     * @return the number of threads waiting.
     */
    int size();

    class SinglePass implements LatchQueue {
        private final BooleanLatch latch = new BooleanLatch();
        private final AtomicInteger count = new AtomicInteger();

        /**
         * Construct a new SinglePass LatchQueue, optionally with the first call to not wait by
         * releasing the latch.
         * 
         * @param prep if you want the first call to await to get a latch.
         */
        SinglePass(final boolean prep) {
            if (prep) {
                latch.release();
            }
        }

        public void await() {
            try {
                count.incrementAndGet();
                latch.await();
            }
            catch (final InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            }
            finally {
                count.decrementAndGet();
            }
        }

        public void release() {
            latch.release();
        }

        public int size() {
            return count.get();
        }
    }
}