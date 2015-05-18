package com.atlassian.util.concurrent;

import static java.lang.Integer.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.function.Supplier;

public class LazyTest {
  @Test public void strong() throws Exception {
    final Supplier<Integer> s = Lazy.supplier(new Counter());
    assertSame(Lazy.Strong.class, s.getClass());
    assertEquals(valueOf(1), s.get());
  }

  @Test public void ttl() throws Exception {
    final Supplier<Integer> s = Lazy.timeToLive(new Counter(), 1L, SECONDS);
    assertSame(Expiring.class, s.getClass());
    assertEquals(valueOf(1), s.get());
  }

  @Test public void tti() throws Exception {
    final Supplier<Integer> s = Lazy.timeToIdle(new Counter(), 1L, SECONDS);
    assertSame(Expiring.class, s.getClass());
    assertEquals(valueOf(1), s.get());
  }
}
