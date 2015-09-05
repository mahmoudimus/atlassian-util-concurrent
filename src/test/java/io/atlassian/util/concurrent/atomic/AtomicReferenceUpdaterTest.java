package io.atlassian.util.concurrent.atomic;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceUpdaterTest {
  @Test public void normal() {
    final String from = "from";
    final String to = "to";
    final AtomicReference<String> ref = new AtomicReference<String>(from);
    final AtomicReferenceUpdater<String> updater = new AtomicReferenceUpdater<String>(ref) {
      public String apply(final String input) {
        assertSame(from, input);
        return to;
      }
    };
    assertSame(to, updater.update());
  }

}
