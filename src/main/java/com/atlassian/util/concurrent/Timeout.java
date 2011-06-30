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

import net.jcip.annotations.Immutable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;

/**
 * Automatically calculates elapsed time from when it is created. Useful when
 * successively calling blocking methods and a time since call time needs to be
 * worked out.
 * <p>
 * Usage:
 * 
 * <pre>
 * Timeout timeout = Timeout.getNanosTimeout(1, TimeUnit.SECONDS);
 * String str = futureString.get(timeout.getTime(), timeout.getUnit());
 * Integer num = futureInt.get(timeout.getTime(), timeout.getUnit());
 * </pre>
 * 
 * where if the first call takes quarter of a second, the second call is passed
 * the equivalent of three-quarters of a second.
 */
@Immutable
public final class Timeout {

    /**
     * Get a {@link Timeout} that uses nanosecond precision. The accuracy will
     * depend on the accuracy of {@link System#nanoTime()}.
     * 
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the <tt>time</tt> argument.
     * @return timeout with {@link TimeUnit#NANOSECONDS} precision.
     */
    public static Timeout getNanosTimeout(final long time, final TimeUnit unit) {
        return new Timeout(time, unit, TimeSuppliers.NANOS);
    }

    /**
     * Get a {@link Timeout} that uses millisecond precision. The accuracy will
     * depend on the accuracy of {@link System#currentTimeMillis()}.
     * 
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the <tt>time</tt> argument.
     * @return timeout with {@link TimeUnit#MILLISECONDS} precision.
     */
    public static Timeout getMillisTimeout(final long time, final TimeUnit unit) {
        return new Timeout(time, unit, TimeSuppliers.MILLIS);
    }

    /**
     * Factory for creating timeouts of the specified duration.
     */
    static Supplier<Timeout> timeoutFactory(final long time, final TimeUnit unit, final TimeSupplier supplier) {
        return new Supplier<Timeout>() {
            @Override
            public Timeout get() {
                return new Timeout(time, unit, supplier);
            }
        };
    }

    //
    // members
    //

    private final long created;
    private final long timeoutPeriod;
    private final TimeSupplier supplier;

    //
    // ctors
    //

    Timeout(final long time, final TimeUnit unit, final TimeSupplier supplier) {
        created = supplier.currentTime();
        this.supplier = supplier;
        timeoutPeriod = this.supplier.precision().convert(time, unit);
    }

    //
    // methods
    //

    public long getTime() {
        return (created + timeoutPeriod) - supplier.currentTime();
    }

    public TimeUnit getUnit() {
        return supplier.precision();
    }

    /**
     * Has this timeout expired
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        return getTime() <= 0;
    }

    /**
     * The original timeout period expressed in {@link #getUnit() units}
     */
    public long getTimeoutPeriod() {
        return timeoutPeriod;
    }

    //
    // util
    //

    void await(final Awaitable waitable) throws TimeoutException, InterruptedException {
        if (!waitable.await(getTime(), getUnit())) {
            throwTimeoutException();
        }
    }

    // TODO unused, retire?
    // /CLOVER:OFF
    void await(final Condition condition) throws TimeoutException, InterruptedException {
        if (!condition.await(getTime(), getUnit())) {
            throwTimeoutException();
        }
    }

    // /CLOVER:ON

    /**
     * Always throws a {@link TimeoutException}.
     * 
     * @throws TimedOutException, always.
     */
    public void throwTimeoutException() throws TimedOutException {
        throw new TimedOutException(timeoutPeriod, getUnit());
    }

    public RuntimeTimeoutException getTimeoutException() {
        return new RuntimeTimeoutException(timeoutPeriod, getUnit());
    }

    //
    // inners
    //

    /**
     * Supply time and precision to a {@link Timeout}.
     */
    interface TimeSupplier {
        long currentTime();

        TimeUnit precision();
    }

    /**
     * Default {@link TimeSupplier} implementations.
     */
    enum TimeSuppliers implements TimeSupplier {
        NANOS {
            @Override
            public long currentTime() {
                return System.nanoTime();
            };

            @Override
            public TimeUnit precision() {
                return TimeUnit.NANOSECONDS;
            };
        },
        MILLIS {
            @Override
            public long currentTime() {
                return System.currentTimeMillis();
            };

            @Override
            public TimeUnit precision() {
                return TimeUnit.MILLISECONDS;
            };
        };
    }
}
