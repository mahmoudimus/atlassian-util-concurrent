package com.atlassian.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Integer supplier that counts how many times it was called and
 * returns the same
 */
class Counter implements Supplier<Integer> {
    final AtomicInteger count = new AtomicInteger();

    @Override
    public Integer get() {
        return count.incrementAndGet();
    }
}