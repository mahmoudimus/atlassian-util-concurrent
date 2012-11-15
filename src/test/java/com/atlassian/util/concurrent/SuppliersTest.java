package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class SuppliersTest {
  @Test public void memoize() {
    final Supplier<String> memoized = Suppliers.memoize("testing");
    assertSame("testing", memoized.get());
    assertSame("testing", memoized.get());
  }

  @Test public void fromFunction() {
    final AtomicInteger count = new AtomicInteger();
    final Function<Integer, Integer> function = new Function<Integer, Integer>() {
      public Integer get(final Integer input) {
        assertSame(1, input);
        return count.incrementAndGet();
      }
    };
    final Supplier<Integer> counter = Suppliers.fromFunction(1, function);
    assertEquals(Integer.valueOf(1), counter.get());
    assertEquals(Integer.valueOf(2), counter.get());
    assertEquals(Integer.valueOf(3), counter.get());
    assertEquals(Integer.valueOf(4), counter.get());
  }
}
