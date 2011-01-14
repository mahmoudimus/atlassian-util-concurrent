package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.atlassian.util.concurrent.AsyncCompletion.Exceptions;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncCompletionTest {
    @Test
    public void testReverseOrder() {
        final AsyncCompletion queue = new AsyncCompletion.Builder(new Executor() {
            private final AtomicReference<Runnable> first = new AtomicReference<Runnable>();

            public void execute(final Runnable command) {
                if (first.get() == null) {
                    first.set(command);
                    return;
                }
                command.run();
                first.get().run();
            }
        }).build();
        final Iterator<Integer> queued = queue.invokeAll(ImmutableList.of(callable(1), callable(2))).iterator();
        assertEquals(2, queued.next().intValue());
        assertEquals(1, queued.next().intValue());
        assertFalse(queued.hasNext());
    }

    @Test
    public void testOrder() {
        final AsyncCompletion completion = new AsyncCompletion.Builder(new Executor() {
            public void execute(final Runnable command) {
                command.run();
            }
        }).build();
        final Iterator<Integer> queued = completion.invokeAll(ImmutableList.of(callable(1), callable(2))).iterator();
        assertEquals(1, queued.next().intValue());
        assertEquals(2, queued.next().intValue());
        assertFalse(queued.hasNext());
    }

    @Test
    public void testSingleExecute() {
        final AtomicInteger count = new AtomicInteger();
        final AsyncCompletion completion = new AsyncCompletion.Builder(new Executor() {
            public void execute(final Runnable command) {
                count.getAndIncrement();
                command.run();
            }
        }).build();
        final Iterable<Integer> queued = completion.invokeAll(ImmutableList.of(callable(1)));
        assertEquals(1, queued.iterator().next().intValue());
        assertEquals(1, queued.iterator().next().intValue());
        assertEquals(1, queued.iterator().next().intValue());
        assertEquals(1, count.get());
    }

    @Test
    public void testNullLastFiltered() {
        final AsyncCompletion completion = new AsyncCompletion.Builder(new Executor() {
            public void execute(final Runnable command) {
                command.run();
            }
        }).build();
        final ImmutableList<Callable<Integer>> input = ImmutableList.of(callable(1), callable((Integer) null));
        final Iterator<Integer> queued = completion.invokeAll(input).iterator();
        assertEquals(1, queued.next().intValue());
        assertFalse(queued.hasNext());
    }

    @Test
    public void testNullFirstFiltered() {
        final AsyncCompletion completion = new AsyncCompletion.Builder(new Executor() {
            public void execute(final Runnable command) {
                command.run();
            }
        }).build();
        final ImmutableList<Callable<Integer>> input = ImmutableList.of(callable((Integer) null), callable(2));
        final Iterator<Integer> queued = completion.invokeAll(input).iterator();
        assertEquals(2, queued.next().intValue());
        assertFalse(queued.hasNext());
    }

    @Test
    public void testLimitedExecute() {
        final List<Runnable> jobs = Lists.newArrayList();
        final AsyncCompletion completion = new AsyncCompletion.Builder(new Executor() {
            public void execute(final Runnable command) {
                jobs.add(command);
            }
        }).handleExceptions(Exceptions.THROW).limitParallelExecutionTo(1);
        final Iterable<Integer> queued = completion.invokeAll(ImmutableList.of(callable(1), callable(2), callable(3)));

        final Iterator<Integer> iterator = queued.iterator();
        assertEquals(1, jobs.size());
        // can't test that hasNext() will block, but it should
        jobs.get(0).run();
        assertEquals(2, jobs.size());
        // can test that next() will not block anymore
        assertEquals(1, iterator.next().intValue());

        jobs.get(1).run();
        assertEquals(3, jobs.size());
        assertEquals(2, iterator.next().intValue());
        jobs.get(2).run();
        assertEquals(3, jobs.size());
        assertEquals(3, iterator.next().intValue());
        assertFalse(iterator.hasNext());
    }

    <T> Callable<T> callable(final T input) {
        return new Callable<T>() {
            public T call() throws Exception {
                return input;
            }
        };
    }
}
