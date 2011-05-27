package com.atlassian.util.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Predicate;

import java.util.concurrent.TimeUnit;

public class TimeToLiveTest {
    @Test
    public void timesOut() {
        final Timeout timeout = new Timeout(3, TimeUnit.NANOSECONDS, new MockTimeSupplier(2, TimeUnit.NANOSECONDS));
        final Predicate<Void> ttl = new Lazy.TimeToLive(timeout);
        assertTrue(ttl.apply(null));
        assertTrue(ttl.apply(null));
        assertFalse(ttl.apply(null));
    }

    @Test
    public void staysTimedOut() {
        final Timeout timeout = new Timeout(2, TimeUnit.NANOSECONDS, new MockTimeSupplier(2, TimeUnit.NANOSECONDS));
        final Predicate<Void> ttl = new Lazy.TimeToLive(timeout);
        assertTrue(ttl.apply(null));
        for (int i = 0; i < 1000; i++) {
            assertFalse(ttl.apply(null));
        }
    }
}
