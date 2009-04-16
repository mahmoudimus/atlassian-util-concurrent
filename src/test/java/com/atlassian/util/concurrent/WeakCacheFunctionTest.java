package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.locks.Lock;

import org.junit.Test;

import com.atlassian.util.concurrent.WeakCacheFunction.MappedReference;

public class WeakCacheFunctionTest {

    @Test
    public void testGetLock() throws Exception {
        final WeakCacheFunction<Integer, Lock> lockMap = WeakCacheFunction.create(10, LockManagers.Func.<Integer> lock());

        final Lock one = lockMap.get(1);
        assertEquals(one, lockMap.get(1));
    }

    @Test
    public void testLockReferenceNotNull() throws Exception {
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
    public void testManyLocks() throws Exception {
        final int size = 10000;
        final WeakCacheFunction<Integer, Lock> lockMap = WeakCacheFunction.create(size, LockManagers.Func.<Integer> lock());

        for (int i = 0; i < 10; i++) {
            System.gc();
            for (int j = 0; j < size; j++) {
                final Lock one = lockMap.get(j);
                assertSame(one, lockMap.get(j));
            }
        }
    }
}
