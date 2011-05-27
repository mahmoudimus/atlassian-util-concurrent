package com.atlassian.util.concurrent;

import static java.lang.Integer.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class StrongTest {
    @Test
    public void lazilyPopulated() throws Exception {
        final Counter counter = new Counter();
        final Supplier<Integer> lazy = new Lazy.Strong<Integer>(counter);
        assertEquals(0, counter.count.get());
        assertEquals(valueOf(1), lazy.get());
        assertEquals(1, counter.count.get());
    }

    @Test
    public void memoized() throws Exception {
        final Counter counter = new Counter();
        final Supplier<Integer> lazy = new Lazy.Strong<Integer>(counter);
        assertEquals(valueOf(1), lazy.get());
        assertEquals(1, counter.count.get());
        assertEquals(valueOf(1), lazy.get());
        assertEquals(1, counter.count.get());
    }

    @Test
    public void supplierIsGarbageCollectible() throws Exception {
        final Lazy.Strong<Integer> lazy = new Lazy.Strong<Integer>(new Counter());
        assertEquals(valueOf(1), lazy.get());
        assertNull(lazy.supplier);
    }
}
