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

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * /** A thread-safe variant of {@link SortedMap} in which all mutative operations (the
 * "destructive" operations described by {@link SortedMap} put, remove and so on) are implemented by
 * making a fresh copy of the underlying map.
 * <p>
 * This is ordinarily too costly, but may be <em>more</em> efficient than alternatives when
 * traversal operations vastly out-number mutations, and is useful when you cannot or don't want to
 * synchronize traversals, yet need to preclude interference among concurrent threads. The
 * "snapshot" style iterators on the collections returned by {@link #entrySet()}, {@link #keySet()}
 * and {@link #values()} use a reference to the internal map at the point that the iterator was
 * created. This map never changes during the lifetime of the iterator, so interference is
 * impossible and the iterator is guaranteed not to throw <tt>ConcurrentModificationException</tt>.
 * The iterators will not reflect additions, removals, or changes to the list since the iterator was
 * created. Removing elements via these iterators is not supported. The mutable operations on these
 * collections (remove, retain etc.) are supported but as with the {@link Map} interface, add and
 * addAll are not and throw {@link UnsupportedOperationException}.
 * <p>
 * The actual copy is performed by a supplied {@link CopyFunction} object. The Factory is
 * responsible for the underlying {@link SortedMap} implementation (for instance a {@link TreeMap})
 * and therefore the semantics of what this map will cope with as far as null keys and values,
 * iteration ordering etc.
 * <p>
 * There are supplied {@link Functions} for the Java Collections {@link SortedMap} implementation.
 * <p>
 * Views of the keys, values and entries are modifiable and will cause a copy. Views taken using
 * {@link #subMap(Object, Object)}, {@link #headMap(Object)} and {@link #tailMap(Object)} are
 * unmodifiable.
 * <p>
 * <strong>Please note</strong> that the thread-safety guarantees are limited to the thread-safety
 * of the non-mutative (non-destructive) operations of the underlying map implementation. For
 * instance implementations with access ordering are actually structurally modified by the
 * {@link #get(Object)} method and are therefore not suitable candidates as delegates for this
 * class.
 * 
 * @author Jed Wesley-Smith
 * @param <K> the key type
 * @param <V> the value type
 */
@ThreadSafe public abstract class CopyOnWriteSortedMap<K, V> extends AbstractCopyOnWriteMap<K, V, SortedMap<K, V>> implements SortedMap<K, V> {

    private static final long serialVersionUID = 7375772978175545647L;

    public interface CopyFunction<M extends SortedMap<?, ?>> extends AbstractCopyOnWriteMap.CopyFunction<M> {}

    //
    // factory methods
    //

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map instances are
     * {@link TreeMap} and the sort uses the key's natural order.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap() {
        return new CopyOnWriteSortedMap<K, V>() {
            @Override public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                return new TreeMap<K, V>(map);
            };
        };
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map instances are
     * {@link TreeMap}, the sort uses the key's natural order and the initial values are supplied.
     * 
     * @param the map to use as the initial values.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Map<? extends K, ? extends V> map) {
        return new CopyOnWriteSortedMap<K, V>() {
            @Override public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                return new TreeMap<K, V>(map);
            };
        };
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map instances are
     * {@link TreeMap}.
     * 
     * @param the Comparator to use for ordering the keys.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Comparator<? super K> comparator) {

        return new CopyOnWriteSortedMap<K, V>() {
            @Override public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                final TreeMap<K, V> treeMap = new TreeMap<K, V>(comparator);
                treeMap.putAll(map);
                return treeMap;
            };
        };
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map instances are
     * {@link TreeMap}, the sort uses the key's natural order and the initial values are supplied.
     * 
     * @param map to use as the initial values.
     * @param comparator for ordering.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Map<? extends K, ? extends V> map, final Comparator<? super K> comparator) {
        return new CopyOnWriteSortedMap<K, V>() {
            @Override public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                final TreeMap<K, V> treeMap = new TreeMap<K, V>(comparator);
                treeMap.putAll(map);
                return treeMap;
            };
        };
    }

    //
    // constructors
    //

    /**
     * Create a new empty {@link CopyOnWriteMap}.
     */
    public CopyOnWriteSortedMap() {
        super(Collections.<K, V> emptyMap());
    }

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to initialize the values.
     * 
     * @param map the initial map to initialize with
     */
    public CopyOnWriteSortedMap(final Map<? extends K, ? extends V> map) {
        super(map);
    }

    //
    // methods
    //

    @Override public abstract <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(N map);

    public Comparator<? super K> comparator() {
        return getDelegate().comparator();
    }

    public K firstKey() {
        return getDelegate().firstKey();
    }

    public K lastKey() {
        return getDelegate().lastKey();
    }

    public SortedMap<K, V> headMap(final K toKey) {
        return Collections.unmodifiableSortedMap(getDelegate().headMap(toKey));
    };

    public SortedMap<K, V> tailMap(final K fromKey) {
        return Collections.unmodifiableSortedMap(getDelegate().tailMap(fromKey));
    };

    public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
        return Collections.unmodifiableSortedMap(getDelegate().subMap(fromKey, toKey));
    };
}
