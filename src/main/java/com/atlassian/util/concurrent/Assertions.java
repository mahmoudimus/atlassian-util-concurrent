package com.atlassian.util.concurrent;

/**
 * Design by contract assertions.
 */
class Assertions {
    public static <T> T isNotNull(final String name, final T notNull) throws IllegalArgumentException {
        if (notNull == null) {
            throw new NullArgumentException(name);
        }
        return notNull;
    }

    private Assertions() {}

    static class NullArgumentException extends IllegalArgumentException {
        private static final long serialVersionUID = 6178592463723624585L;

        NullArgumentException(final String name) {
            super(name + " should not be null!");
        }
    }
}
