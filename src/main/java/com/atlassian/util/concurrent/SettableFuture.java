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

import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * @TODO Document me.
 */
@ThreadSafe public class SettableFuture<T> implements Future<T> {
    private volatile AtomicMarkableReference<T> ref = new AtomicMarkableReference<T>(null, false);
    private final CountDownLatch latch = new CountDownLatch(1);

    public void set(final T value) {
        final boolean[] mark = new boolean[1];
        while (true) {
            final T oldValue = ref.get(mark);
            if (mark[0]) {
                if (!equals(oldValue, value)) {
                    throw new IllegalArgumentException("cannot change value after it has been set");
                }
                return;
            }
            if (!ref.compareAndSet(null, value, false, true)) {
                continue;
            }
            latch.countDown();
            return;
        }
    }

    private boolean equals(final T one, final T two) {
        if (one == null) {
            return two == null;
        }
        return one.equals(two);
    }

    public T get() throws InterruptedException {
        latch.await();
        return ref.getReference();
    }

    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        if (!latch.await(timeout, unit)) {
            throw new TimedOutException(timeout, unit);
        }
        return ref.getReference();
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
}
