package com.atlassian.util.concurrent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Static factory methods for creating {@link CopyOnWriteMap} and
 * {@link CopyOnWriteSortedMap} instances.
 * 
 * @author Jed Wesley-Smith
 */
public class CopyOnWriteMaps {
    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap}.
     */
    public static <K, V> CopyOnWriteMap<K, V> newHashMap() {
        return new CopyOnWriteMap<K, V>() {
            private static final long serialVersionUID = 5221824943734164497L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new HashMap<K, V>(map);
            }
        };
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap}
     * using the supplied map as the initial values.
     */
    public static <K, V> CopyOnWriteMap<K, V> newHashMap(final Map<? extends K, ? extends V> map) {
        return new CopyOnWriteMap<K, V>(map) {
            private static final long serialVersionUID = -7616159260882572421L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new HashMap<K, V>(map);
            }
        };
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying
     * {@link LinkedHashMap}. Iterators for this map will be return elements in
     * insertion order.
     */
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap() {
        return new CopyOnWriteMap<K, V>() {
            private static final long serialVersionUID = -4597421704607601676L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new LinkedHashMap<K, V>(map);
            }
        };
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying
     * {@link LinkedHashMap} using the supplied map as the initial values.
     * Iterators for this map will be return elements in insertion order.
     */
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap(final Map<? extends K, ? extends V> map) {
        return new CopyOnWriteMap<K, V>(map) {
            private static final long serialVersionUID = -8659999465009072124L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new LinkedHashMap<K, V>(map);
            }
        };
    }

    //
    // sorted maps
    //

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap} and the sort uses the key's natural order.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap() {
        return new CopyOnWriteSortedMap<K, V>() {
            private static final long serialVersionUID = 8015823768891873357L;

            @Override
            public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                return new TreeMap<K, V>(map);
            };
        };
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}, the sort uses the key's natural order and
     * the initial values are supplied.
     * 
     * @param the map to use as the initial values.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Map<? extends K, ? extends V> map) {
        return new CopyOnWriteSortedMap<K, V>() {
            private static final long serialVersionUID = 6065245106313875871L;

            @Override
            public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                return new TreeMap<K, V>(map);
            };
        };
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}.
     * 
     * @param the Comparator to use for ordering the keys.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Comparator<? super K> comparator) {

        return new CopyOnWriteSortedMap<K, V>() {
            private static final long serialVersionUID = -7243810284130497340L;

            @Override
            public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                final TreeMap<K, V> treeMap = new TreeMap<K, V>(comparator);
                treeMap.putAll(map);
                return treeMap;
            };
        };
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}, the sort uses the key's natural order and
     * the initial values are supplied.
     * 
     * @param map to use as the initial values.
     * @param comparator for ordering.
     */
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Map<? extends K, ? extends V> map, final Comparator<? super K> comparator) {
        return new CopyOnWriteSortedMap<K, V>() {
            private static final long serialVersionUID = -6016130690072425548L;

            @Override
            public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
                final TreeMap<K, V> treeMap = new TreeMap<K, V>(comparator);
                treeMap.putAll(map);
                return treeMap;
            };
        };
    }
}
