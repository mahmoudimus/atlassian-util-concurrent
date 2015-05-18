package com.atlassian.util.concurrent;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class TimeToIdleTest {
  @Test public void timesOut() {
    final MockTimeSupplier supplier = new MockTimeSupplier(2, TimeUnit.NANOSECONDS);
    final Predicate<Void> ttl = new Lazy.TimeToIdle(Timeout.timeoutFactory(2, NANOSECONDS, supplier));
    assertTrue(ttl.test(null));
    assertTrue(ttl.test(null));
    // advance
    supplier.currentTime();
    for (int i = 0; i < 1000; i++) {
      assertFalse(String.valueOf(i), ttl.test(null));
    }
  }

  @Test public void resetsAfterSuccess() {
    final MockTimeSupplier supplier = new MockTimeSupplier(2, TimeUnit.NANOSECONDS);
    final Predicate<Void> ttl = new Lazy.TimeToIdle(Timeout.timeoutFactory(2, NANOSECONDS, supplier));
    for (int i = 0; i < 1000; i++) {
      assertTrue(String.valueOf(i), ttl.test(null));
    }
  }
}
