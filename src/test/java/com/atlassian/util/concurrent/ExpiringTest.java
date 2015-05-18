package com.atlassian.util.concurrent;

import static java.lang.Integer.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class ExpiringTest {
  @Test public void expiring() {
    final Counter counter = new Counter();

    final Supplier<Integer> e = new Expiring<Integer>(counter, () -> new Predicate<Void>() {
      boolean once = true; // first time true

      @Override public boolean test(final Void input) {
        try {
          return once;
        } finally {
          once = false;
        }
      }
    });
    assertEquals(0, counter.count.get());
    assertEquals(valueOf(1), e.get());
    assertEquals(1, counter.count.get());
    assertEquals(valueOf(2), e.get());
    assertEquals(2, counter.count.get());
  }

  @Test public void notExpiring() {
    final Counter counter = new Counter();

    final Supplier<Integer> e = new Expiring<>(counter, () -> x -> true);
    assertEquals(0, counter.count.get());
    assertEquals(valueOf(1), e.get());
    assertEquals(1, counter.count.get());
    assertEquals(valueOf(1), e.get());
    assertEquals(1, counter.count.get());
    assertEquals(valueOf(1), e.get());
  }

  @Test(expected = AssertionError.class) public void detectsProgramErrorInfiniteLoopProtection() {
    Integer integer = new Expiring<>(new Counter(), () -> x -> false).get();
  }

  @Test(expected = UnsupportedOperationException.class) public void deadGet() {
    Expiring.Dead.DEAD.get();
  }

  @Test public void deadReallyIs() {
    assertFalse(Expiring.Dead.DEAD.alive());
  }
}
