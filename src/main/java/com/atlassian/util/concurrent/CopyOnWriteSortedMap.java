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

public class CopyOnWriteSortedMap<K, V> extends AbstractCopyOnWriteMap<K, V, SortedMap<K, V>> implements SortedMap<K, V> {

    public interface CopyFunction<M extends SortedMap<?, ?>> extends AbstractCopyOnWriteMap.CopyFunction<M> {}

    //
    // factory methods
    //

    static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap() {
        return new CopyOnWriteSortedMap<K, V>(Functions.<K, V> tree());
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
