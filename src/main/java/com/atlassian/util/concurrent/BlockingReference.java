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

import static com.atlassian.util.concurrent.Assertions.notNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

/**
 * A Reference with queue semantics where rather than getting the current
 * reference it is taken instead. Analogous to a single element
 * {@link BlockingQueue}.
 * <p>
 * Note: this class does not support null elements being {@link #set(Object)}
 * and will throw an exception. If the internal reference is null, then calls to
 * {@link #take()} or {@link #take(long, TimeUnit)} will block.
 * <p>
 * This class is most suited to SRSW usage. Multiple writers will overwrite each
 * other's elements, and if multiple readers are waiting to take a value, one
 * reader will be arbitrarily chosen (similar to {@link Condition#signal()}).
 * 
 * @param <V> the value type
 * @see BlockingQueue
 */
@ThreadSafe
public class BlockingReference<V> {
    private final AtomicReference<V> ref = new AtomicReference<V>();
    private final BooleanLatch latch = new BooleanLatch();

    /**
     * Takes the current element if it is not null and replaces it with null. If
     * the current element is null then wait until it becomes non-null.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     */
    public V take() throws InterruptedException {
        while (true) {
            latch.await();
            final V result = ref.getAndSet(null);
            if (result != null) {
                return result;
            }
        }
    }

    /**
     * Takes the current element if it is not null and replaces it with null. If
     * the current element is null then wait until it becomes non-null. The
     * method will throw a {@link TimeoutException} if the timeout is reached
     * before an element becomes available.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     * @throws TimeoutException if the timeout is reached without another thread
     * having called {@link #set(Object)}.
     */
    public V take(final long timeout, final TimeUnit unit) throws TimeoutException, InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        return ref.getAndSet(null);
    }

    /**
     * Set the value of this reference. This method is lock-free. A thread
     * waiting in {@link #take()} or {@link #take(long, TimeUnit)} will be
     * released and given this value.
     * 
     * @param value the new value.
     */
    public void set(final V value) {
        notNull("value", value);
        ref.set(value);
        latch.release();
    }

    /**
     * Whether or not the current value is null or not. If this is true and the
     * next call to {@link #take()} or {@link #take(long, TimeUnit)} will not
     * block.
     * 
     * @return true if the current reference is null.
     */
    public boolean isEmpty() {
        return ref.get() == null;
    }
}
