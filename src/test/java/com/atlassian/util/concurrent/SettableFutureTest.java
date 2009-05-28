package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Util.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class SettableFutureTest {
    @Test
    public void isDoneOnceSet() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        assertFalse(future.isDone());
        future.set(1);
        assertTrue(future.isDone());
        assertEquals(Integer.valueOf(1), future.get());
        try {
            future.set(2);
            fail("IllegalStateException expected");
        } catch (final IllegalStateException expected) {}
        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test(expected = IllegalStateException.class)
    public void onlySettableOnce() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(1);
        future.set(2);
    }

    @Test(expected = IllegalStateException.class)
    public void onlySettableOnceWithNull() throws InterruptedException {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(null);
        future.set(2);
    }

    @Test
    public void settableWithNull() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(null);
        assertEquals(null, future.get());
    }

    @Test
    public void settableTwiceWithSameValue() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(1);
        future.set(1);
        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test
    public void settableTwiceWithNullValue() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(null);
        future.set(null);
        assertEquals(null, future.get());
    }

    @Test
    public void notCancellable() {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.cancel(false);
        assertEquals(false, future.isCancelled());
        future.cancel(true);
        assertEquals(false, future.isCancelled());
    }

    @Test
    public void getWaits() throws Exception {
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
                    // /CLOVER:OFF
                } catch (final InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                } catch (final ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                }
                // /CLOVER:ON
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

    @Test
    public void getTimeOut() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(2);
        assertEquals(Integer.valueOf(2), future.get(1, TimeUnit.NANOSECONDS));
    }

    @Test(expected = TimeoutException.class)
    public void getTimesOut() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.get(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = ExecutionException.class)
    public void getThrowsExecutionException() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.setException(new IllegalStateException());
        future.get();
    }

    @Test(expected = ExecutionException.class)
    public void getTimeoutThrowsExecutionException() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.setException(new IllegalStateException());
        future.get(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void setThrowsIfExceptionSet() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.setException(new UnsupportedOperationException());
        future.set(1);
    }

    @Test(expected = IllegalStateException.class)
    public void setExceptionThrowsIfSet() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(1);
        future.setException(new UnsupportedOperationException());
    }

    @Test(expected = IllegalStateException.class)
    public void setExceptionThrowsIfExceptionSet() throws Exception {
        final SettableFuture<Integer> future = new SettableFuture<Integer>();
        future.set(1);
        future.setException(new UnsupportedOperationException());
        future.setException(new RuntimeException());
    }
}
