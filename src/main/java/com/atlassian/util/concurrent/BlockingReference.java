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

/**
 * A Reference with queue semantics where rather than getting the current
 * reference it is taken instead. Analagous to a single element
 * {@link BlockingQueue} but does not support that whole interface.
 * 
 * @param <T>
 */
public class BlockingReference<T> {
    private final AtomicReference<T> ref = new AtomicReference<T>();
    private final BooleanLatch latch = new BooleanLatch();

    // public T take() throws InterruptedException {
    // latch.await();
    // return ref.getAndSet(null);
    // }

    public T take() throws InterruptedException {
        while (true) {
            latch.await();
            final T result = ref.getAndSet(null);
            if (result != null) {
                return result;
            }
        }
    }

    public T take(final long timeout, final TimeUnit unit) throws TimeoutException, InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        return ref.getAndSet(null);
    }

    public void set(final T value) {
        notNull("value", value);
        ref.set(value);
        latch.release();
    }

    public boolean isNull() {
        return ref.get() == null;
    }
}
