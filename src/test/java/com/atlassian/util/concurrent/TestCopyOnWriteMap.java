package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.atlassian.util.concurrent.CopyOnWriteMap.CopyFunction;
import com.atlassian.util.concurrent.CopyOnWriteMap.Functions;

public class TestCopyOnWriteMap {

    @Test
    public void factoryCalledOnConstructor() {
        final AtomicInteger count = new AtomicInteger();
        final Map<String, String> init = MapBuilder.build("1", "o1", "2", "o2", "3", "o3");
        final Map<String, String> map = new CopyOnWriteMap<String, String>(init, new CopyOnWriteMap.CopyFunction<Map<String, String>>() {
            public Map<String, String> copy(final Map<String, String> map) {
                count.getAndIncrement();
                return new HashMap<String, String>(map);
            }
        });
        assertEquals(1, count.get());
        assertEquals(3, map.size());
        assertTrue(map.containsKey("2"));
        assertTrue(map.containsValue("o3"));
        assertEquals("o1", map.get("1"));
    }

    @Test
    public void factoryCalledOnWrite() {
        final AtomicInteger count = new AtomicInteger();
        final Map<String, String> map = new CopyOnWriteMap<String, String>(new CopyOnWriteMap.CopyFunction<Map<String, String>>() {
            public Map<String, String> copy(final Map<String, String> map) {
                count.getAndIncrement();
                return new HashMap<String, String>(map);
            }
        });

        assertEquals("should be called in ctor", 1, count.get());
        map.put("test", "test");
        assertEquals("should be called in put", 2, count.get());
        assertEquals(1, map.size());
        assertTrue(map.containsKey("test"));
        assertTrue(map.containsValue("test"));
        assertEquals("should not be called in reads", 2, count.get());
        map.putAll(MapBuilder.build("1", "test1", "2", "test2", "3", "test3"));
        assertEquals("should be called in putAll", 3, count.get());
        assertEquals(4, map.size());
        assertTrue(map.containsKey("1"));
        assertTrue(map.containsValue("test3"));
        map.remove("2");
        assertEquals("should be called in remove", 4, count.get());
        assertEquals(3, map.size());
        assertFalse(map.containsValue("test2"));
        map.clear();
        assertEquals("should be called in clear", 5, count.get());
        assertEquals(0, map.size());
    }

    @Test
    public void hashMapCopyFunction() throws Exception {
        final CopyFunction<Map<String, String>> function = Functions.hash();
        final Map<String, String> result = function.copy(new HashMap<String, String>());
        assertEquals(0, result.size());
        assertEquals(HashMap.class, result.getClass());
    }

    @Test
    public void hashAndEquality() throws Exception {
        final Map<String, String> map = MapBuilder.<String, String> builder().add("key", "value").toMap();
        final CopyOnWriteMap<String, String> cowMap = CopyOnWriteMap.newHashMap(map);
        assertEquals(map, cowMap);
        assertEquals(map.hashCode(), cowMap.hashCode());
    }

    @Test
    public void hashAndEqualityKeySet() throws Exception {
        final Map<String, String> map = MapBuilder.<String, String> builder().add("key", "value").toMap();
        final CopyOnWriteMap<String, String> cowMap = CopyOnWriteMap.newHashMap(map);
        assertEquals(map.keySet(), cowMap.keySet());
        assertEquals(map.keySet().hashCode(), cowMap.keySet().hashCode());
    }

    @Test
    public void hashAndEqualityValues() throws Exception {
        final Map<String, String> map = MapBuilder.<String, String> builder().add("key", "value").toMap();
        final CopyOnWriteMap<String, String> cowMap = CopyOnWriteMap.newHashMap(map);
        assertEquals(new ArrayList<String>(map.values()), new ArrayList<String>(cowMap.values()));
        assertEquals(new ArrayList<String>(map.values()).hashCode(), new ArrayList<String>(cowMap.values()).hashCode());
    }

    @Test
    public void hashAndEqualityEntrySet() throws Exception {
        final Map<String, String> map = MapBuilder.<String, String> builder().add("key", "value").toMap();
        final CopyOnWriteMap<String, String> cowMap = CopyOnWriteMap.newHashMap(map);
        assertEquals(map.entrySet(), cowMap.entrySet());
        assertEquals(map.entrySet().hashCode(), cowMap.entrySet().hashCode());
    }

    @Test
    public void linkedHashMapCopyFunction() throws Exception {
        final CopyFunction<Map<String, String>> function = Functions.linked();
        final Map<String, String> result = function.copy(new HashMap<String, String>());
        assertEquals(0, result.size());
        assertEquals(LinkedHashMap.class, result.getClass());
    }

    @Test
    public void modifiableValues() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        final Map<String, String> map = new CopyOnWriteMap<String, String>(new MapBuilder<String, String>().add("test", "test").add("testing",
            "testing").toMap(), new CopyOnWriteMap.CopyFunction<Map<String, String>>() {
            public Map<String, String> copy(final Map<String, String> map) {
                count.getAndIncrement();
                return new HashMap<String, String>(map);
            }
        });
        assertEquals(1, count.get());
        final Collection<String> values = map.values();
        try {
            values.add("something");
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        assertEquals(1, count.get());
        try {
            values.addAll(Arrays.asList("one", "two", "three"));
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        final Iterator<String> iterator = values.iterator();
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        try {
            iterator.remove();
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        assertEquals(1, count.get());
        assertFalse(values.remove("blah"));
        assertEquals("not modified if element not present to be removed", 1, count.get());
        assertTrue(values.remove("test"));
        assertEquals(2, count.get());
        assertEquals(1, map.size());
        assertFalse(values.retainAll(Arrays.asList("testing")));
        assertEquals(3, count.get());
        assertEquals(1, map.size());
        assertTrue(values.removeAll(Arrays.asList("test", "blah", "testing")));
        assertEquals(4, count.get());
        assertEquals(0, map.size());
    }

    @Test
    public void modifiableEntrySet() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        final Map<String, String> map = new CopyOnWriteMap<String, String>(new MapBuilder<String, String>().add("test", "test").add("testing",
            "testing").toMap(), new CopyOnWriteMap.CopyFunction<Map<String, String>>() {
            public Map<String, String> copy(final Map<String, String> map) {
                count.getAndIncrement();
                return new HashMap<String, String>(map);
            }
        });
        assertEquals(1, count.get());
        final Collection<Entry<String, String>> keys = map.entrySet();
        class E implements Map.Entry<String, String> {
            final String e;

            public E(final String e) {
                this.e = e;
            }

            public String getKey() {
                return e;
            }

            public String getValue() {
                return e;
            }

            public String setValue(final String value) {
                throw new RuntimeException("should not be called, don't use UnsupportedOp here");
            }
        }

        try {
            keys.add(new E("something"));
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        assertEquals(1, count.get());
        try {
            keys.addAll(Arrays.asList(new E("one"), new E("two"), new E("three")));
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        final Iterator<Entry<String, String>> iterator = keys.iterator();
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        try {
            iterator.remove();
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        assertEquals(1, count.get());
        assertFalse(keys.remove("blah"));
        assertEquals("not modified if element not present to be removed", 1, count.get());
        assertTrue(keys.remove(new E("test")));
        assertEquals(2, count.get());
        assertEquals(1, map.size());
        assertFalse(keys.retainAll(Arrays.asList(new E("testing"))));
        assertEquals(3, count.get());
        assertEquals(1, map.size());
        assertTrue(keys.removeAll(Arrays.asList(new E("test"), new E("blah"), new E("testing"))));
        assertEquals(4, count.get());
        assertEquals(0, map.size());
    }

    @Test
    public void modifiableKeySet() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        final Map<String, String> map = new CopyOnWriteMap<String, String>(new MapBuilder<String, String>().add("test", "test").add("testing",
            "testing").toMap(), new CopyOnWriteMap.CopyFunction<Map<String, String>>() {
            public Map<String, String> copy(final Map<String, String> map) {
                count.getAndIncrement();
                return new HashMap<String, String>(map);
            }
        });
        assertEquals(1, count.get());
        final Collection<String> keys = map.keySet();
        try {
            keys.add("something");
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        assertEquals(1, count.get());
        try {
            keys.addAll(Arrays.asList("one", "two", "three"));
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        final Iterator<String> iterator = keys.iterator();
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        try {
            iterator.remove();
            fail("UnsupportedOp expected");
        } catch (final UnsupportedOperationException ignore) {}
        assertEquals(1, count.get());
        assertFalse(keys.remove("blah"));
        assertEquals("not modified if element not present to be removed", 1, count.get());
        assertTrue(keys.remove("test"));
        assertEquals(2, count.get());
        assertEquals(1, map.size());
        assertFalse(keys.retainAll(Arrays.asList("testing")));
        assertEquals(3, count.get());
        assertEquals(1, map.size());
        assertTrue(keys.removeAll(Arrays.asList("test", "blah", "testing")));
        assertEquals(4, count.get());
        assertEquals(0, map.size());
    }

    @Test
    public void nullCopyFunction() throws Exception {
        try {
            new CopyOnWriteMap<String, String>(null);
            fail("Should have thrown IllegalArgumentEx");
        } catch (final IllegalArgumentException ignore) {}
    }

    @Test
    public void emptyMapNullCopyFunction() throws Exception {
        try {
            new CopyOnWriteMap<String, String>(new HashMap<String, String>(), null);
            fail("Should have thrown IllegalArgumentEx");
        } catch (final IllegalArgumentException ignore) {}
    }

    @Test
    public void nullMapWithCopyFunction() throws Exception {
        try {
            final CopyOnWriteMap.CopyFunction<Map<String, String>> hashFunction = CopyOnWriteMap.Functions.hash();
            new CopyOnWriteMap<String, String>(null, hashFunction);
            fail("Should have thrown IllegalArgumentEx");
        } catch (final IllegalArgumentException ignore) {}
    }

    @Test
    public void copyFunctionReturnsNull() throws Exception {
        try {
            new CopyOnWriteMap<String, String>(new CopyOnWriteMap.CopyFunction<Map<String, String>>() {
                public Map<String, String> copy(final Map<String, String> map) {
                    return null;
                };
            });
            fail("Should have thrown IllegalArgumentEx");
        } catch (final IllegalArgumentException ignore) {}
    }
}

class MapBuilder<K, V> {
    private final Map<K, V> map = new HashMap<K, V>();

    static <S> Map<S, S> build(final S... elements) {
        if (elements.length % 2 != 0) {
            throw new IllegalArgumentException("must have even number of elements: " + elements.length);
        }
        final MapBuilder<S, S> result = new MapBuilder<S, S>();
        for (int i = 0; i < elements.length; i = i + 2) {
            result.add(elements[i], elements[i + 1]);
        }
        return result.toMap();
    }

    static <K, V> MapBuilder<K, V> builder() {
        return new MapBuilder<K, V>();
    }

    MapBuilder<K, V> add(final K key, final V value) {
        map.put(key, value);
        return this;
    }

    Entry<K, V> entry(final K key, final V value) {
        return new Entry<K, V>() {
            public K getKey() {
                return key;
            }

            public V getValue() {
                return value;
            }

            public V setValue(final V arg0) {
                throw new UnsupportedOperationException();
            };
        };
    }

    Map<K, V> toMap() {
        return Collections.unmodifiableMap(new HashMap<K, V>(map));
    }
}