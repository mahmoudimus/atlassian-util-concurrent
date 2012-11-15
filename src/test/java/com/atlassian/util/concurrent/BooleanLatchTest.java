package com.atlassian.util.concurrent;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BooleanLatchTest {
  @Test public void singleThreadIsReleased() throws Exception {
    final AtomicInteger call = new AtomicInteger();
    final BooleanLatch latch = new BooleanLatch();
    final Exec<String> completionService = getCompletionService(factory(5, new Callable<String>() {
      public String call() throws Exception {
        latch.await();
        return String.valueOf(call.incrementAndGet());
      }
    }));
    try {
      latch.release();
      final Future<String> take = completionService.take();
      assertNotNull(take.get());
      assertEquals("1", take.get());
      Thread.sleep(10);
      // these threads were already waiting, SRSW will only notify ONE
      // thread
      // in this state - we are testing that the client who is using this
      // incorrectly will see dodgy behaviour
      final Future<String> poll = completionService.poll();
      assertNull(poll);
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
    } finally {
      completionService.shutdown();
    }
  }

  @Test public void singleThreadIsReleasedWithTimeout() throws Exception {
    final AtomicInteger call = new AtomicInteger();
    final BooleanLatch latch = new BooleanLatch();
    final Exec<String> completionService = getCompletionService(factory(5, new Callable<String>() {
      public String call() throws Exception {
        latch.await(100, TimeUnit.SECONDS);
        return String.valueOf(call.incrementAndGet());
      }
    }));
    try {
      latch.release();
      final Future<String> take = completionService.take();
      assertNotNull(take.get());
      assertEquals("1", take.get());
      Thread.sleep(10);
      // these threads were already waiting, SRSW will only notify ONE
      // thread
      // in this state - we are testing that the client who is using this
      // incorrectly will see dodgy behaviour
      final Future<String> poll = completionService.poll();
      assertNull(poll);
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
      Thread.sleep(1);
      assertNull(completionService.poll());
    } finally {
      completionService.shutdown();
    }
  }

  private CallableFactory factory(final int threads, final Callable<String> delegate) {
    final CountDownLatch start = new CountDownLatch(threads);

    final Supplier<Callable<String>> supplier = new Supplier<Callable<String>>() {
      public Callable<String> get() {
        return new Callable<String>() {
          public String call() throws Exception {
            start.countDown();
            start.await();
            return delegate.call();
          }
        };
      }
    };

    return new CallableFactory() {
      public void await() {
        try {
          start.await();
        } catch (final InterruptedException e) {
          // /CLOVER:OFF
          throw new RuntimeInterruptedException(e);
          // /CLOVER:ON
        }
      }

      public Callable<String> get() {
        return supplier.get();
      }

      public int threads() {
        return threads;
      }
    };
  }

  interface CallableFactory extends Supplier<Callable<String>> {
    int threads();

    void await();
  }

  private Exec<String> getCompletionService(final CallableFactory factory) throws InterruptedException {
    final int threads = factory.threads();
    final ExecutorService pool = newFixedThreadPool(threads, ThreadFactories.namedThreadFactory(this.getClass().getCanonicalName()));
    final ExecutorCompletionService<String> completionService = new ExecutorCompletionService<String>(pool);
    for (int i = 0; i < threads; i++) {
      completionService.submit(factory.get());
    }
    factory.await();
    return new Exec<String>(pool, completionService);
  }

  static class Exec<T> {
    final ExecutorService pool;
    final ExecutorCompletionService<T> completion;

    Exec(final ExecutorService executor, final ExecutorCompletionService<T> completion) {
      this.pool = executor;
      this.completion = completion;
    }

    public void shutdown() {
      pool.shutdownNow();
    }

    public Future<T> poll() {
      return completion.poll();
    }

    public Future<T> take() throws InterruptedException {
      return completion.take();
    }
  }
}
