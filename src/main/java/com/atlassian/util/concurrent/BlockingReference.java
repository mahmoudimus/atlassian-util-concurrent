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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

import net.jcip.annotations.ThreadSafe;

/**
 * A Reference with queue semantics where the current reference may be retrieved
 * or taken instead, and if there is no current element then it will be block
 * until the reference becomes available. This is somewhat analogous to a single
 * element {@link BlockingQueue}.
 * <p>
 * Note: this class does not support null elements being {@link #set(Object)}
 * and will throw an exception. If the internal reference is null, then calls to
 * {@link #take()} or {@link #take(long, TimeUnit)} will block.
 * <p>
 * This class is most suited to {@link #newSRSW() SRSW} or {@link #newMRSW()
 * MRSW} usage. Multiple writers will overwrite each other's elements and the
 * chosen value will be arbitrary in the absence of any external consensus. If
 * multiple readers are waiting to {@link #take()} a value, one reader will be
 * arbitrarily chosen (similar to {@link Condition#signal()}). Multiple readers
 * can however {@link #get()} the current value if it is not null, but they may
 * see the current value more than once. If multiple readers attempt to
 * {@link #get()} a value from the SRSW reference and it is not yet present then
 * only one waiting thread may be notified, please use the MRSW version for this
 * case.
 * <p>
 * This implementation has been optimized for SRSW performance with
 * {@link #set(Object)}/{@link #take()} pairs.
 * <p>
 * Prometheus contains a similar construct called an <a href"http://prometheus.codehaus.org/javadoc/main/org/codehaus/prometheus/references/AwaitableReference.html"
 * >AwaitableReference</a>. This class is more explicit in that it handles
 * take/get separately.
 * 
 * @param <V> the value type
 * @author Jed Wesley-Smith
 * @see BlockingQueue
 */
@ThreadSafe
public class BlockingReference<V> {

    //
    // static factory methods
    //

    /**
     * Create a BlockingReference best suited to single-reader/single-writer
     * situations. In a MRSW case this instance may get missed signals if
     * multiple reader threads are all waiting on the value.
     */
    public static <V> BlockingReference<V> newSRSW() {
        return newSRSW(null);
    }

    /**
     * Create a BlockingReference best suited to single-reader/single-writer
     * situations. In a MRSW case this instance may get missed signals if
     * multiple reader threads are all waiting on the value.
     * 
     * @param initialValue the initial value
     */
    public static <V> BlockingReference<V> newSRSW(final V initialValue) {
        return new BlockingReference<V>(new BooleanLatch(), initialValue);
    }

    /**
     * Create a BlockingReference best suited to multi-reader/single-writer
     * situations. In a SRSW case this instance may not perform quite as well.
     */
    public static <V> BlockingReference<V> newMRSW() {
        return newMRSW(null);
    }

    /**
     * Create a BlockingReference best suited to multi-reader/single-writer
     * situations. In a SRSW case this instance may not perform quite as well.
     * 
     * @param initialValue the initial value
     */
    public static <V> BlockingReference<V> newMRSW(final V initialValue) {
        return new BlockingReference<V>(new PhasedLatch() {
            /*
             * Workaround for the fact that take() always calls await. Calling
             * await() on a phased latch by default waits on the <i>next</i>
             * phase (after the current one). We need to make sure we await on
             * the previous phase instead so we remember the previous phase.
             */
            private final AtomicInteger currentPhase = new AtomicInteger(super.getPhase());

            @Override
            public synchronized int getPhase() {
                try {
                    return currentPhase.get();
                } finally {
                    currentPhase.set(super.getPhase());
                }
            }
        }, initialValue);
    }

    //
    // instance vars
    //

    private final AtomicReference<V> ref = new AtomicReference<V>();
    private final ReusableLatch latch;

    //
    // ctors
    //

    BlockingReference(final ReusableLatch latch, final V initialValue) {
        this.latch = latch;
        internalSet(initialValue);
    }

    // /CLOVER:OFF
    /**
     * Creates a new SRSW BlockingReference.
     * 
     * @deprecated use {@link #newSRSW()} instead.
     */
    @Deprecated
    public BlockingReference() {
        this(new BooleanLatch(), null);
    }

    /**
     * Creates a new SRSW BlockingReference.
     * 
     * @deprecated use {@link #newSRSW()} instead.
     */
    @Deprecated
    public BlockingReference(@NotNull final V value) {
        this(new BooleanLatch(), value);
    }

    // /CLOVER:ON

    //
    // methods
    //

    /**
     * Takes the current element if it is not null and replaces it with null. If
     * the current element is null then wait until it becomes non-null.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@link Thread#interrupt() interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     */
    public @NotNull
    V take() throws InterruptedException {
        V result = null;
        while (result == null) {
            latch.await();
            result = ref.getAndSet(null);
        }
        return result;
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
     * <li>is {@link Thread#interrupt() interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     * @throws TimeoutException if the timeout is reached without another thread
     * having called {@link #set(Object)}.
     */
    public @NotNull
    V take(final long time, final TimeUnit unit) throws TimeoutException, InterruptedException {
        final Timeout timeout = Timeout.getNanosTimeout(time, unit);
        V result = null;
        while (result == null) {
            timeout.await(latch);
            result = ref.getAndSet(null);
        }
        return result;
    }

    /**
     * Gets the current element if it is not null, if it is null then this
     * method blocks and waits until it is not null. Unlike {@link #take()} it
     * does not reset the current element to null.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@link Thread#interrupt() interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     */
    public @NotNull
    V get() throws InterruptedException {
        V result = ref.get();
        while (result == null) {
            latch.await();
            result = ref.get();
        }
        return result;
    }

    /**
     * Gets the current element if it is not null, if it is null then this
     * method blocks and waits until it is not null. Unlike {@link #take()} it
     * does not reset the current element to null.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@link Thread#interrupt() interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @return the current element
     * @throws TimeoutException if the timeout is reached without another thread
     * having called {@link #set(Object)}.
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     */
    public @NotNull
    V get(final long time, @NotNull final TimeUnit unit) throws TimeoutException, InterruptedException {
        final Timeout timeout = Timeout.getNanosTimeout(time, unit);
        V result = ref.get();
        while (result == null) {
            timeout.await(latch);
            result = ref.get();
        }
        return result;
    }

    /**
     * Set the value of this reference. This method is lock-free. A thread
     * waiting in {@link #take()} or {@link #take(long, TimeUnit)} will be
     * released and given this value.
     * 
     * @param value the new value.
     */
    public void set(@NotNull final V value) {
        notNull("value", value);
        internalSet(value);
    }

    /**
     * Whether or not the current value is null or not. If this is true and the
     * next call to {@link #take()} or {@link #take(long, TimeUnit)} will not
     * block.
     * 
     * @return true if the current reference is null.
     */
    public boolean isEmpty() {
        return peek() == null;
    }

    /**
     * Return the current value whether is null or not. If this is true and the
     * next call to {@link #take()} or {@link #take(long, TimeUnit)} will not
     * block.
     * 
     * @return the current reference or null if there is none.
     */
    public @Nullable
    V peek() {
        return ref.get();
    }

    /**
     * Clear the current reference.
     */
    public void clear() {
        internalSet(null);
    }

    //
    // private
    //

    /**
     * Set the value
     * 
     * @param value maybe null
     */
    private void internalSet(@Nullable final V value) {
        ref.set(value);
        latch.release();
    }
}
