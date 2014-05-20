package com.atlassian.util.concurrent;

import java.util.concurrent.Executor;

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

  // /CLOVER:OFF
  private Executors() {
    throw new AssertionError("cannot instantiate!");
  }
  // /CLOVER:ON
}
