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

/**
 * {@link WeakCacheFunction} caches an element per Descriptor.
 * <p>
 * Each element is {@link WeakReference weakly referenced} internally.
 * 
 * @param <K> comparable descriptor, should have a good hash function.
 */
class WeakCacheFunction<K, V> implements Function<K, V> {
    static <K, V> WeakCacheFunction<K, V> create(final int initialCapacity, final Function<K, V> delegate) {
        return new WeakCacheFunction<K, V>(initialCapacity, delegate);
    }

    private final ConcurrentMap<K, MappedReference<K, V>> locks;
    private final ReferenceQueue<V> queue = new ReferenceQueue<V>();
    private final Function<K, V> delegate;

    /**
     * Construct a new {@link WeakCacheFunction} instance.
     * 
     * @param initialCapacity how large the internal map should be initially.
     * @param delegate for creating the initial values.
     * @throws IllegalArgumentException if the initial capacity of elements is
     * negative.
     */
    WeakCacheFunction(final int initialCapacity, final @NotNull Function<K, V> delegate) {
        locks = new ConcurrentHashMap<K, MappedReference<K, V>>(initialCapacity);
        this.delegate = notNull("delegate", delegate);
    }

    /**
     * Get a Lock for the supplied Descriptor.
     * 
     * @param descriptor must not be null
     * @return descriptor lock
     */
    public V get(final K descriptor) {
        expungeStaleEntries();
        notNull("descriptor", descriptor);
        while (true) {
            final MappedReference<K, V> reference = locks.get(descriptor);
            if (reference != null) {
                final V value = reference.get();
                if (value != null) {
                    return value;
                }
                locks.remove(descriptor, reference);
            }
            locks.putIfAbsent(descriptor, new MappedReference<K, V>(descriptor, delegate.get(descriptor), queue));
        }
    }

    // expunge entries whose value reference has been collected
    @SuppressWarnings("unchecked")
    private void expungeStaleEntries() {
        MappedReference<K, V> ref;
        while ((ref = (MappedReference<K, V>) queue.poll()) != null) {
            final K key = ref.getDescriptor();
            if (key == null) {
                // DO NOT REMOVE! In theory this should not be necessary as it
                // should not be able to be null - but we have seen it happen!
                continue;
            }
            locks.remove(key, ref);
        }
    }

    /**
     * A weak reference that maintains a reference to the key so that it can be
     * removed from the map when the value is garbage collected.
     */
    static final class MappedReference<K, V> extends WeakReference<V> {
        private final K key;

        public MappedReference(final K key, final V value, final ReferenceQueue<? super V> q) {
            super(notNull("value", value), q);
            this.key = notNull("key", key);
        }

        final K getDescriptor() {
            return key;
        }
    }
}
