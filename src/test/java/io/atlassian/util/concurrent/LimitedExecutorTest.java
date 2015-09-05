package io.atlassian.util.concurrent;

import static io.atlassian.util.concurrent.Executors.limited;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class LimitedExecutorTest {
  @Test public void only() throws InterruptedException {
    final AtomicInteger count = new AtomicInteger(0);
    final Executor executor = limited(new NaiveExecutor(), 2);
    final PhasedLatch ready = new PhasedLatch();
    final CountDownLatch release = new CountDownLatch(1);
    class Adder implements Runnable {
      @Override public void run() {
        count.incrementAndGet();
        ready.release();
        try {
          release.await();
        } catch (final InterruptedException e) {}
      }
    }
    executor.execute(new Adder());
    executor.execute(new Adder());
    executor.execute(new Adder());
    executor.execute(new Adder());
    executor.execute(new Adder());
    ready.awaitPhase(1);
    assertEquals(2, count.get());
    assertEquals(2, count.get());
    release.countDown();
    ready.awaitPhase(4);
    assertEquals(5, count.get());
  }
}
