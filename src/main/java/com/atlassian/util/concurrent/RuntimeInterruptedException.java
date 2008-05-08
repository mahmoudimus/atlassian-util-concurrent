package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.isNotNull;

/**
 * Convenience class for re-throwing {@link InterruptedException}. Sets the {@link Thread#interrupted()} flag to true.
 */
public class RuntimeInterruptedException extends RuntimeException {

    private static final long serialVersionUID = -5025209597479375477L;

    public RuntimeInterruptedException(final InterruptedException cause) {
        super(isNotNull("cause", cause));
        Thread.currentThread().interrupt();
    }

    public RuntimeInterruptedException(final String message, final InterruptedException cause) {
        super(message, isNotNull("cause", cause));
        Thread.currentThread().interrupt();
    }

    @Override
    public InterruptedException getCause() {
        return (InterruptedException) super.getCause();
    }
}