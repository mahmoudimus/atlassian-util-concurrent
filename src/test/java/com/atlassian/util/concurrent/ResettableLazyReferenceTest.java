package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.TestUtil.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ResettableLazyReferenceTest {

  /**
   * Used to pound the tests
   * 
   * @param args ignored
   * @throws Exception
   */
  // public static void main(final String[] args) throws Exception {
  // final LazyReferenceTest test = new LazyReferenceTest();
  // for (int i = 0; i < 10000; i++) {
  // // test.concurrentCreate();
  // // test.getInterruptibly();
  // test.getNotInterruptable();
  // }
  // }
  @Test public void concurrentCreate() throws Exception {
    final int nThreads = 40;
    final Object[] results = new Object[nThreads];
    final AtomicInteger createCallCount = new AtomicInteger(0);
    final ResettableLazyReference<Object> ref = new ResettableLazyReference<Object>() {
      @Override protected Object create() {
        /*
         * We are trying to simulate an expensive object construction call. So
         * we do a sleep here. The idea is that we will get many threads to call
         * create() at the same time, make create "slow" and then ensure that
         * create() method was indeed invoked only once.
         */
        createCallCount.incrementAndGet();
        pause();
        pause();
        pause();
        pause();
        pause();
        return new Object();
      }
    };

    /*
     * pool size must be large enough to accommodate all Callables running in
     * parallel as they latch
     */
    final ExecutorService pool = Executors.newFixedThreadPool(nThreads);
    final CountDownLatch latch = new CountDownLatch(nThreads);

    final List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(nThreads);

    for (int i = 0; i < nThreads; i++) {
      final int j = i;
      tasks.add(new Callable<Object>() {
        public Object call() throws Exception {
          /*
           * Put in a latch to synchronize all threads and try to get them to
           * call ref.get() at the same time (to increase concurrency and make
           * this test more useful)
           */
          latch.countDown();
          latch.await();
          results[j] = ref.get();
          return results[j];
        }
      });
    }

    List<Future<Object>> futures = null;
    futures = pool.invokeAll(tasks);

    // Ensure the create() method was invoked once
    assertEquals(1, createCallCount.get());

    /*
     * Ensure that all the references are the same, use the futures in case of
     * exception
     */
    final Object result = results[0];
    for (final Future<Object> future : futures) {
      assertSame(result, future.get());
    }
    for (int i = 0; i < results.length; i++) {
      assertSame("got back different reference in '" + i + "' place", result, results[i]);
    }
    pool.shutdown();
  }

  @Test public void exception() {
    final Exception myException = new Exception();

    final ResettableLazyReference<Object> ref = new ResettableLazyReference<Object>() {
      @Override protected Object create() throws Exception {
        throw myException;
      }
    };

    try {
      ref.get();
      fail("RuntimeException should have been thrown");
    } catch (final RuntimeException yay) {
      assertNotNull(yay.getCause());
      assertTrue(myException == yay.getCause());
    }
  }

  @Test public void getNotInterruptable() throws Exception {
    final BooleanLatch latch = new BooleanLatch();

    final ResettableLazyReference<Integer> ref = new ResettableLazyReference<Integer>() {
      @Override protected Integer create() {
        // do not interrupt
        while (true) {
          try {
            latch.await();
            return 10;
          } catch (final InterruptedException e) {}
        }
      }
    };

    final Thread client = new Thread(new Runnable() {
      public void run() {
        ref.get();
      }
    }, this.getClass().getName());
    client.start();

    for (int i = 0; i < 10; i++) {
      pause();
      assertFalse(ref.isInitialized());
      client.interrupt();
    }
    pause();
    assertFalse(ref.isInitialized());

    latch.release();
    pause();
    assertTrue(ref.isInitialized());

    final int obj = ref.get();
    assertEquals(10, obj);
  }

  @Test(expected = InterruptedException.class) public void getInterruptiblyThrowsInterrupted() throws Exception {
    final ResettableLazyReference<String> ref = new ResettableLazyReference<String>() {
      @Override protected String create() throws Exception {
        return "test";
      }
    };
    Thread.currentThread().interrupt();
    ref.getInterruptibly();
  }

  @Test public void getInterruptibly() throws Exception {
    final class Result<T> {
      final T result;
      final Exception exception;

      Result(final T result) {
        this.result = result;
        this.exception = null;
      }

      Result(final Exception exception) {
        this.result = null;
        this.exception = exception;
      }
    }
    final BooleanLatch latch = new BooleanLatch();

    final ResettableLazyReference<Integer> ref = new ResettableLazyReference<Integer>() {
      @Override protected Integer create() {
        // do not interrupt
        while (true) {
          try {
            latch.await();
            return 10;
          } catch (final InterruptedException e) {}
        }
      }
    };

    final AtomicReference<Result<Integer>> result1 = new AtomicReference<Result<Integer>>();
    final Thread client1 = new Thread(new Runnable() {
      public void run() {
        try {
          result1.compareAndSet(null, new Result<Integer>(ref.getInterruptibly()));
        } catch (final Exception e) {
          result1.compareAndSet(null, new Result<Integer>(e));
        }
      }
    }, this.getClass().getName());
    client1.start();

    pause();
    final AtomicReference<Result<Integer>> result2 = new AtomicReference<Result<Integer>>();
    final Thread client2 = new Thread(new Runnable() {
      public void run() {
        try {
          result2.compareAndSet(null, new Result<Integer>(ref.getInterruptibly()));
        } catch (final Exception e) {
          result2.compareAndSet(null, new Result<Integer>(e));
        }
      }
    }, this.getClass().getName());
    client2.start();

    for (int i = 0; i < 10; i++) {
      pause();
      assertFalse(ref.isInitialized());
      client1.interrupt();
      client2.interrupt();
    }

    assertNull(result1.get());
    assertNotNull(result2.get().exception);
    assertEquals(InterruptedException.class, result2.get().exception.getClass());
    pause();
    assertFalse(ref.isInitialized());

    latch.release();
    pause();
    assertTrue(ref.isInitialized());

    {
      final int result = ref.get();
      assertEquals(10, result);
    }
    assertNotNull(result1.get());
    assertNotNull(result1.get().result);
    {
      final int result = result1.get().result;
      assertEquals(10, result);
    }
  }

  @Test(expected = CancellationException.class) public void cancellable() throws Exception {
    final ResettableLazyReference<String> ref = new ResettableLazyReference<String>() {
      // /CLOVER:OFF
      @Override protected String create() throws Exception {
        return "created!";
      }
      // /CLOVER:ON
    };
    ref.cancel();
    ref.get(); // throws
  }

  @Test public void getNotInterruptible() throws Exception {
    final ResettableLazyReference<String> ref = new ResettableLazyReference<String>() {
      @Override protected String create() throws Exception {
        return "test!";// exchange.get();
      }
    };
    Thread.currentThread().interrupt();
    ref.get();
    assertTrue(Thread.interrupted());
  }

  @Test public void initExConstructorWithBlankExecExCause() throws Exception {
    @SuppressWarnings("serial")
    final ExecutionException e = new ExecutionException("") {};
    final Exception ex = new LazyReference.InitializationException(e);
    assertSame(e, ex.getCause());
  }

  @Test public void initExConstructorWithRealExecExCause() throws Exception {
    final NoSuchMethodError er = new NoSuchMethodError();
    final ExecutionException e = new ExecutionException("", er);
    final Exception ex = new LazyReference.InitializationException(e);
    assertSame(er, ex.getCause());
  }

  @Test public void resettable() throws Exception {
    final ResettableLazyReference<Integer> count = new ResettableLazyReference<Integer>() {
      private int createCallCount;

      @Override protected Integer create() throws Exception {
        return ++createCallCount;
      }
    };
    assertEquals(Integer.valueOf(1), count.get());
    assertEquals(Integer.valueOf(1), count.get());
    assertEquals(Integer.valueOf(1), count.get());
    count.reset();
    assertEquals(Integer.valueOf(2), count.get());
    assertEquals(Integer.valueOf(2), count.get());
    count.reset();
    assertEquals(Integer.valueOf(3), count.get());
    assertEquals(Integer.valueOf(3), count.get());
    assertEquals(Integer.valueOf(3), count.get());
  }
}