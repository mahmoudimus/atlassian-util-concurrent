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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link WeakLockMap} holds a {@link Lock} per Descriptor.
 * <p>
 * Each {@link Lock} is {@link WeakReference weakly referenced} internally.
 * 
 * @param <D> comparable descriptor, should have a good hash function.
 */
class WeakLockMap<D> implements Function<D, Lock> {
    private final ConcurrentMap<D, WeakReference<Lock>> locks;
    private final ReferenceQueue<Lock> queue;

    /**
     * Construct a new {@link WeakLockMap} instance.
     * 
     * @param initialCapacity how large the internal map should be initially.
     * @throws IllegalArgumentException if the initial capacity of elements is negative.
     */
    WeakLockMap(final int initialCapacity) {
        locks = new ConcurrentHashMap<D, WeakReference<Lock>>(initialCapacity);
        queue = new ReferenceQueue<Lock>();
    }

    /**
     * Get a Lock for the supplied Descriptor.
     * 
     * @param descriptors
     * @return descriptor lock
     */
    public Lock get(final D descriptor) {
        expungeStaleEntries();

        while (true) {
            final WeakReference<Lock> reference = locks.get(descriptor);
            if (reference != null) {
                final Lock lock = reference.get();
                if (lock != null) {
                    return lock;
                }
                locks.remove(descriptor, reference);
            }
            locks.putIfAbsent(descriptor, new LockReference<D>(descriptor, queue));
        }
    }

    // expunge descriptors whose lock reference has been collected
    @SuppressWarnings("unchecked") private void expungeStaleEntries() {
        LockReference<D> ref;
        while ((ref = (LockReference<D>) queue.poll()) != null) {
            locks.remove(ref.getDescriptor(), ref);
        }
    }

    /**
     * A weak reference that maintains a reference to the descriptor so that it can be removed from
     * the map when garbage collected
     */
    private static class LockReference<D> extends WeakReference<Lock> {
        private final D descriptor;

        public LockReference(final D descriptor, final ReferenceQueue<? super Lock> q) {
            super(new ReentrantLock(), q);
            this.descriptor = notNull("descriptor", descriptor);
        }

        public D getDescriptor() {
            return descriptor;
        }
    }
}