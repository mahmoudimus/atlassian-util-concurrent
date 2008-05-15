package com.atlassian.util.concurrent;

import static org.junit.Assert.*;

import com.atlassian.util.concurrent.CopyOnWriteSortedMap.CopyFunction;

import org.junit.Test;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class TestCopyOnWriteSortedMap {

    @Test
    public void testComparator() {
        final CopyFunction<SortedMap<String, String>> treeFunction = CopyOnWriteSortedMap.Functions.tree();
        final SortedMap<String, String> map = new CopyOnWriteSortedMap<String, String>(new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER), treeFunction);
        assertNotNull(map.comparator());
        assertEquals(String.CASE_INSENSITIVE_ORDER, map.comparator());
    }

    @Test
    public void testFirstKey() {
        final SortedMap<String, String> map = CopyOnWriteSortedMap.newTreeMap();
        map.put("one", "value");
        map.put("two", "value");
        map.put("three", "value");
        assertEquals("one", map.firstKey());
    }

    @Test
    public void testLastKey() {
        final SortedMap<String, String> map = CopyOnWriteSortedMap.newTreeMap();
        map.put("one", "value");
        map.put("two", "value");
        map.put("three", "value");
        assertEquals("two", map.lastKey());
    }

    @Test
    public void testHeadMap() {
        final SortedMap<String, String> map = CopyOnWriteSortedMap.newTreeMap();
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        final SortedMap<String, String> headMap = map.headMap("3");
        assertEquals(2, headMap.size());
        assertTrue(headMap.containsKey("1"));
        assertTrue(headMap.containsKey("2"));
        assertTrue(headMap.containsValue("one"));
        assertTrue(headMap.containsValue("two"));

        assertUnmodifiableMap(headMap, "3", "three");
    }

    @Test
    public void testTailMap() {
        final SortedMap<String, String> map = CopyOnWriteSortedMap.newTreeMap();
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        final SortedMap<String, String> tailMap = map.tailMap("2");
        assertEquals(2, tailMap.size());
        assertTrue(tailMap.containsKey("2"));
        assertTrue(tailMap.containsKey("3"));
        assertTrue(tailMap.containsValue("two"));
        assertTrue(tailMap.containsValue("three"));

        assertUnmodifiableMap(tailMap, "1", "one");
    }

    @Test
    public void testSubMap() {
        final CopyOnWriteSortedMap<String, String> map = CopyOnWriteSortedMap.newTreeMap();
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.put("4", "four");
        final SortedMap<String, String> subMap = map.subMap("2", "4");
        assertEquals(2, subMap.size());
        assertTrue(subMap.containsKey("2"));
        assertTrue(subMap.containsKey("3"));
        assertTrue(subMap.containsValue("two"));
        assertTrue(subMap.containsValue("three"));

        assertUnmodifiableMap(subMap, "1", "one");
    }

    static <K, V> void assertUnmodifiableMap(final Map<K, V> map, final K key, final V value) {
        TestCopyOnWriteMap.assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                map.put(key, value);
            }
        });
        TestCopyOnWriteMap.assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                map.remove(key);
            }
        });
        TestCopyOnWriteMap.assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                map.clear();
            }
        });

        TestCopyOnWriteMap.assertUnmodifiableCollection(map.keySet(), key);
        TestCopyOnWriteMap.assertUnmodifiableCollection(map.values(), value);
    }
}
