package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import com.atlassian.util.concurrent.WeakLockMap.LockReference;

import org.junit.Assert;
import org.junit.Test;

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.locks.Lock;

public class WeakLockMapTest {
    @Test public void testGetLock() throws Exception {
        final WeakLockMap<Integer> lockMap = new WeakLockMap<Integer>(10000);

        final Lock one = lockMap.get(1);
        assertEquals(one, lockMap.get(1));
    }

    @Test public void testLockReferenceNotNull() throws Exception {
        final LockReference<String> ref = new LockReference<String>("test", new ReferenceQueue<Lock>());
        assertNotNull(ref.getDescriptor());
    }

    @Test public void testLockReferenceNull() throws Exception {
        try {
            new LockReference<String>(null, new ReferenceQueue<Lock>());
            Assert.fail("IllegalArgumentException expected");
        }
        catch (final IllegalArgumentException expected) {}
    }

    @Test public void testManyLocks() throws Exception {
        final int size = 10000;
        final WeakLockMap<Integer> lockMap = new WeakLockMap<Integer>(size);

        for (int i = 0; i < 10; i++) {
            System.gc();
            for (int j = 0; j < size; j++) {
                final Lock one = lockMap.get(j);
                assertSame(one, lockMap.get(j));
            }
        }
    }
}
