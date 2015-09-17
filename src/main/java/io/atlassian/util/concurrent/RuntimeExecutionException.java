package io.atlassian.util.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutionException;

/**
 * Convenience class for re-throwing {@link ExecutionException}.
 */
public class RuntimeExecutionException extends RuntimeException {
  private static final long serialVersionUID = 1573022712345306212L;

  public RuntimeExecutionException(final ExecutionException cause) {
    super(requireNonNull(cause, "cause"));
  }

  public RuntimeExecutionException(final String message, final ExecutionException cause) {
    super(message, requireNonNull(cause, "cause"));
  }
}