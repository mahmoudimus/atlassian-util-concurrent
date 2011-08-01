package com.atlassian.util.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import static com.google.common.base.Functions.identity;

/**
 * Represents an exception handling policy.
 * Default implementations can be found in {@link Policies}.
 */
public interface ExceptionPolicy {
    
    /**
     * Handle a supplier which may or may not throw an Exception.
     *
     * @param <T> the return type of the Supplier
     * @return the ExceptionPolicy handler
     */
    <T> Function<Supplier<T>, Supplier<T>> handler();

    /**
     * Default exception handling policies
     */
    enum Policies implements ExceptionPolicy {
        IGNORE_EXCEPTIONS {
            public <T> Function<Supplier<T>, Supplier<T>> handler() {
                return Functions.<T> ignoreExceptions();
            }
        },
        THROW {
            public <T> Function<Supplier<T>, Supplier<T>> handler() {
                return identity();
            }
        };
    }
}
