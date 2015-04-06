package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.TestUtil.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class PhasedLatchTest {
  @Test public void phasesAwait() throws Exception {
    final PhasedLatch latch = new PhasedLatch();

    assertPhases(latch, element -> {
      try {
        latch.await();
      } catch (final InterruptedException e) {
        throw new Finished();
      }
    });
  }

  @Test public void phasesAwaitTimeout() throws Exception {
    final PhasedLatch latch = new PhasedLatch();

    assertPhases(latch, element -> {
      try {
        latch.await(1, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        throw new Finished();
      }
    });
  }

  @Test public void phasesAwaitPhase() throws Exception {
    final PhasedLatch latch = new PhasedLatch();

    assertPhases(latch, element -> {
      try {
        latch.awaitPhase(latch.getPhase());
      } catch (final InterruptedException e) {
        throw new Finished();
      }
    });
  }

  @Test public void phasesAwaitPhaseTimeout() throws Exception {
    final PhasedLatch latch = new PhasedLatch();

    assertPhases(latch, element -> {
      try {
        latch.awaitPhase(latch.getPhase(), 1, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        throw new Finished();
      }
    });
  }

  @SuppressWarnings("serial") static class Finished extends RuntimeException {}

  private void assertPhases(final PhasedLatch latch, final Sink<PhasedLatch> job) {
    final AtomicInteger count = new AtomicInteger();
    final Runnable worker = () -> {
      try {
        while (true) {
          job.consume(latch);
          count.getAndIncrement();
        }
      } catch (final Finished ignore) {}
    };
    final Thread client = new Thread(worker);
    final Thread client2 = new Thread(worker);
    try {
      client.start();
      client2.start();
      assertEquals(0, count.get());
      pause();
      assertEquals(0, count.get());
      latch.release();
      pause();
      assertEquals(2, count.get());
      latch.release();
      pause();
      assertEquals(4, count.get());
      latch.release();
      pause();
      assertEquals(6, count.get());
      latch.release();
    } finally {
      client.interrupt();
      client2.interrupt();
    }
  }

  @Test public void phaseComparator() throws Exception {
    final PhasedLatch.PhaseComparator comparator = new PhasedLatch.PhaseComparator();
    assertTrue(comparator.isPassed(1, 0));
    assertFalse(comparator.isPassed(1, 1));
    assertFalse(comparator.isPassed(1, 2));
  }

  @Test public void phaseComparatorAtMaxValue() throws Exception {
    final PhasedLatch.PhaseComparator comparator = new PhasedLatch.PhaseComparator();
    assertTrue(comparator.isPassed(Integer.MAX_VALUE, Integer.MAX_VALUE - 1));
    assertFalse(comparator.isPassed(Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertFalse(comparator.isPassed(Integer.MAX_VALUE, Integer.MAX_VALUE + 1));
  }
}
