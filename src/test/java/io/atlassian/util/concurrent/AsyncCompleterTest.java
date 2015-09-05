package io.atlassian.util.concurrent;

import static io.atlassian.util.concurrent.Promises.forFuture;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.atlassian.util.concurrent.ExceptionPolicy.Policies;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
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
    final Iterator<Integer> queued = queue.invokeAll(asList(callable(1), callable(2))).iterator();
    assertEquals(2, queued.next().intValue());
    assertEquals(1, queued.next().intValue());
    assertFalse(queued.hasNext());
  }

  @Test public void order() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final Iterator<Integer> queued = completion.invokeAll(asList(callable(1), callable(2))).iterator();
    assertEquals(1, queued.next().intValue());
    assertEquals(2, queued.next().intValue());
    assertFalse(queued.hasNext());
  }

  @Test public void singleExecute() {
    final AtomicInteger count = new AtomicInteger();
    final AsyncCompleter completion = new AsyncCompleter.Builder(command -> {
      count.getAndIncrement();
      command.run();
    }).build();
    final Iterable<Integer> queued = completion.invokeAll(asList(callable(1)));
    assertEquals(1, queued.iterator().next().intValue());
    assertEquals(1, queued.iterator().next().intValue());
    assertEquals(1, queued.iterator().next().intValue());
    assertEquals(1, count.get());
  }

  @Test public void nullLastFiltered() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final List<Callable<Integer>> input = asList(callable(1), callable((Integer) null));
    final Iterator<Integer> queued = completion.invokeAll(input).iterator();
    assertEquals(1, queued.next().intValue());
    assertFalse(queued.hasNext());
  }

  @Test public void nullFirstFiltered() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final List<Callable<Integer>> input = asList(callable((Integer) null), callable(2));
    final Iterator<Integer> queued = completion.invokeAll(input).iterator();
    assertEquals(2, queued.next().intValue());
    assertFalse(queued.hasNext());
  }

  @Test public void limitedExecute() {
    final List<Runnable> jobs = new ArrayList<>();
    final AsyncCompleter completion = new AsyncCompleter.Builder(jobs::add).handleExceptions(Policies.THROW).limitParallelExecutionTo(1);
    final Iterable<Integer> queued = completion.invokeAll(asList(callable(1), callable(2), callable(3)));

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
    Callable<Integer> callable = () -> 1;
    assertEquals(1, new AsyncCompleter.Builder(new CallerExecutor()).build().invokeAll(asList(callable), 1, TimeUnit.NANOSECONDS).iterator().next().intValue());
  }

  @Test(expected = RuntimeTimeoutException.class) public void callableTimedOutBeforeCompleting() {
    final CountDownLatch latch = new CountDownLatch(1);
    try {
      new AsyncCompleter.Builder(new NaiveExecutor()).build().invokeAll(asList(() -> {
        latch.await();
        return 1;
      }), 1, TimeUnit.NANOSECONDS).iterator().next();
    } finally {
      latch.countDown();
    }
  }

  @Test public void invocationRegistersWithAccessor() throws Exception {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new CallerExecutor()).build();
    final AtomicReference<Future<String>> ref = new AtomicReference<Future<String>>();
    completion.invokeAllTasks(asList(callable("blah!")), new AsyncCompleter.Accessor<String>() {
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

  /*
   * Some background on this one - Streams was giving the AsyncCompletor a
   * CompletionService which broke this property:
   * 
   * Future a = completionService.submit(task)
   * a.equals(completionService.poll())
   * 
   * The Future returned by .poll was different than the one returned by
   * .submit, which meant that futures.remove(future) always failed This test is
   * to ensure that we complain bitterly inside atlassian-util-concurrent if
   * this happens
   */
  @Test(expected = IllegalArgumentException.class) public void errorWhenGivenBadCompletionService() {
    final AsyncCompleter completion = new AsyncCompleter.Builder(new NaiveExecutor())
      .completionServiceFactory(new CancellingCompletionServiceFactory()).checkCompletionServiceFutureIdentity().build();

    Iterator<Integer> queued = completion.invokeAll(asList(callable(1), callable(2)), 1, TimeUnit.MINUTES).iterator();
    assertEquals(1, queued.next().intValue());
    assertEquals(2, queued.next().intValue());
  }

  private static class CancellingCompletionServiceFactory implements AsyncCompleter.ExecutorCompletionServiceFactory {
    @Override public <T> java.util.function.Function<Executor, CompletionService<T>> create() {
      return BadCompletionService::new;
    }
  }

  private static class BadCompletionService<T> implements CompletionService<T> {

    final ExecutorCompletionService<T> delegate;
    final Executor executor;

    public BadCompletionService(final Executor e) {
      executor = e;
      delegate = new ExecutorCompletionService<T>(e);
    }

    @Override public Future<T> submit(final Callable<T> callable) {
      return forFuture(delegate.submit(callable), executor);
    }

    @Override public Future<T> submit(Runnable runnable, T result) {
      return forFuture(delegate.submit(runnable, result), executor);
    }

    @Override public Future<T> take() throws InterruptedException {
      return delegate.take();
    }

    @Override public Future<T> poll() {
      return delegate.poll();
    }

    @Override public Future<T> poll(final long l, final TimeUnit timeUnit) throws InterruptedException {
      return delegate.poll(l, timeUnit);
    }
  }

  <T> Callable<T> callable(final T input) {
    return () -> input;
  }

}
