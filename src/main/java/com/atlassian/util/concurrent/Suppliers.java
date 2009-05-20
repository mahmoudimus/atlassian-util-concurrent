package com.atlassian.util.concurrent;

/**
 * Useful {@link Supplier} implementations.
 */
public final class Suppliers {
    /**
     * A {@link Supplier} that always returns the supplied source.
     * 
     * @param <T> the type
     * @param source the object that is always returned.
     * @return a supplier that always returns the supplied argument
     */
    public static <T> Supplier<T> memoize(final T source) {
        return new Supplier<T>() {
            public T get() {
                return source;
            }
        };
    }

    /**
     * A {@link Supplier} that asks the argument function for the result using
     * the input argument.
     * 
     * @param <D> the input type
     * @param <T> the result type
     * @param input used as the argument when calling the function.
     * @param function asked to get the result.
     * @return the result
     */
    public static <D, T> Supplier<T> fromFunction(final D input, final Function<D, T> function) {
        return new Supplier<T>() {
            public T get() {
                return function.get(input);
            }
        };
    }

    // /CLOVER:OFF
    private Suppliers() {
        throw new AssertionError("cannot instantiate!");
    }
    // /CLOVER:ON
}
