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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

abstract class AbstractCopyOnWriteMap<K, V, M extends Map<K, V>> {
    private volatile M delegate;
    private final CopyFunction<M> factory;

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to
     * initialize the values and the {@link CopyFunction} for creating our
     * actual delegate instances.
     * 
     * @param map the initial map to initialize with
     * @param factory the copy function
     */
    protected AbstractCopyOnWriteMap(final M map, final CopyFunction<M> factory) {
        this.factory = notNull("CopyFunction", factory);
        notNull("map", map);
        this.delegate = notNull("map", factory.copy(map));
    }

    //--------------------------------------------------------------------------
    // -------------------- mutable operations

    public synchronized void clear() {
        final M map = copy();
        map.clear();
        delegate = map;
    }

    public synchronized V remove(final Object key) {
        final M map = copy();
        final V result = map.remove(key);
        delegate = map;
        return result;
    }

    public synchronized V put(final K key, final V value) {
        final M map = copy();
        final V result = map.put(key, value);
        delegate = map;
        return result;
    }

    public synchronized void putAll(final Map<? extends K, ? extends V> t) {
        final M map = copy();
        map.putAll(t);
        delegate = map;
    }

    private synchronized M copy() {
        return factory.copy(delegate);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    public Collection<V> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    public V get(final Object key) {
        return delegate.get(key);
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public int size() {
        return delegate.size();
    }

    @Override
    public boolean equals(final Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    protected final M getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    /**
     * Copy the current map. Always done under a lock so we don't get multiple
     * threads doing this concurrently.
     */
    protected interface CopyFunction<M extends Map<?, ?>> {
        /**
         * Create a new map copied from the one supplied. Implementations should
         * not keep a reference to this map, and must not modify the map after
         * it has been returned. This will be called under synchronization, so
         * it should not do any IO or blocking operations.
         * 
         * @param map the map to copy. Will not be null.
         * @return a new copied map. Must not be null.
         */
        M copy(M map);
    }
}