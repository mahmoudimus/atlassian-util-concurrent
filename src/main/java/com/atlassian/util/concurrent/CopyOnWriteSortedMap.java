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

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * /** A thread-safe variant of {@link SortedMap} in which all mutative
 * operations (the "destructive" operations described by {@link SortedMap} put,
 * remove and so on) are implemented by making a fresh copy of the underlying
 * map.
 * <p>
 * This is ordinarily too costly, but may be <em>more</em> efficient than
 * alternatives when traversal operations vastly out-number mutations, and is
 * useful when you cannot or don't want to synchronize traversals, yet need to
 * preclude interference among concurrent threads. The "snapshot" style
 * iterators on the collections returned by {@link #entrySet()},
 * {@link #keySet()} and {@link #values()} use a reference to the internal map
 * at the point that the iterator was created. This map never changes during the
 * lifetime of the iterator, so interference is impossible and the iterator is
 * guaranteed not to throw <tt>ConcurrentModificationException</tt>. The
 * iterators will not reflect additions, removals, or changes to the list since
 * the iterator was created. Removing elements via these iterators is not
 * supported. The mutable operations on these collections (remove, retain etc.)
 * are supported but as with the {@link Map} interface, add and addAll are not
 * and throw {@link UnsupportedOperationException}.
 * <p>
 * The actual copy is performed by a supplied {@link CopyFunction} object. The
 * Factory is responsible for the underlying {@link SortedMap} implementation
 * (for instance a {@link TreeMap}) and therefore the semantics of what this map
 * will cope with as far as null keys and values, iteration ordering etc.
 * <p>
 * There are supplied {@link Functions} for the Java Collections
 * {@link SortedMap} implementation.
 * <p>
 * Views of the keys, values and entries are modifiable and will cause a copy.
 * Views taken using {@link #subMap(Object, Object)}, {@link #headMap(Object)}
 * and {@link #tailMap(Object)} are unmodifiable.
 * <p>
 * <strong>Please note</strong> that the thread-safety guarantees are limited to
 * the thread-safety of the non-mutative (non-destructive) operations of the
 * underlying map implementation. For instance implementations with access
 * ordering are actually structurally modified by the {@link #get(Object)}
 * method and are therefore not suitable candidates as delegates for this class.
 * 
 * @author Jed Wesley-Smith
 * @param <K> the key type
 * @param <V> the value type
 */
@ThreadSafe
public class CopyOnWriteSortedMap<K, V> extends AbstractCopyOnWriteMap<K, V, SortedMap<K, V>> implements SortedMap<K, V> {

    private static final long serialVersionUID = 7375772978175545647L;

    public interface CopyFunction<M extends SortedMap<?, ?>> extends AbstractCopyOnWriteMap.CopyFunction<M> {}

    //
    // factory methods
    //

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}.
     */
    static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap() {
        return new CopyOnWriteSortedMap<K, V>(Functions.<K, V> tree());
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}.
     * 
     * @param the map to use as the initial values.
     */
    static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final SortedMap<K, V> map) {
        return new CopyOnWriteSortedMap<K, V>(map, Functions.<K, V> tree());
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}.
     * 
     * @param the Comparator to use for ordering the keys.
     */
    static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Comparator<? super K> comparator) {
        return new CopyOnWriteSortedMap<K, V>(new TreeMap<K, V>(comparator), Functions.<K, V> tree());
    }

    //
    // constructors
    //

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to
     * initialize the values and the {@link CopyFunction} for creating our
     * actual delegate instances.
     * 
     * @param map the initial map to initialize with
     * @param factory the copy function
     */
    public CopyOnWriteSortedMap(final SortedMap<K, V> map, final CopyFunction<SortedMap<K, V>> factory) {
        super(map, factory);
    }

    /**
     * Create a new empty {@link CopyOnWriteMap} with the {@link CopyFunction}
     * for creating our actual delegate instances.
     * 
     * @param factory the copy function
     */
    public CopyOnWriteSortedMap(final CopyFunction<SortedMap<K, V>> factory) {
        super(new TreeMap<K, V>(), factory);
    }

    //
    // methods
    //

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

    //
    // inner classes
    //

    /**
     * Factories that create the standard Collections {@link Map}
     * implementations.
     */
    public static final class Functions {
        public static <K, V> CopyFunction<SortedMap<K, V>> tree() {
            return new CopyFunction<SortedMap<K, V>>() {
                public SortedMap<K, V> copy(final SortedMap<K, V> map) {
                    return new TreeMap<K, V>(map);
                }
            };
        }
    }
}
