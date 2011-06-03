package com.atlassian.util.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import static com.google.common.base.Functions.identity;

/**
 * Represents an exception handling policy.
 * Default implementations can be found in {@link Policies}.
 */
public interface ExceptionPolicy {
    <T> Function<Supplier<T>, Supplier<T>> handler();

    /**
     * Default exception handling policies
     */
    public enum Policies implements ExceptionPolicy {
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
