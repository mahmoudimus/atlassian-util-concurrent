package com.atlassian.util.concurrent.atomic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AtomicIntegerTest {

    @Test
    public void getAndSetIntegerIfNormal() {
        final int from = 1;
        final int to = 2;
        final AtomicInteger ref = new AtomicInteger(from);
        assertEquals(to, ref.getOrSetAndGetIf(from, to));
    }

    @Test
    public void getAndSetIntegerReturnsOldValueIfChanged() {
        final int old = 1;
        final int from = 2;
        final int to = 3;
        final AtomicInteger ref = new AtomicInteger(old);
        assertEquals(old, ref.getOrSetAndGetIf(from, to));
    }

    @Test
    public void getAndSetIntegerSameValue() {
        final int from = 1;
        final int to = from;
        final AtomicInteger ref = new AtomicInteger(from);
        assertEquals(to, ref.getOrSetAndGetIf(from, to));
    }

    @Test
    public void getAndSetIntegerSameValueDifferent() {
        final int from = 1;
        final int to = from;
        final int different = 3;
        final AtomicInteger ref = new AtomicInteger(different);
        assertEquals(different, ref.getOrSetAndGetIf(from, to));
    }
}
