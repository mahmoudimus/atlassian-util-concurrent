package com.atlassian.util.concurrent;

import static java.lang.Thread.interrupted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class RuntimeInterruptedExceptionTest {
  @Before public void clearInterruptStatus() {
    interrupted();
  }

  @Test public void testStringConstructor() {
    final InterruptedException cause = new InterruptedException("original");
    final RuntimeInterruptedException ex = new RuntimeInterruptedException("test", cause);
    assertFalse(Thread.interrupted());
    assertEquals("test", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  @Test public void testSimpleConstructor() {
    final InterruptedException cause = new InterruptedException("original");
    final RuntimeInterruptedException ex = new RuntimeInterruptedException(cause);
    assertFalse(Thread.interrupted());
    assertTrue(ex.getMessage().contains("original"));
    assertSame(cause, ex.getCause());
  }
}
