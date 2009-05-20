package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RuntimeInterruptedExceptionTest {
    @Test
    public void testStringConstructor() {
        final InterruptedException cause = new InterruptedException("original");
        final RuntimeInterruptedException ex = new RuntimeInterruptedException("test", cause);
        assertTrue(Thread.interrupted());
        assertEquals("test", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testSimpleConstructor() {
        final InterruptedException cause = new InterruptedException("original");
        final RuntimeInterruptedException ex = new RuntimeInterruptedException(cause);
        assertTrue(Thread.interrupted());
        assertTrue(ex.getMessage().contains("original"));
        assertSame(cause, ex.getCause());
    }
}
