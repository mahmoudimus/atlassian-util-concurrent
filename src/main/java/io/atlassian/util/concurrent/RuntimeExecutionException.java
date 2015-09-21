package io.atlassian.util.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutionException;

/**
 * Convenience class for re-throwing {@link java.util.concurrent.ExecutionException}.
 */
public class RuntimeExecutionException extends RuntimeException {
  private static final long serialVersionUID = 1573022712345306212L;

  /**
   * Constructor for RuntimeExecutionException.
   *
   * @param cause a {@link java.util.concurrent.ExecutionException}.
   */
  public RuntimeExecutionException(final ExecutionException cause) {
    super(requireNonNull(cause, "cause"));
  }

  /**
   * Constructor for RuntimeExecutionException.
   *
   * @param message a {@link java.lang.String}.
   * @param cause a {@link java.util.concurrent.ExecutionException}.
   */
  public RuntimeExecutionException(final String message, final ExecutionException cause) {
    super(message, requireNonNull(cause, "cause"));
  }
}
