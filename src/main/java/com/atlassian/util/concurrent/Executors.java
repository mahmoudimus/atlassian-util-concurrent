package com.atlassian.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import net.javacrumbs.completionstage.CompletableCompletionStage;
import net.javacrumbs.completionstage.CompletionStageFactory;

import javax.annotation.Nonnull;

public final class Executors {
  /**
   * {@link Executor} that limits the number submitted jobs to a fixed upper
   * bound, by blocking the producer thread on submission.
   * 
   * @since 2.6.1
   * @see LimitedExecutor for implementation details.
   */
  public static Executor limited(Executor delegate, int limit) {
    return new LimitedExecutor(delegate, limit);
  }

  /**
   * {@link Executor} that limits the number submitted jobs to a fixed upper
   * bound, by blocking the producer thread on submission.
   * 
   * @since 2.6.2
   * @see LimitedExecutor for implementation details.
   */
  public static ExecutorSubmitter submitter(Executor delegate) {
    return new DefaultSubmitter(delegate);
  }

  static class DefaultSubmitter implements ExecutorSubmitter {
    private final Executor executor;
    private final CompletionStageFactory completionStageFactory;

    DefaultSubmitter(final Executor executor) {
      this.executor = executor;
      this.completionStageFactory = new CompletionStageFactory(executor);
    }

    @Override public void execute(@Nonnull final Runnable command) {
      executor.execute(command);
    }

    @Override public <T> Promise<T> submit(final Callable<T> callable) {
      final CallableRunner<T> runner = new CallableRunner<T>(callable);
      executor.execute(runner);
      return runner.get();
    }

    @Override public <T> Promise<T> submitSupplier(final Supplier<T> supplier) {
      return submit(Suppliers.toCallable(supplier));
    }

    class CallableRunner<T> implements Runnable, Supplier<Promise<T>> {
      final Callable<T> task;
      final CompletableCompletionStage<T> future = completionStageFactory.createCompletionStage();

      CallableRunner(Callable<T> task) {
        this.task = task;
      }

      @Override public void run() {
        try {
          future.complete(task.call());
        } catch (Exception ex) {
          future.completeExceptionally(ex);
        }
      }

      @Override public Promise<T> get() {
        return Promises.forCompletionStage(future);
      }
    }
  }

  // /CLOVER:OFF
  private Executors() {
    throw new AssertionError("cannot instantiate!");
  }
  // /CLOVER:ON
}
