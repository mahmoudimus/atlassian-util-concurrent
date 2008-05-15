package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Util.pause;
import static org.junit.Assert.*;
import com.atlassian.util.concurrent.CopyOnWriteMap.CopyFunction;
import com.atlassian.util.concurrent.CopyOnWriteMap.Functions;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void writesBlock() {
        final LatchQueue queue = new LatchQueue.SinglePass(true);
        final Map<Object, Object> map = new CopyOnWriteMap<Object, Object>(new CopyOnWriteMap.CopyFunction<Map<Object, Object>>() {
            public Map<Object, Object> copy(final Map<Object, Object> map) {
                queue.await();
                return new HashMap<Object, Object>(map);
            }
        });
        // clear out initial latch
        queue.go();

        final ExecutorService pool = Executors.newCachedThreadPool();

        try {
            final Future<Object> future1 = pool.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    return map.remove(new Object());
                }
            });
            pause();
            assertFalse(future1.isDone());
            final Future<Object> future2 = pool.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    return map.remove(new Object());
                }
            });
            assertFalse(future1.isDone());
            assertFalse(future2.isDone());
            final Future<Object> future3 = pool.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    map.clear();
                    return null;
                }
            });
            assertFalse(future1.isDone());
            assertFalse(future2.isDone());
            assertFalse(future3.isDone());
            // should only be one thread inside Factory waiting for queue
            assertEquals(1, queue.size());
            queue.go();
            try {
                assertEquals(null, future1.get());
            }
            catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }
            pause();
            assertFalse(future2.isDone());
            assertFalse(future3.isDone());
            queue.go();
            pause();
            assertTrue(future2.isDone());
            assertFalse(future3.isDone());
            queue.go();
            pause();
            assertTrue(future3.isDone());
        }
        finally {
            pool.shutdown();
        }
    }

    @Test
    public void testHashCopyFunction() throws Exception {
        final CopyFunction<Map<String, String>> function = Functions.hash();
        final Map<String, String> result = function.copy(new HashMap<String, String>());
        assertEquals(0, result.size());
        assertEquals(HashMap.class, result.getClass());
    }

    @Test
    public void unmodifiableValues() throws Exception {
        final Map<String, String> map = CopyOnWriteMap.newHashMap();
        map.put("test", "test");

        assertUnmodifiableCollection(map.entrySet(), new Map.Entry<String, String>() {
            public String getKey() {
                return "test";
            }

            public String getValue() {
                return "test";
            }

            public String setValue(final String value) {
                throw new RuntimeException("should not be called, don't use UnsupportedOp here");
            }
        });
        assertUnmodifiableCollection(map.keySet(), "test");
        assertUnmodifiableCollection(map.values(), "test");
    }

    @Test
    public void testNullCopyFunction() throws Exception {
        try {
            new CopyOnWriteMap<String, String>(null);
            fail("Should have thrown IllegalArgumentEx");
        }
        catch (final IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void emptyMapNullCopyFunction() throws Exception {
        try {
            new CopyOnWriteMap<String, String>(new HashMap<String, String>(), null);
            fail("Should have thrown IllegalArgumentEx");
        }
        catch (final IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void nullMapWithCopyFunction() throws Exception {
        try {
            final CopyOnWriteMap.CopyFunction<Map<String, String>> hashFunction = CopyOnWriteMap.Functions.hash();
            new CopyOnWriteMap<String, String>(null, hashFunction);
            fail("Should have thrown IllegalArgumentEx");
        }
        catch (final IllegalArgumentException expected) {
            // expected
        }
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
        }
        catch (final IllegalArgumentException expected) {
            // expected
        }
    }

    static <T> void assertUnmodifiableCollection(final Collection<T> coll, final T element) {
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
        catch (final UnsupportedOperationException yay) {
            // expected
        }
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