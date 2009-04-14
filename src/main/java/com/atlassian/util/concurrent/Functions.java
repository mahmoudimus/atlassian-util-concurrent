package com.atlassian.util.concurrent;

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
        return new Function<D, R>() {
            public R get(final D input) {
                return supplier.get();
            }
        };
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

    static class Identity<T> implements Function<T, T> {
        public T get(final T input) {
            return input;
        }
    }

    private Functions() {}
}
