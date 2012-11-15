package com.atlassian.util.concurrent;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Predicate;

import java.util.concurrent.TimeUnit;

public class TimeToIdleTest {
  @Test public void timesOut() {
    final MockTimeSupplier supplier = new MockTimeSupplier(2, TimeUnit.NANOSECONDS);
    final Predicate<Void> ttl = new Lazy.TimeToIdle(Timeout.timeoutFactory(2, NANOSECONDS, supplier));
    assertTrue(ttl.apply(null));
    assertTrue(ttl.apply(null));
    // advance
    supplier.currentTime();
    for (int i = 0; i < 1000; i++) {
      assertFalse(String.valueOf(i), ttl.apply(null));
    }
  }

  @Test public void resetsAfterSuccess() {
    final MockTimeSupplier supplier = new MockTimeSupplier(2, TimeUnit.NANOSECONDS);
    final Predicate<Void> ttl = new Lazy.TimeToIdle(Timeout.timeoutFactory(2, NANOSECONDS, supplier));
    for (int i = 0; i < 1000; i++) {
      assertTrue(String.valueOf(i), ttl.apply(null));
    }
  }
}
