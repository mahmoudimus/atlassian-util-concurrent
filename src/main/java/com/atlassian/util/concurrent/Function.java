package com.atlassian.util.concurrent;

public interface Function<A, B> {
    B get(A input);
}
