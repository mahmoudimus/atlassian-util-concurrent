package com.atlassian.util.concurrent.atomic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AtomicLongTest {

    @Test
    public void getAndSetLongIfNormal() {
        final long from = 1;
        final long to = 2;
        final AtomicLong ref = new AtomicLong(from);
        assertEquals(to, ref.getOrSetAndGetIf(from, to));
    }

    @Test
    public void getAndSetLongReturnsOldValueIfChanged() {
        final long old = 1;
        final long from = 2;
        final long to = 3;
        final AtomicLong ref = new AtomicLong(old);
        assertEquals(old, ref.getOrSetAndGetIf(from, to));
    }

    @Test
    public void getAndSetLongSameValue() {
        final long from = 1;
        final long to = from;
        final AtomicLong ref = new AtomicLong(from);
        assertEquals(to, ref.getOrSetAndGetIf(from, to));
    }

    @Test
    public void getAndSetLongSameValueDifferent() {
        final long from = 1;
        final long to = from;
        final int different = 3;
        final AtomicLong ref = new AtomicLong(different);
        assertEquals(different, ref.getOrSetAndGetIf(from, to));
    }
}
