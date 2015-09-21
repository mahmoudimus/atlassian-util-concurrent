package io.atlassian.util.concurrent;

import io.atlassian.util.concurrent.atomic.AtomicReference;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

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

    DefaultSubmitter(final Executor executor) {
      this.executor = executor;
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

    static class CallableRunner<T> implements Runnable, Supplier<Promise<T>> {

      enum State { WAITING, RUNNING, FINISHED }

      final Callable<T> task;
      final Promises.SettablePromise<T> promise = Promises.settablePromise();
      final AtomicReference<State> state = new AtomicReference<>(State.WAITING);

      CallableRunner(Callable<T> taskToRun) {
        task = taskToRun;
        promise.fail(t -> {
          if (promise.isCancelled()) {
            state.set(State.FINISHED);
          }
        });
      }

      @Override public void run() {
        if (state.compareAndSet(State.WAITING, State.RUNNING)) {
          try {
            final T value = task.call();
            if (state.compareAndSet(State.RUNNING, State.FINISHED)) {
              promise.set(value);
            }
          } catch (Exception ex) {
            if (state.compareAndSet(State.RUNNING, State.FINISHED)) {
              promise.exception(ex);
            }
          }
        }
      }

      @Override public Promise<T> get() {
        return promise;
      }
    }

  }

  // /CLOVER:OFF
  private Executors() {
    throw new AssertionError("cannot instantiate!");
  }
  // /CLOVER:ON
}
