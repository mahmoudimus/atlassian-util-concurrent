package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.notNull;

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

        public R get(final D input) {
            return supplier.get();
        }
    };

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
        public T get(final T input) {
            return input;
        }
    }

    // /CLOVER:OFF
    private Functions() {}
    // /CLOVER:ON
}
