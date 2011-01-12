package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CompletionQueueTest {
    @Test
    public void testReverseOrder() {
        final Iterator<Integer> queued = CompletionQueue.completionQueue(ImmutableList.of(callable(1), callable(2)), new Executor() {
            private final AtomicReference<Runnable> first = new AtomicReference<Runnable>();

            public void execute(final Runnable command) {
                if (first.get() == null) {
                    first.set(command);
                    return;
                }
                command.run();
                first.get().run();
            }
        }).iterator();
        assertEquals(2, queued.next().intValue());
        assertEquals(1, queued.next().intValue());
        assertFalse(queued.hasNext());
    }

    @Test
    public void testOrder() {
        final Iterator<Integer> queued = CompletionQueue.completionQueue(ImmutableList.of(callable(1), callable(2)), new Executor() {
            public void execute(final Runnable command) {
                command.run();
            }
        }).iterator();
        assertEquals(1, queued.next().intValue());
        assertEquals(2, queued.next().intValue());
        assertFalse(queued.hasNext());
    }

    @Test
    public void testSingleExecute() {
        final AtomicInteger count = new AtomicInteger();
        final Iterable<Integer> queued = CompletionQueue.completionQueue(ImmutableList.of(callable(1)), new Executor() {
            public void execute(final Runnable command) {
                count.getAndIncrement();
                command.run();
            }
        });
        assertEquals(1, queued.iterator().next().intValue());
        assertEquals(1, queued.iterator().next().intValue());
        assertEquals(1, queued.iterator().next().intValue());
        assertEquals(1, count.get());
    }

    Callable<Integer> callable(final int i) {
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                return i;
            }
        };
    }
}
