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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

public class CopyOnWriteSortedMapTest {

    @Test public void comparator() {
        final SortedMap<String, String> map = CopyOnWriteMaps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        assertNotNull(map.comparator());
        assertEquals(String.CASE_INSENSITIVE_ORDER, map.comparator());
        map.put("one", "two");
        assertNotNull(map.comparator());
        assertEquals(String.CASE_INSENSITIVE_ORDER, map.comparator());
    }

    @Test public void firstKey() {
        final SortedMap<String, String> map = CopyOnWriteMaps.newTreeMap();
        map.put("one", "value");
        map.put("two", "value");
        map.put("three", "value");
        assertEquals("one", map.firstKey());
    }

    @Test public void lastKey() {
        final SortedMap<String, String> map = CopyOnWriteMaps.newTreeMap();
        map.put("one", "value");
        map.put("two", "value");
        map.put("three", "value");
        assertEquals("two", map.lastKey());
    }

    @Test public void headMap() {
        final SortedMap<String, String> map = CopyOnWriteMaps.newTreeMap();
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

    @Test public void tailMap() {
        final SortedMap<String, String> map = CopyOnWriteMaps.newTreeMap();
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

    @Test public void subMap() {
        final CopyOnWriteSortedMap<String, String> map = CopyOnWriteMaps.newTreeMap();
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
        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                map.put(key, value);
            }
        });
        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                map.remove(key);
            }
        });
        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                map.clear();
            }
        });

        assertUnmodifiableCollection(map.keySet(), key);
        assertUnmodifiableCollection(map.values(), value);
    }

    private static <T> void assertUnmodifiableCollection(final Collection<T> coll, final T element) {
        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                coll.clear();
            }
        });

        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                coll.remove(element);
            }
        });

        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                coll.removeAll(Collections.EMPTY_LIST);
            }
        });

        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                coll.add(element);
            }
        });

        final Collection<T> empty = Collections.emptyList();
        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                coll.addAll(empty);
            }
        });

        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                coll.retainAll(empty);
            }
        });

        assertThrowsUnsupportedOp(new Runnable() {
            public void run() {
                final Iterator<?> it = coll.iterator();
                it.next();
                it.remove();
            }
        });
    }

    static void assertThrowsUnsupportedOp(final Runnable runnable) {
        try {
            runnable.run();
            fail("should have thrown UnsupportedOperationException");
        }
        catch (final UnsupportedOperationException ignore) {}
    }
}
