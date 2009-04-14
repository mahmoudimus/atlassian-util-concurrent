package com.atlassian.util.concurrent;

public class Functions {
    public static <T> Function<T, T> identity() {
        return new Identity<T>();
    }

    static class Identity<T> implements Function<T, T> {
        public T get(final T input) {
            return input;
        }
    }
}
