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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicMarkableReference;

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
 * <p>
 * This class does not support cancellation.
 */
@ThreadSafe
public class SettableFuture<T> implements Future<T> {
    private final AtomicMarkableReference<Value<T>> ref = new AtomicMarkableReference<Value<T>>(null, false);
    private final CountDownLatch latch = new CountDownLatch(1);

    public void set(final T value) {
        setHolder(new Value<T>(value));
    }

    public void setException(final Throwable throwable) {
        setHolder(new Value<T>(throwable));
    }

    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        return ref.getReference().get();
    }

    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!latch.await(timeout, unit)) {
            throw new TimedOutException(timeout, unit);
        }
        return ref.getReference().get();
    }

    public boolean isDone() {
        return ref.getReference() != null;
    }

    // not cancellable
    public boolean isCancelled() {
        return false;
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
        return false;
    }

    private void setHolder(final Value<T> value) {
        final boolean[] mark = new boolean[1];
        while (true) {
            final Value<T> oldValue = ref.get(mark);
            if (mark[0]) {
                if (!oldValue.equals(value)) {
                    throw new IllegalStateException("cannot change value after it has been set");
                }
                return;
            }
            // /CLOVER:OFF
            if (!ref.compareAndSet(null, value, false, true)) {
                continue;
            }
            // /CLOVER:ON
            latch.countDown();
            return;
        }
    }

    private static class Value<T> {
        private final T value;
        private final Throwable throwable;

        Value(final T value) {
            this.value = value;
            throwable = null;
        }

        Value(final Throwable throwable) {
            this.throwable = throwable;
            this.value = null;
        }

        T get() throws ExecutionException {
            if (throwable != null) {
                throw new ExecutionException(throwable);
            }
            return value;
        }

        /**
         * Simple equality test delegates to members.
         */
        @Override
        public boolean equals(final Object obj) {
            final Value<?> other = (Value<?>) obj;
            if (value != null) {
                return value.equals(other.value);
            }
            if (other.value != null) {
                return false;
            }
            // if throwable is null then we are a legitimate null value
            return (throwable == null) ? (other.throwable == null) : throwable.equals(other.throwable);
        }
    }
}
