package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class FunctionsTest {
    @Test
    public void fromSupplier() {
        final Supplier<Integer> supplier = new Supplier<Integer>() {
            int count = 0;

            public Integer get() {
                return count++;
            }
        };
        final Function<String, Integer> function = Functions.fromSupplier(supplier);
        assertEquals(Integer.valueOf(0), function.get("some"));
        assertEquals(Integer.valueOf(1), supplier.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromSupplierNotNull() {
        Functions.fromSupplier(null);
    }

    @Test
    public void identity() {
        final Function<String, String> function = Functions.identity();
        assertSame("same", function.get("same"));
    }
}
