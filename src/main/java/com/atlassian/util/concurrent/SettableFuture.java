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

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.ThreadSafe;

/**
 * {@link SettableFuture} is a {@link Future} implementation where the
 * responsibility for producing the result is external to the future instance,
 * unlike {@link FutureTask} where the future holds the operation (a
 * {@link Callable} or {@link Runnable} instance) and the first thread that
 * calls {@link FutureTask#run()} executes the operation.
 * <p>
 * This is useful in situations where all the inputs may not be available at
 * construction time.
 */
@ThreadSafe
public class SettableFuture<T> implements Future<T> {
    private final AtomicReference<Value<T>> ref = new AtomicReference<Value<T>>();
    private final CountDownLatch latch = new CountDownLatch(1);

    public void set(final T ref) {
        setAndCheckValue(new ReferenceValue<T>(ref));
    }

    public void setException(final Throwable throwable) {
        setAndCheckValue(new ThrowableValue<T>(throwable));
    }

    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        return ref.get().get();
    }

    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!latch.await(timeout, unit)) {
            throw new TimedOutException(timeout, unit);
        }
        return ref.get().get();
    }

    public boolean isDone() {
        return ref.get() != null;
    }

    public boolean isCancelled() {
        return isDone() && (ref.get() instanceof CancelledValue);
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
        return setValue(new CancelledValue<T>()) == null;
    }

    /**
     * Set the inner value and check that if there is an old value it equals the
     * one we are setting.
     * 
     * @param value to set.
     * @return the old value if set or null.
     */
    private void setAndCheckValue(final Value<T> value) {
        final Value<T> oldValue = setValue(value);
        if ((oldValue != null) && !value.equals(oldValue)) {
            throw new IllegalStateException("cannot change value after it has been set");
        }
    }

    /**
     * Set the inner value.
     * 
     * @param value to set.
     * @return the old value if set or null.
     */
    private Value<T> setValue(final Value<T> value) {
        while (true) {
            final Value<T> oldValue = ref.get();
            if (oldValue != null) {
                return oldValue;
            }
            // /CLOVER:OFF
            if (!ref.compareAndSet(null, value)) {
                continue;
            }
            // /CLOVER:ON
            latch.countDown();
            return null;
        }
    }

    /** the inner value */
    private static interface Value<T> {
        T get() throws ExecutionException;
    }

    /** holds a reference */
    private static class ReferenceValue<T> implements Value<T> {
        private final T value;

        ReferenceValue(final T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        @Override
        public boolean equals(final Object obj) {
            // no need to check for reference equality, not possible
            if (!(obj instanceof ReferenceValue)) {
                return false;
            }
            final ReferenceValue<?> other = (ReferenceValue<?>) obj;
            if (value != null) {
                return value.equals(other.value);
            }
            return other.value == null;
        }
    }

    /** holds an exception */
    private static class ThrowableValue<T> implements Value<T> {
        private final Throwable throwable;

        ThrowableValue(final Throwable throwable) {
            this.throwable = throwable;
        }

        public T get() throws ExecutionException {
            throw new ExecutionException(throwable);
        }

        @Override
        public boolean equals(final Object obj) {
            // no need to check for reference equality, not possible
            if (!(obj instanceof ThrowableValue)) {
                return false;
            }
            return throwable.equals(((ThrowableValue<?>) obj).throwable);
        }
    }

    // doesn't need to implement equals as cancel doesn't check
    private static class CancelledValue<T> implements Value<T> {
        public T get() throws ExecutionException {
            throw new CancellationException();
        }
    }
}
