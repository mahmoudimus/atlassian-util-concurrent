package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.TestUtil.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class SettableFutureTest {
  @Test public void isDoneOnceSet() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    assertFalse(future.isDone());
    future.set(1);
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertEquals(Integer.valueOf(1), future.get());
    try {
      future.set(2);
      // /CLOVER:OFF
      fail("IllegalStateException expected");
      // /CLOVER:ON
    } catch (final IllegalStateException expected) {}
    assertEquals(Integer.valueOf(1), future.get());
    assertFalse(future.cancel(true));
  }

  @Test(expected = IllegalStateException.class) public void onlySettableOnce() throws InterruptedException {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.set(1);
    future.set(2);
  }

  @Test(expected = IllegalStateException.class) public void onlySettableOnceWithNull() throws InterruptedException {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.set(null);
    future.set(2);
  }

  @Test public void settableWithNull() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.set(null);
    assertEquals(null, future.get());
    assertFalse(future.cancel(true));
    assertFalse(future.isCancelled());
  }

  @Test public void settableTwiceWithSameValue() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.set(1);
    future.set(1);
    assertEquals(Integer.valueOf(1), future.get());
    assertFalse(future.cancel(true));
    assertFalse(future.isCancelled());
  }

  @Test public void settableTwiceWithNullValue() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.set(null);
    future.set(null);
    assertEquals(null, future.get());
    assertFalse(future.cancel(true));
    assertFalse(future.isCancelled());
  }

  @Test public void cancellable() {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    assertTrue(future.cancel(false));
    assertTrue(future.isCancelled());
    assertFalse(future.cancel(false));
  }

  @Test public void cancellableMayInterrupt() {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    assertTrue(future.cancel(true));
    assertTrue(future.isCancelled());
    assertFalse(future.cancel(true));
  }

  @Test(expected = CancellationException.class) public void getThrowsIfCancelled() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.cancel(false);
    future.get();
  }

  @Test(expected = CancellationException.class) public void getTimeoutThrowsIfCancelled() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.cancel(false);
    future.get(1, TimeUnit.NANOSECONDS);
  }

  @Test(expected = IllegalStateException.class) public void setThrowsIfCancelled() {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.cancel(false);
    future.set(1);
  }

  @Test(expected = IllegalStateException.class) public void setExceptionThrowsIfCancelled() {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.cancel(false);
    future.setException(new RuntimeException());
  }

  @Test public void getWaits() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    final CountDownLatch running = new CountDownLatch(1);
    final AtomicInteger count = new AtomicInteger(3);
    final CountDownLatch complete = new CountDownLatch(1);
    new Thread(() -> {
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

  @Test public void getTimeOut() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.set(2);
    assertEquals(Integer.valueOf(2), future.get(1, TimeUnit.NANOSECONDS));
  }

  @Test(expected = TimeoutException.class) public void getTimesOut() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.get(1, TimeUnit.NANOSECONDS);
  }

  @Test(expected = UnsupportedOperationException.class) public void getThrowsWrappingExecutionException() throws Throwable {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.setException(new UnsupportedOperationException());
    assertFalse(future.isCancelled());
    try {
      future.get();
    } catch (final ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test(expected = ExecutionException.class) public void getTimeoutThrowsExecutionException() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.setException(new IllegalStateException());
    future.get(1, TimeUnit.NANOSECONDS);
  }

  @Test(expected = IllegalStateException.class) public void setThrowsIfExceptionSet() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.setException(new UnsupportedOperationException());
    future.set(1);
  }

  @Test(expected = IllegalStateException.class) public void setExceptionThrowsIfSet() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.set(1);
    future.setException(new UnsupportedOperationException());
  }

  @Test(expected = IllegalStateException.class) public void setExceptionThrowsIfExceptionSet() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    future.setException(new UnsupportedOperationException());
    future.setException(new RuntimeException());
  }

  @Test public void setExceptionDoesntThrowIfSameExceptionSet() throws Exception {
    final SettableFuture<Integer> future = new SettableFuture<Integer>();
    final UnsupportedOperationException throwable = new UnsupportedOperationException();
    future.setException(throwable);
    future.setException(throwable);
  }
}
