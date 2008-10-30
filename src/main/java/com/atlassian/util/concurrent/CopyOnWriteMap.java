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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A thread-safe variant of {@link Map} in which all mutative operations (the "destructive"
 * operations described by {@link Map} put, remove and so on) are implemented by making a fresh copy
 * of the underlying map.
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
 * responsible for the underlying Map implementation (for instance a HashMap, TreeMap,
 * ListOrderedMap etc.) and therefore the semantics of what this map will cope with as far as null
 * keys and values, iteration ordering etc.
 * <p>
 * There are supplied {@link Functions} for the common Collections {@link Map} implementations.
 * <p>
 * Views of the keys, values and entries are modifiable and will cause a copy.
 * <p>
 * <strong>Please note</strong> that the thread-safety guarantees are limited to the thread-safety
 * of the non-mutative (non-destructive) operations of the underlying map implementation. For
 * instance some implementations such as {@link WeakHashMap} and {@link LinkedHashMap} with access
 * ordering are actually structurally modified by the {@link #get(Object)} method and are therefore
 * not suitable candidates as delegates for this class.
 */
@ThreadSafe public class CopyOnWriteMap<K, V> extends AbstractCopyOnWriteMap<K, V, Map<K, V>> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 7935514534647505917L;

    public interface CopyFunction<M extends Map<?, ?>> extends AbstractCopyOnWriteMap.CopyFunction<M> {}

    //
    // factory methods
    //

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap}.
     */
    public static <K, V> CopyOnWriteMap<K, V> newHashMap() {
        return new CopyOnWriteMap<K, V>(Functions.<K, V> hash());
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap} using the supplied
     * map as the initial values.
     */
    public static <K, V> CopyOnWriteMap<K, V> newHashMap(final Map<K, V> map) {
        return new CopyOnWriteMap<K, V>(map, Functions.<K, V> hash());
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link LinkedHashMap}. Iterators for
     * this map will be return elements in insertion order.
     */
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap() {
        return new CopyOnWriteMap<K, V>(Functions.<K, V> linked());
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link LinkedHashMap} using the
     * supplied map as the initial values. Iterators for this map will be return elements in
     * insertion order.
     */
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap(final Map<K, V> map) {
        return new CopyOnWriteMap<K, V>(map, Functions.<K, V> linked());
    }

    //
    // constructors
    //

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to initialize the values
     * and the {@link CopyFunction} for creating our actual delegate instances.
     * 
     * @param map the initial map to initialize with
     * @param factory the copy function
     */
    public CopyOnWriteMap(final Map<K, V> map, final CopyFunction<Map<K, V>> factory) {
        super(map, factory);
    }

    /**
     * Create a new empty {@link CopyOnWriteMap} with the {@link CopyFunction} for creating our
     * actual delegate instances.
     * 
     * @param factory the copy function
     */
    public CopyOnWriteMap(final CopyFunction<Map<K, V>> factory) {
        super(new HashMap<K, V>(), factory);
    }

    //
    // inner classes
    //

    /**
     * Factories that create the standard Collections {@link Map} implementations.
     */
    public static final class Functions {
        /**
         * {@link HashMap} copy function.
         * 
         * @param <K> the key type
         * @param <V> the value type
         * @return a new {@link HashMap}
         */
        public static <K, V> CopyFunction<Map<K, V>> hash() {
            return new CopyFunction<Map<K, V>>() {
                public Map<K, V> copy(final Map<K, V> map) {
                    return new HashMap<K, V>(map);
                }
            };
        }

        /**
         * {@link LinkedHashMap} copy function. Note that LinkedHashMaps with access ordering should
         * not be used.
         * 
         * @param <K> the key type
         * @param <V> the value type
         * @return a new {@link LinkedHashMap}
         */
        public static <K, V> CopyFunction<Map<K, V>> linked() {
            return new CopyFunction<Map<K, V>>() {
                public Map<K, V> copy(final Map<K, V> map) {
                    return new LinkedHashMap<K, V>(map);
                }
            };
        }
    }
}