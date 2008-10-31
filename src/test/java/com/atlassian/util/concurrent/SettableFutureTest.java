package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Util.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class SettableFutureTest {
    @Test public void isDoneOnceSet() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        assertFalse(future.isDone());
        future.set(1);
        assertTrue(future.isDone());
        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test public void onlySettableOnce() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(1);
        try {
            future.set(2);
            fail("should not be settable twice with different value");
        }
        catch (final IllegalArgumentException expected) {}
        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test public void onlySettableOnceWithNull() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(null);
        try {
            future.set(2);
            fail("should not be settable twice with different value");
        }
        catch (final IllegalArgumentException expected) {}
        assertEquals(null, future.get());
    }

    @Test public void settableTwiceWithSameValue() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(1);
        future.set(1);
        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test public void settableTwiceWithNullValue() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(1);
        future.set(1);
        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test public void notCancellable() {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.cancel(false);
        assertEquals(false, future.isCancelled());
        future.cancel(true);
        assertEquals(false, future.isCancelled());
    }

    @Test public void getWaits() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        final CountDownLatch running = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger(3);
        final CountDownLatch complete = new CountDownLatch(1);
        new Thread(new Runnable() {
            public void run() {
                try {
                    running.countDown();
                    count.set(future.get());
                    complete.countDown();
                }
                catch (final InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                }
            }
        }).start();
        running.await();
        pause();
        assertFalse(future.isDone());
        assertEquals(3, count.get());
        future.set(12);
        complete.await();
        assertEquals(12, count.get());
        assertTrue(future.isDone());
    }
}
