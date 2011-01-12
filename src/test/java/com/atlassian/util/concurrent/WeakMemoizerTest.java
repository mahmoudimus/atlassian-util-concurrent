package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.atlassian.util.concurrent.WeakMemoizer.MappedReference;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakMemoizerTest {

    static final Function<Integer, String> lock() {
        return Functions.fromSupplier(new Supplier<String>() {
            @SuppressWarnings
            public String get() {
                return new String("test");
            }
        });
    }

    @Test
    public void testGetLock() throws Exception {
        final WeakMemoizer<Integer, String> memoizer = WeakMemoizer.weakMemoizer(lock());

        final String one = memoizer.apply(1);
        assertEquals(one, memoizer.apply(1));
    }

    @Test
    public void testLockReferenceNotNull() throws Exception {
        @SuppressWarnings
        final String value = new String("value");
        final MappedReference<String, String> ref = new MappedReference<String, String>("test", value, new ReferenceQueue<String>());
        assertNotNull(ref.getDescriptor());
        assertNotNull(ref.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferenceNullDescriptor() throws Exception {
        new MappedReference<String, String>(null, "value", new ReferenceQueue<String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferenceNullValue() throws Exception {
        new MappedReference<String, String>("ref", null, new ReferenceQueue<String>());
    }

    @Test
    public void testMany() throws Exception {
        final WeakMemoizer<Integer, String> memoizer = WeakMemoizer.weakMemoizer(lock());

        final int size = 10000;
        for (int i = 0; i < 10; i++) {
            System.gc();
            for (int j = 0; j < size; j++) {
                final String one = memoizer.apply(j);
                assertSame(one, memoizer.apply(j));
            }
        }
    }

    @Test
    public void testLosesReference() throws Exception {
        final WeakMemoizer<Integer, String> memoizer = WeakMemoizer.weakMemoizer(lock());

        final WeakReference<String> one = new WeakReference<String>(memoizer.apply(1));
        for (int i = 0; i < 10; i++) {
            System.gc();
        }
        assertNotNull(memoizer.apply(1));
        assertNull(one.get());
    }
}
