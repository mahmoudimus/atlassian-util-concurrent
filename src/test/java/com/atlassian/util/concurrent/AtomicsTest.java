package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Suppliers.memoize;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicsTest {
    @Test
    public void getAndSetIfNormal() {
        final String from = "from";
        final String to = "to";
        final AtomicReference<String> ref = new AtomicReference<String>(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, memoize(to)));
    }

    @Test
    public void getAndSetContended() {
        final String from = "from";
        final String to = "to";
        final String different = "different";
        final AtomicReference<String> ref = new AtomicReference<String>(from);
        assertEquals(different, Atomics.getAndSetIf(ref, from, new Supplier<String>() {
            public String get() {
                // being called, set the reference so the CAS fails
                ref.set(different);
                return to;
            }
        }));
    }

    @Test
    public void getAndSetReturnsOldValueIfChanged() {
        final String old = "old";
        final String from = "from";
        final String to = "to";
        final AtomicReference<String> ref = new AtomicReference<String>(old);
        assertEquals(old, Atomics.getAndSetIf(ref, from, memoize(to)));
    }
}
