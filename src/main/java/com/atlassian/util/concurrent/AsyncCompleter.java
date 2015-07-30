/**
 * Copyright 2011 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Executors.limited;
import static com.atlassian.util.concurrent.Suppliers.memoize;
import static com.atlassian.util.concurrent.Timeout.getNanosTimeout;
import static java.util.Objects.requireNonNull;

import com.atlassian.util.concurrent.ExceptionPolicy.Policies;

import net.jcip.annotations.ThreadSafe;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Convenient encapsulation of {@link CompletionService} usage that allows a
 * collection of jobs to be issued to an {@link Executor} and return an
 * {@link Iterable} of the results that is in the order that the results return.
 * <p>
 * Unlike {@link ExecutorService#invokeAll(java.util.Collection)}
 * {@link #invokeAll(Iterable)} here does not itself block, rather the
 * {@link Iterator#next()} calls to the returned {@link Iterable} will block the
 * first time it is iterated. This allows the client to defer the reification of
 * the result until it is ready to use it.
 * <p>
 * To create an instance of this class, please use the supplied {@link Builder}.
 * 
 * @since 1.0
 */
@ThreadSafe public final class AsyncCompleter {
  private final Executor executor;
  private final ExceptionPolicy policy;
  private final ExecutorCompletionServiceFactory completionServiceFactory;
  private final CompletionServiceDecorator completionServiceDecorator;

  AsyncCompleter(final Executor executor, final ExceptionPolicy policy, final ExecutorCompletionServiceFactory completionServiceFactory,
    CompletionServiceDecorator completionServiceDecorator) {
    this.executor = requireNonNull(executor, "executor");
    this.policy = requireNonNull(policy, "policy");
    this.completionServiceFactory = requireNonNull(completionServiceFactory, "completionServiceFactory");
    this.completionServiceDecorator = completionServiceDecorator;
  }

  /**
   * Queue the {@link Callable jobs} on the contained {@link Executor} and
   * return a lazily evaluated {@link Iterable} of the results in the order they
   * return in (fastest first).
   * <p>
   * Note that if any of the jobs return null then nulls WILL BE included in the
   * results. Similarly if an exception is thrown and exceptions are being
   * ignored then there will be a NULL result returned. If you want to filter
   * nulls this is trivial, but be aware that filtering of the results forces
   * {@link Iterator#next()} to be called while calling
   * {@link Iterator#hasNext()} (which may block).
   * 
   * @param <T> the result type
   * @param callables the jobs to run
   * @return an Iterable that returns the results in the order in which they
   * return, excluding any null values.
   */
  public <T> Iterable<T> invokeAll(final Iterable<? extends Callable<T>> callables) {
    return invokeAllTasks(callables, new BlockingAccessor<T>());
  }

  /**
   * Version of {@link #invokeAll(Iterable)} that supports a timeout. Any jobs
   * that are not complete by the timeout are interrupted and discarded.
   * 
   * @param <T> the result type
   * @param callables the jobs to run
   * @param time the max time spent per job specified by:
   * @param unit the TimeUnit time is specified in
   * @return an Iterable that returns the results in the order in which they
   * return, excluding any null values.
   * 
   * @see #invokeAll(Iterable)
   * @since 2.1
   */
  public <T> Iterable<T> invokeAll(final Iterable<? extends Callable<T>> callables, final long time, final TimeUnit unit) {
    return invokeAllTasks(callables, new TimeoutAccessor<T>(getNanosTimeout(time, unit)));
  }

  /**
   * Implementation for the invokeAll methods, needs to be passed an accessor
   * function that is responsible for getting things from the CompletionService.
   */
  <T> Iterable<T> invokeAllTasks(final Iterable<? extends Callable<T>> callables, final Accessor<T> accessor) {
    final CompletionService<T> apply = completionServiceDecorator.apply(completionServiceFactory.<T> create().apply(executor));
    // we must copy the resulting Iterable<Supplier> so
    // each iteration doesn't resubmit the jobs
    List<Supplier<T>> lazyAsyncSuppliers = StreamSupport.stream(callables.spliterator(), false).
      map(new AsyncCompletionFunction<>(apply, accessor)::apply).collect(Collectors.toList());

    return lazyAsyncSuppliers.stream().
            map(policy.<T> handler()::apply).
            map(Functions.<T> fromSupplier()::apply).
            filter(x -> x != null).collect(Collectors.toList());
  }

  /**
   * For creating instances of {@link AsyncCompleter}.
   */
  public static class Builder {
    Executor executor;
    ExceptionPolicy policy = Policies.THROW;
    ExecutorCompletionServiceFactory completionServiceFactory = new DefaultExecutorCompletionServiceFactory();
    CompletionServiceDecorator completionServiceDecorator = CompletionServiceDecorator.Identity.INSTANCE;

    /**
     * Create a Builder with the supplied Executor
     * 
     * @param executor
     */
    public Builder(@NotNull final Executor executor) {
      this.executor = requireNonNull(executor, "executor");
    }

    /**
     * Ignore exceptions thrown, note will cause nulls in the resulting iterable!
     */
    public Builder ignoreExceptions() {
      return handleExceptions(Policies.IGNORE_EXCEPTIONS);
    }

    public Builder handleExceptions(final ExceptionPolicy policy) {
      this.policy = policy;
      return this;
    }

    public Builder completionServiceFactory(final ExecutorCompletionServiceFactory completionServiceFactory) {
      this.completionServiceFactory = requireNonNull(completionServiceFactory, "completionServiceFactory");
      return this;
    }

    public Builder checkCompletionServiceFutureIdentity() {
      this.completionServiceDecorator = new CompletionServiceDecorator.IdentityChecker();
      return this;
    }

    /**
     * Create a {@link AsyncCompleter} that limits the number of jobs submitted
     * to the underlying executor to a hard limit.
     * <p>
     * Note: this only makes sense if the underlying executor does not have a
     * limit on the number of threads it will create, on the size of the queue
     * submitted jobs will live in, or where the limit is much higher than this
     * limit.
     * 
     * @param limit the number of parallel jobs to execute at any one time
     * @see LimitedExecutor for more discussion of how this limit is relevant
     */
    public AsyncCompleter limitParallelExecutionTo(final int limit) {
      return new AsyncCompleter(limited(executor, limit), policy, completionServiceFactory, completionServiceDecorator);
    }

    public AsyncCompleter build() {
      return new AsyncCompleter(executor, policy, completionServiceFactory, completionServiceDecorator);
    }
  }

  /**
   * Extension point if a custom CompletionService is required, for instance to
   * implement a custom concellation policy.
   */
  public interface ExecutorCompletionServiceFactory {
    <T> Function<Executor, CompletionService<T>> create();
  }

  /**
   * Function for submitting {@link Callable} instances to an executor and
   * asynchronously waiting for the first result. Instances of this should not
   * be shared as each has its own {@link CompletionService} instance (and
   * therefore its own queue) so anything subsequently submitted to this
   * Function may end up as the result of the supplier.
   * 
   * @param <T> the result type.
   */
  private static class AsyncCompletionFunction<T> implements Function<Callable<T>, Supplier<T>> {
    private final CompletionService<T> completionService;
    private final Accessor<T> accessor;

    // the result gets memoized, so we only need one
    private final Supplier<T> nextCompleteItem = new Supplier<T>() {
      public T get() {
        return accessor.apply(completionService);
      }
    };

    AsyncCompletionFunction(final CompletionService<T> completionService, final Accessor<T> accessor) {
      this.completionService = completionService;
      this.accessor = accessor;
    }

    public Supplier<T> apply(final Callable<T> task) {
      accessor.register(completionService.submit(task));
      // never call get twice as it gets a new element from the queue
      return memoize(nextCompleteItem::get);
    }
  }

  /**
   * Responsible for accessing the next complete item and maintaining the
   * registered futures in case of timeouts.
   */
  interface Accessor<T> extends Function<CompletionService<T>, T> {
    /** Register a Future, may be interesting later */
    void register(Future<T> f);
  }

  static final class TimeoutAccessor<T> implements Accessor<T> {
    private final Timeout timeout;
    private final Collection<Future<T>> futures = new ConcurrentLinkedQueue<Future<T>>();

    TimeoutAccessor(final Timeout timeout) {
      this.timeout = timeout;
    }

    @Override public T apply(final CompletionService<T> completionService) {
      try {
        final Future<T> future = completionService.poll(timeout.getTime(), timeout.getUnit());
        if (future == null) {
          cancelRemaining();
          throw timeout.getTimeoutException();
        }
        futures.remove(future);
        return future.get();
      } catch (final InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      } catch (final ExecutionException e) {
        throw new RuntimeExecutionException(e);
      }
    }

    @Override public void register(final Future<T> f) {
      futures.add(f);
    }

    private void cancelRemaining() {
      for (final Future<T> f : futures) {
        f.cancel(true);
      }
      futures.clear();
    }
  }

  static final class BlockingAccessor<T> implements Accessor<T> {
    @Override public T apply(final CompletionService<T> completionService) {
      try {
        return completionService.take().get();
      } catch (final InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      } catch (final ExecutionException e) {
        throw new RuntimeExecutionException(e);
      }
    }

    @Override public void register(final Future<T> f) {}
  }

  static final class DefaultExecutorCompletionServiceFactory implements ExecutorCompletionServiceFactory {
    public <T> Function<Executor, CompletionService<T>> create() {
      return new ExecutorCompletionServiceFunction<T>();
    }
  }

  static final class ExecutorCompletionServiceFunction<T> implements Function<Executor, CompletionService<T>> {
    public CompletionService<T> apply(final Executor executor) {
      return new ExecutorCompletionService<T>(executor);
    }
  }

  interface CompletionServiceDecorator {
    <T> CompletionService<T> apply(CompletionService<T> acc);

    static enum Identity implements CompletionServiceDecorator {
      INSTANCE;
      @Override public <T> CompletionService<T> apply(CompletionService<T> acc) {
        return acc;
      }
    }

    /**
     * Used to assert that the CompletionService returns identical Futures from
     * submit and take/poll
     */
    class IdentityChecker implements CompletionServiceDecorator {
      @Override public <T> CompletionService<T> apply(CompletionService<T> delegate) {
        return new IdentityCheckedCompletionService<T>(delegate);
      }
    }
  }

  /**
   * Checks that any Future returned equals one that was submitted through here.
   * <p>
   * Note that this guarantee is not strictly supported by the interface, but it
   * may be desirable to check that the delegate preserves identity.
   */
  static class IdentityCheckedCompletionService<T> implements CompletionService<T> {
    private final CompletionService<T> delegate;
    private final Collection<Future<T>> futures = new ConcurrentLinkedQueue<Future<T>>();

    IdentityCheckedCompletionService(CompletionService<T> delegate) {
      this.delegate = delegate;
    }

    Future<T> add(Future<T> f) {
      futures.add(f);
      return f;
    }

    Future<T> check(Future<T> f) {
      if(!futures.remove(f)){
        throw new IllegalArgumentException("Expected the future to be in the list of registered futures");
      }
      return f;
    }

    public Future<T> submit(Callable<T> task) {
      return add(delegate.submit(task));
    }

    public Future<T> submit(Runnable task, T result) {
      return add(delegate.submit(task, result));
    }

    public Future<T> take() throws InterruptedException {
      return check(delegate.take());
    }

    public Future<T> poll() {
      return check(delegate.poll());
    }

    public Future<T> poll(long timeout, TimeUnit unit) throws InterruptedException {
      return check(delegate.poll(timeout, unit));
    }
  }
}
