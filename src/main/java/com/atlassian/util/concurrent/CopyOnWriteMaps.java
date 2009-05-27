package com.atlassian.util.concurrent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Static factory methods for creating {@link CopyOnWriteMap} and
 * {@link CopyOnWriteSortedMap} instances.
 * 
 * @author Jed Wesley-Smith
 * @deprecated use the static factory methods is {@link CopyOnWriteMap} and
 * {@link CopyOnWriteSortedMap} directly.
 */
@Deprecated
// /CLOVER:OFF
public class CopyOnWriteMaps {
    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap}.
     * 
     * @deprecated use the {@link CopyOnWriteMap#newHashMap()} instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteMap<K, V> newHashMap() {
        return CopyOnWriteMap.newHashMap();
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap}
     * using the supplied map as the initial values.
     * 
     * @deprecated use the {@link CopyOnWriteMap#newHashMap(Map)} instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteMap<K, V> newHashMap(final Map<? extends K, ? extends V> map) {
        return CopyOnWriteMap.newHashMap(map);
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying
     * {@link LinkedHashMap}. Iterators for this map will be return elements in
     * insertion order.
     * 
     * @deprecated use the {@link CopyOnWriteMap#newLinkedMap()} instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap() {
        return CopyOnWriteMap.newLinkedMap();
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying
     * {@link LinkedHashMap} using the supplied map as the initial values.
     * Iterators for this map will be return elements in insertion order.
     * 
     * @deprecated use the {@link CopyOnWriteMap#newLinkedMap(Map)} instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap(final Map<? extends K, ? extends V> map) {
        return CopyOnWriteMap.newLinkedMap(map);
    }

    //
    // sorted maps
    //

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap} and the sort uses the key's natural order.
     * 
     * @deprecated use {@link CopyOnWriteSortedMap#newTreeMap()} instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap() {
        return CopyOnWriteSortedMap.newTreeMap();
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}, the sort uses the key's natural order and
     * the initial values are supplied.
     * 
     * @param map the map to use as the initial values.
     * @deprecated use {@link CopyOnWriteSortedMap#newTreeMap(Map)} instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Map<? extends K, ? extends V> map) {
        return CopyOnWriteSortedMap.newTreeMap(map);
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}.
     * 
     * @param comparator the Comparator to use for ordering the keys.
     * @deprecated use {@link CopyOnWriteSortedMap#newTreeMap(Comparator)}
     * instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Comparator<? super K> comparator) {
        return CopyOnWriteSortedMap.newTreeMap(comparator);
    }

    /**
     * Create a new {@link CopyOnWriteSortedMap} where the underlying map
     * instances are {@link TreeMap}, the sort uses the key's natural order and
     * the initial values are supplied.
     * 
     * @param map to use as the initial values.
     * @param comparator for ordering.
     * @deprecated use {@link CopyOnWriteSortedMap#newTreeMap(Map, Comparator)}
     * instead.
     */
    @Deprecated
    public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final Map<? extends K, ? extends V> map, final Comparator<? super K> comparator) {
        return CopyOnWriteSortedMap.newTreeMap(map, comparator);
    }
}
// /CLOVER:ON
