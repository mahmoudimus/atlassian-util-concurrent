package io.atlassian.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * Adds the {@link java.util.concurrent.ExecutorService} job submission methods without exposing the
 * life-cycle management API. Is tweaked to return {@link Promise promises}
 * rather than anemic {@link Future futures}.
 *
 * @since 2.6.2
 */
public interface ExecutorSubmitter extends Executor {
  /**
   * Submits a value-returning task for execution and returns a Promise
   * representing the pending results of the task.
   *
   * @param task the task to submit
   * @return a Promise representing pending completion of the task
   * @throws RejectedExecutionException if the task cannot be scheduled for
   * execution
   * @throws java.lang.NullPointerException if the task is null
   * @param <T> a T to return.
   */
  <T> Promise<T> submit(Callable<T> task);

  /**
   * Submits a value-returning task for execution and returns a Promise
   * representing the pending results of the task.
   *
   * @param task the task to submit
   * @return a Promise representing pending completion of the task
   * @throws RejectedExecutionException if the task cannot be scheduled for
   * execution
   * @throws java.lang.NullPointerException if the task is null
   * @param <T> a T to return.
   */
  <T> Promise<T> submitSupplier(Supplier<T> task);
}
