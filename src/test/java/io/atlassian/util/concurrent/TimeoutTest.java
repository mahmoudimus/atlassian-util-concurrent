package io.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutTest {
  @Test public void timeInNanoseconds() {
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS, new MockTimeSupplier(1000, TimeUnit.NANOSECONDS));
    assertEquals(999999999L, timeout.getTime());
    assertEquals(TimeUnit.NANOSECONDS, timeout.getUnit());
    assertEquals(999999998L, timeout.getTime());
    assertEquals(TimeUnit.NANOSECONDS, timeout.getUnit());
  }

  @Test public void timeInMicroseconds() {
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS, new MockTimeSupplier(1000, TimeUnit.MICROSECONDS));
    assertEquals(999999L, timeout.getTime());
    assertEquals(TimeUnit.MICROSECONDS, timeout.getUnit());
    assertEquals(999998L, timeout.getTime());
    assertEquals(TimeUnit.MICROSECONDS, timeout.getUnit());
  }

  @Test public void timeInMilliseconds() {
    final Timeout timeout = new Timeout(1, TimeUnit.SECONDS, new MockTimeSupplier(1000, TimeUnit.MILLISECONDS));
    assertEquals(999L, timeout.getTime());
    assertEquals(TimeUnit.MILLISECONDS, timeout.getUnit());
    assertEquals(998L, timeout.getTime());
    assertEquals(TimeUnit.MILLISECONDS, timeout.getUnit());
  }

  @Test public void hourInNanoseconds() {
    final Timeout timeout = new Timeout(3600, TimeUnit.SECONDS, new MockTimeSupplier(1000, TimeUnit.NANOSECONDS));
    assertEquals(3599999999999L, timeout.getTime());
    assertEquals(TimeUnit.NANOSECONDS, timeout.getUnit());
    assertEquals(3599999999998L, timeout.getTime());
    assertEquals(TimeUnit.NANOSECONDS, timeout.getUnit());
  }

  @Test public void hourIsExpired() {
    final Timeout timeout = new Timeout(3, TimeUnit.NANOSECONDS, new MockTimeSupplier(2, TimeUnit.NANOSECONDS));
    assertEquals(2, timeout.getTime());
    assertFalse(timeout.isExpired());
    assertEquals(TimeUnit.NANOSECONDS, timeout.getUnit());
    assertEquals(0L, timeout.getTime());
    assertEquals(TimeUnit.NANOSECONDS, timeout.getUnit());
    assertTrue(timeout.isExpired());
  }

  @Test public void millisFactory() {
    final Timeout timeout = Timeout.getMillisTimeout(2, TimeUnit.MILLISECONDS);
    assertFalse(timeout.isExpired());
    assertEquals(TimeUnit.MILLISECONDS, timeout.getUnit());
  }

  @Test public void timeoutPeriodMillis() {
    final Timeout timeout = Timeout.getMillisTimeout(2, TimeUnit.MILLISECONDS);
    assertEquals(2, timeout.getTimeoutPeriod());
  }

  @Test public void timeoutPeriodNanos() {
    final Timeout timeout = Timeout.getNanosTimeout(2, TimeUnit.MILLISECONDS);
    assertEquals(2000000, timeout.getTimeoutPeriod());
  }

  @Test(expected = TimedOutException.class) public void throwsTimeoutException() throws TimeoutException, InterruptedException {
    final Timeout timeout = Timeout.getMillisTimeout(0, TimeUnit.MILLISECONDS);
    timeout.await(new Awaitable() {
      public void await() throws InterruptedException {
        Thread.sleep(1);
      }

      public boolean await(final long time, final TimeUnit unit) throws InterruptedException {
        return false;
      }
    });
    assertEquals(TimeUnit.MILLISECONDS, timeout.getUnit());
  }
}
