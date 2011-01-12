package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.notNull;

import com.google.common.base.Supplier;

public final class Functions {
    /**
     * Get a function that uses the Supplier as a factory for all inputs.
     * 
     * @param <D> the key type, ignored
     * @param <R> the result type
     * @param supplier called for all inputs
     * @return the function
     */
    public static <D, R> Function<D, R> fromSupplier(final @NotNull Supplier<R> supplier) {
        return new FromSupplier<D, R>(supplier);
    }

    private static class FromSupplier<D, R> implements Function<D, R> {
        private final Supplier<R> supplier;

        FromSupplier(final Supplier<R> supplier) {
            this.supplier = notNull("supplier", supplier);
        }

        public R apply(final D input) {
            return supplier.get();
        }
    };

    /**
     * Get the value from a supplier.
     * 
     * @param <T> the type returned, note the Supplier can be covariant.
     * @return a function that extracts the value from a supplier
     */
    static <T> Function<Supplier<? extends T>, T> fromSupplier() {
        return new ValueExtractor<T>();
    }

    private static class ValueExtractor<T> implements Function<Supplier<? extends T>, T> {
        public T apply(final Supplier<? extends T> supplier) {
            return supplier.get();
        }
    }

    /**
     * Get a function that always returns the input.
     * 
     * @param <T> the type of the input and the output for the function.
     * @return the identity function.
     */
    public static <T> Function<T, T> identity() {
        return new Identity<T>();
    }

    private static class Identity<T> implements Function<T, T> {
        public T apply(final T input) {
            return input;
        }
    }

    /**
     * Function that can be used to ignore any RuntimeExceptions that a
     * {@link Supplier} may produce and return null instead.
     * 
     * @param <T> the result type
     * @return a Function that transforms an exception into a null
     */
    static <T> Function<Supplier<T>, Supplier<T>> ignoreExceptions() {
        return new ExceptionIgnorer<T>();
    }

    static class ExceptionIgnorer<T> implements Function<Supplier<T>, Supplier<T>> {
        public Supplier<T> apply(final Supplier<T> from) {
            return new IgnoreAndReturnNull<T>(from);
        }
    }

    static class IgnoreAndReturnNull<T> implements Supplier<T> {
        private final Supplier<T> delegate;

        IgnoreAndReturnNull(final Supplier<T> delegate) {
            this.delegate = notNull("delegate", delegate);
        }

        public T get() {
            try {
                return delegate.get();
            } catch (final RuntimeException ignore) {
                return null;
            }
        }
    }

    // /CLOVER:OFF
    private Functions() {}
    // /CLOVER:ON
}
