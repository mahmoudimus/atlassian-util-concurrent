package io.atlassian.util.concurrent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Helper methods for working with completion stages
 */
@ParametersAreNonnullByDefault public final class CompletionStages {

  private CompletionStages() {}

  /**
   * Return a completion stage that is already failed with the supplied
   * throwable
   *
   * @param throwable The Throwable to completeExceptionally the CompletionStage
   * @param <T> The type of the CompletionStage
   * @return A CompletionStage which has completed exceptionally with the
   * supplied throwable
   */
  public static <T> CompletionStage<T> fail(final Throwable throwable) {
    CompletableFuture<T> future = new CompletableFuture();
    future.completeExceptionally(throwable);
    return future;
  }

  /**
   * Block and retrieve the value from a {@link CompletionStage} or handle the
   * associated error. In general you should avoid calling this method unless
   * you absolutely have to force a value from the CompletionStage as it will
   * block.
   *
   * @param completionStage The completionStage that holds the value
   * @param onError Function to be invoked if the forcing of the CompletionStage
   * fails
   * @param <T> The type held inside the CompletionState
   * @return The value in the completion stage if the CompletionStage succeeds
   * or the result of passing the error to onError
   */
  public static <T> T unsafeBlockAndGet(final CompletionStage<T> completionStage, final Function<Throwable, ? extends T> onError) {
    try {
      return completionStage.toCompletableFuture().get();
    } catch (Throwable throwable) {
      return onError.apply(throwable);
    }
  }

  /**
   * Block and retrieve the value from a {@link CompletionStage} or handle the
   * associated error. In general you should avoid calling this method unless
   * you absolutely have to force a value from the CompletionStage as it will
   * block.
   *
   * @param <T> The type held inside the CompletionState
   * @param completionStage The completionStage that holds the value
   * @param timeout How long to wait when retrieving the future value
   * @param onError Function to be invoked if the forcing of the CompletionStage
   * fails
   * @return The value in the completion stage if the CompletionStage succeeds
   * or the result of passing the error to onError
   */
  public static <T> T unsafeBlockAndGet(final CompletionStage<T> completionStage, final Timeout timeout,
    final Function<Throwable, ? extends T> onError) {
    try {
      return completionStage.toCompletableFuture().get(timeout.getTimeoutPeriod(), timeout.getUnit());
    } catch (Throwable throwable) {
      return onError.apply(throwable);
    }
  }

  /**
   * An error handling function that will just rethrow the exception
   *
   * @param onError Function that will map a Throwable to a RuntimeException
   * @param <T> The return type for the error handling function
   * @return A Function that consumes a Throwable and throws it rather than
   * returning a T
   * @throws RuntimeException created from the onError function
   */
  public static <T> Function<Throwable, T> rethrow(Function<Throwable, ? extends RuntimeException> onError) {
    return (Throwable throwable) -> {
      throw onError.apply(throwable);
    };
  }
}
