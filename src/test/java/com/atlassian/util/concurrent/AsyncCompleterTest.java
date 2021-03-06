package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.atlassian.util.concurrent.ExceptionPolicy.Policies;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncCompleterTest {
  @Test public void reverseOrder() {
    final AsyncCompleter queue = new AsyncCompleter.Builder(new Executor() {
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

  @Test public void order() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final Iterator<Integer> queued = completion.invokeAll(ImmutableList.of(callable(1), callable(2))).iterator();
    assertEquals(1, queued.next().intValue());
    assertEquals(2, queued.next().intValue());
    assertFalse(queued.hasNext());
  }

  @Test public void singleExecute() {
    final AtomicInteger count = new AtomicInteger();
    final AsyncCompleter completion = new AsyncCompleter.Builder(new Executor() {
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

  @Test public void nullLastFiltered() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final ImmutableList<Callable<Integer>> input = ImmutableList.of(callable(1), callable((Integer) null));
    final Iterator<Integer> queued = completion.invokeAll(input).iterator();
    assertEquals(1, queued.next().intValue());
    assertFalse(queued.hasNext());
  }

  @Test public void nullFirstFiltered() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final ImmutableList<Callable<Integer>> input = ImmutableList.of(callable((Integer) null), callable(2));
    final Iterator<Integer> queued = completion.invokeAll(input).iterator();
    assertEquals(2, queued.next().intValue());
    assertFalse(queued.hasNext());
  }

  @Test public void limitedExecute() {
    final List<Runnable> jobs = Lists.newArrayList();
    final AsyncCompleter completion = new AsyncCompleter.Builder(new Executor() {
      public void execute(final Runnable command) {
        jobs.add(command);
      }
    }).handleExceptions(Policies.THROW).limitParallelExecutionTo(1);
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

  @Test public void callableCompletedBeforeTimeout() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final ImmutableList<Callable<Integer>> input = ImmutableList.of(sleeper(1, 2));
    final Integer value = completion.invokeAll(input, 1, TimeUnit.NANOSECONDS).iterator().next();
    assertEquals(1, value.intValue());
  }

  @Test(expected = RuntimeTimeoutException.class) public void callableTimedOutBeforeCompleting() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new NaiveExecutor()).build();
    // should reach timeout before completing
    completion.invokeAll(ImmutableList.of(sleeper(1, 10)), 1, TimeUnit.NANOSECONDS).iterator().next();
  }

  @Test public void invocationRegistersWithAccessor() throws Exception {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final AtomicReference<Future<String>> ref = new AtomicReference<Future<String>>();
    completion.invokeAllTasks(ImmutableList.of(callable("blah!")), new AsyncCompleter.Accessor<String>() {
      @Override public String apply(final CompletionService<String> input) {
        try {
          return input.poll().get();
        } catch (final Exception e) {
          throw new AssertionError(e);
        }
      }

      @Override public void register(final Future<String> f) {
        assertTrue(ref.compareAndSet(null, f));
      }
    });
    assertEquals("blah!", ref.get().get());
  }

  <T> Callable<T> callable(final T input) {
    return new Callable<T>() {
      public T call() throws Exception {
        return input;
      }
    };
  }

  <T> Callable<T> sleeper(final T input, final int sleep) {
    return new Callable<T>() {
      public T call() throws Exception {
        Thread.sleep(sleep);
        return input;
      }
    };
  }
}
