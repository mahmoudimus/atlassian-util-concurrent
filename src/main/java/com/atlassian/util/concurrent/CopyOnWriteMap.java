package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.isNotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

/**
 * A thread-safe variant of {@link Map} in which all mutative operations (the "destructive" operations described by {@link Map} put, remove and so on)
 * are implemented by making a fresh copy of the underlying map.
 * <p>
 * This is ordinarily too costly, but may be <em>more</em> efficient than alternatives when traversal operations vastly out-number mutations, and is
 * useful when you cannot or don't want to synchronize traversals, yet need to preclude interference among concurrent threads. The "snapshot" style
 * iterators on the collections returned by {@link #entrySet()}, {@link #keySet()} and {@link #values()} use a reference to the internal map at the
 * point that the iterator was created. This map never changes during the lifetime of the iterator, so interference is impossible and the iterator is
 * guaranteed not to throw <tt>ConcurrentModificationException</tt>. The iterators will not reflect additions, removals, or changes to the list
 * since the iterator was created. Element-changing operations on iterators and collections themselves (remove, set, and add) are not supported. These
 * methods throw {@link UnsupportedOperationException}.
 * <p>
 * The actual copy is performed by a supplied {@link CopyFunction} object. The Factory is responsible for the underlying Map implementation (for
 * instance a HashMap, TreeMap, ListOrderedMap etc.) and therefore the semantics of what this map will cope with as far as null keys and values,
 * iteration ordering etc.
 * <p>
 * There are supplied {@link Functions} for the common Collections {@link Map} implementations.
 * <p>
 * <strong>Please note</strong> that the thread-safety guarantees are limited to the thread-safety of the non-mutative (non-destructive) operations
 * of the underlying map implementation. For instance some implementations such as {@link WeakHashMap} and {@link LinkedHashMap} are actually
 * structurally modified by the {@link #get(Object)} method and are therefore not suitable candidates as delegates for this class.
 */
public class CopyOnWriteMap<K, V> implements Map<K, V>, Serializable {

    private static final long serialVersionUID = 7935514534647505917L;

    private volatile Map<K, V> delegate;
    private final CopyFunction<K, V> factory;

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to initialize the values and the {@link CopyFunction} for creating our actual
     * delegate instances.
     * 
     * @param map
     *            the initial map to initialize with
     * @param factory
     *            the copy function
     */
    public CopyOnWriteMap(final Map<K, V> map, final CopyFunction<K, V> factory) {
        this.factory = isNotNull("CopyFunction", factory);
        this.delegate = factory.copy(isNotNull("map", map));
    }

    /**
     * Create a new empty {@link CopyOnWriteMap} with the {@link CopyFunction} for creating our actual delegate instances.
     * 
     * @param factory
     *            the copy function
     */
    public CopyOnWriteMap(final CopyFunction<K, V> factory) {
        this.factory = isNotNull("CopyFunction", factory);
        final Map<K, V> emptyMap = Collections.emptyMap();
        this.delegate = isNotNull("copied map", factory.copy(emptyMap));
    }

    // ---------------------------------------------------------------------------------------------- mutable operations

    public synchronized void clear() {
        final Map<K, V> map = factory.copy(delegate);
        map.clear();
        delegate = map;
    }

    public synchronized V remove(final Object key) {
        final Map<K, V> map = factory.copy(delegate);
        final V result = map.remove(key);
        delegate = map;
        return result;
    }

    public synchronized V put(final K key, final V value) {
        final Map<K, V> map = factory.copy(delegate);
        final V result = map.put(key, value);
        delegate = map;
        return result;
    }

    public synchronized void putAll(final Map<? extends K, ? extends V> t) {
        final Map<K, V> map = factory.copy(delegate);
        map.putAll(t);
        delegate = map;
    }

    // ------------------------------------------------------------------------------------------ unmodifiable set views

    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    public Collection<V> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    // ---------------------------------------------------------------------------------------- simple immutable getters

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

    /*
     * not on the Map interface, but delegate to the internal map anyway as AbstractMap provides a handy toString()
     */
    @Override
    public String toString() {
        return delegate.toString();
    }

    /**
     * Copy the current map. Always done under a lock so we don't get multiple threads doing this concurrently.
     */
    public static interface CopyFunction<K, V> {
        /**
         * Create a new map copied from the one supplied. Implementations should not keep a reference to this map, and must not modify the map after
         * it has been returned. This will be called under synchronization, so it should not do any IO or blocking operations.
         * 
         * @param map
         *            the map to copy. Will not be null.
         * @return a new copied map. Must not be null.
         */
        Map<K, V> copy(Map<K, V> map);
    }

    /**
     * Factories that create the standard Collections {@link Map} implementations.
     */
    public static final class Functions {
        public static <K, V> CopyFunction<K, V> hash() {
            return new CopyFunction<K, V>() {
                public Map<K, V> copy(final Map<K, V> map) {
                    return new HashMap<K, V>(map);
                }
            };
        }

        public static <K, V> CopyFunction<K, V> tree() {
            return new CopyFunction<K, V>() {
                public Map<K, V> copy(final Map<K, V> map) {
                    return new TreeMap<K, V>(map);
                }
            };
        }
    }
}