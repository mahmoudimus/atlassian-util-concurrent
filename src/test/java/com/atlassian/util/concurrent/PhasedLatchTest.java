package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Util.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class PhasedLatchTest {
    @Test public void phases() throws Exception {
        final PhasedLatch latch = new PhasedLatch();
        final AtomicInteger count = new AtomicInteger();
        final Thread client = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        latch.awaitPhase(latch.getPhase());
                        count.getAndIncrement();
                    }
                }
                catch (final InterruptedException ignore) {}
            }
        });
        client.start();

        final Thread client2 = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        latch.await();
                        count.getAndIncrement();
                    }
                }
                catch (final InterruptedException ignore) {}
            }
        });
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

        client.interrupt();
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
