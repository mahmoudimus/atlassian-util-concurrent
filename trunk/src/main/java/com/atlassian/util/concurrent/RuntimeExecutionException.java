package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.notNull;

import java.util.concurrent.ExecutionException;

/**
 * Convenience class for re-throwing {@link ExecutionException}.
 */
public class RuntimeExecutionException extends RuntimeException {
    public RuntimeExecutionException(final ExecutionException cause) {
        super(notNull("cause", cause));
    }

    public RuntimeExecutionException(final String message, final ExecutionException cause) {
        super(message, notNull("cause", cause));
    }
}