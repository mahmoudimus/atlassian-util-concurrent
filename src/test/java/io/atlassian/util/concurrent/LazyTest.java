package io.atlassian.util.concurrent;

import static java.lang.Integer.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;

public class LazyTest {
  @Test public void strong() throws Exception {
    final Supplier<Integer> s = Lazy.supplier(new Counter());
    //assertSame(Lazy.Strong.class, s.getClass());
    assertThat(valueOf(1), is(s.get()));
  }

  @Test public void ttl() throws Exception {
    final Supplier<Integer> s = Lazy.timeToLive(new Counter(), 1L, SECONDS);
    //assertSame(Expiring.class, s.getClass());
    assertThat(valueOf(1), is(s.get()));
  }

  @Test public void tti() throws Exception {
    final Supplier<Integer> s = Lazy.timeToIdle(new Counter(), 1L, SECONDS);
    //assertSame(Expiring.class, s.getClass());
    assertThat(valueOf(1), is(s.get()));
  }

  @Test public void getNotInterruptible() throws Exception {
    final Supplier<String> ref = Lazy.resettable(() -> "test!");
    Thread.currentThread().interrupt();
    assertThat(ref.get(), is("test!"));
    assertTrue(Thread.interrupted());
  }

  @Test public void resetReturnsPreviousValue() throws Exception {
    final AtomicInteger count = new AtomicInteger();
    final ResettableLazyReference<Integer> ref = Lazy.resettable(() -> count.getAndIncrement());
    assertThat(ref.get(), is(0));
    assertThat(ref.resets().get(), is(0));
    assertThat(ref.get(), is(1));
    assertThat(ref.resets().get(), is(1));
    assertThat(ref.get(), is(2));
    assertThat(ref.resets().get(), is(2));
    assertThat(ref.get(), is(3));
    assertThat(ref.resets().get(), is(3));
    assertThat(ref.get(), is(4));
    assertThat(ref.resets().get(), is(4));
    assertThat(ref.get(), is(5));
    assertThat(ref.resets().get(), is(5));
  }
}
