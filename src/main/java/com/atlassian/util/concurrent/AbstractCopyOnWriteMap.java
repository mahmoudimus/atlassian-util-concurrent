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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

abstract class AbstractCopyOnWriteMap<K, V, M extends Map<K, V>> implements Map<K, V> {
    private volatile M delegate;
    private final CopyFunction<M> factory;
    private final transient EntrySet entrySet = new EntrySet();
    private final transient KeySet keySet = new KeySet();
    private final transient Values values = new Values();

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

    //
    // mutable operations
    //

    public synchronized final void clear() {
        final M map = copy();
        map.clear();
        delegate = map;
    }

    public synchronized final V remove(final Object key) {
        // short circuit if key doesn't exist
        if (!delegate.containsKey(key)) {
            return null;
        }
        final M map = copy();
        final V result = map.remove(key);
        delegate = map;
        return result;
    }

    public synchronized final V put(final K key, final V value) {
        final M map = copy();
        final V result = map.put(key, value);
        delegate = map;
        return result;
    }

    public synchronized final void putAll(final Map<? extends K, ? extends V> t) {
        final M map = copy();
        map.putAll(t);
        delegate = map;
    }

    private synchronized M copy() {
        return factory.copy(delegate);
    }

    //
    // Collection views
    //

    public final Set<Map.Entry<K, V>> entrySet() {
        return entrySet;
    }

    public final Set<K> keySet() {
        return keySet;
    }

    public final Collection<V> values() {
        return values;
    }

    //
    // delegate operations
    //

    public final boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    public final boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    public final V get(final Object key) {
        return delegate.get(key);
    }

    public final boolean isEmpty() {
        return delegate.isEmpty();
    }

    public final int size() {
        return delegate.size();
    }

    @Override
    public final boolean equals(final Object o) {
        return delegate.equals(o);
    }

    @Override
    public final int hashCode() {
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

    private class KeySet implements Set<K> {

        //
        // mutable operations
        //

        public void clear() {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                map.keySet().clear();
                delegate = map;
            }
        }

        public boolean remove(final Object o) {
            return AbstractCopyOnWriteMap.this.remove(o) != null;
        }

        public boolean removeAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.keySet().removeAll(c);
                delegate = map;
                return result;
            }
        }

        public boolean retainAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.keySet().retainAll(c);
                delegate = map;
                return result;
            }
        }

        //
        // delegate operations
        //

        public boolean contains(final Object o) {
            return delegate.containsKey(o);
        }

        public boolean containsAll(final Collection<?> c) {
            return delegate.keySet().containsAll(c);
        }

        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        public Iterator<K> iterator() {
            return new UnmodifiableIterator<K>(delegate.keySet().iterator());
        }

        public int size() {
            return delegate.keySet().size();
        }

        public Object[] toArray() {
            return delegate.keySet().toArray();
        }

        public <T> T[] toArray(final T[] a) {
            return delegate.keySet().toArray(a);
        }

        //
        // unsupported operations
        //

        public boolean add(final K o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(final Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }
    }

    private final class Values implements Collection<V> {

        public void clear() {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                map.values().clear();
                delegate = map;
            }
        }

        public boolean remove(final Object o) {
            synchronized (AbstractCopyOnWriteMap.this) {
                if (!contains(o)) {
                    return false;
                }
                final M map = copy();
                final boolean result = map.values().remove(o);
                delegate = map;
                return result;
            }
        }

        public boolean removeAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.values().removeAll(c);
                delegate = map;
                return result;
            }
        }

        public boolean retainAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.values().retainAll(c);
                delegate = map;
                return result;
            }
        }

        //
        // delegate operations
        //

        public boolean contains(final Object o) {
            return delegate.values().contains(o);
        }

        public boolean containsAll(final Collection<?> c) {
            return delegate.values().containsAll(c);
        }

        public Iterator<V> iterator() {
            return new UnmodifiableIterator<V>(delegate.values().iterator());
        }

        public boolean isEmpty() {
            return delegate.values().isEmpty();
        }

        public int size() {
            return delegate.values().size();
        }

        public Object[] toArray() {
            return delegate.values().toArray();
        }

        public <T> T[] toArray(final T[] a) {
            return delegate.values().toArray(a);
        }

        //
        // unsupported operations
        //

        public boolean add(final V o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(final Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }
    }

    private class EntrySet implements Set<Map.Entry<K, V>> {

        public void clear() {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                map.entrySet().clear();
                delegate = map;
            }
        }

        public boolean remove(final Object o) {
            synchronized (AbstractCopyOnWriteMap.this) {
                if (!contains(o)) {
                    return false;
                }
                final M map = copy();
                final boolean result = map.entrySet().remove(o);
                delegate = map;
                return result;
            }
        }

        public boolean removeAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.entrySet().removeAll(c);
                delegate = map;
                return result;
            }
        }

        public boolean retainAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.entrySet().retainAll(c);
                delegate = map;
                return result;
            }
        }

        //
        // delegate operations
        //

        public boolean contains(final Object o) {
            return delegate.entrySet().contains(o);
        }

        public boolean containsAll(final Collection<?> c) {
            return delegate.entrySet().containsAll(c);
        }

        public boolean isEmpty() {
            return delegate.entrySet().isEmpty();
        }

        public Iterator<Entry<K, V>> iterator() {
            return new UnmodifiableIterator<Entry<K, V>>(delegate.entrySet().iterator());
        }

        public int size() {
            return delegate.entrySet().size();
        }

        public Object[] toArray() {
            return delegate.entrySet().toArray();
        }

        public <T> T[] toArray(final T[] a) {
            return delegate.entrySet().toArray(a);
        }

        //
        // unsupported operations
        //

        public boolean add(final Map.Entry<K, V> o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(final Collection<? extends Map.Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }
    }

    private class UnmodifiableIterator<T> implements Iterator<T> {
        private final Iterator<T> delegate;

        public UnmodifiableIterator(final Iterator<T> delegate) {
            this.delegate = delegate;
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public T next() {
            return delegate.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}