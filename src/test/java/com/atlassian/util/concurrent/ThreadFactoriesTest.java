package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.ThreadFactories.namedThreadFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoriesTest {

    @Test()
    public void threadFactory() {
        final ThreadFactory threadFactory = namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, Thread.NORM_PRIORITY);
        assertNotNull(threadFactory.newThread(new MockRunnable()));
    }

    @Test()
    public void threadName() {
        final ThreadFactory threadFactory = namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, Thread.NORM_PRIORITY);
        assertEquals(this.getClass().getName() + ":thread-1", threadFactory.newThread(new MockRunnable()).getName());
        assertEquals(this.getClass().getName() + ":thread-2", threadFactory.newThread(new MockRunnable()).getName());
    }

    @Test()
    public void userThread() {
        final ThreadFactory threadFactory = namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, Thread.NORM_PRIORITY);
        assertFalse(threadFactory.newThread(new MockRunnable()).isDaemon());
    }

    @Test()
    public void daemonThread() {
        final ThreadFactory threadFactory = namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.DAEMON, Thread.NORM_PRIORITY);
        assertTrue(threadFactory.newThread(new MockRunnable()).isDaemon());
    }

    @Test()
    public void minPriority() {
        final ThreadFactory threadFactory = namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, Thread.MIN_PRIORITY);
        assertEquals(Thread.MIN_PRIORITY, threadFactory.newThread(new MockRunnable()).getPriority());
    }

    @Test()
    public void normalPriority() {
        final ThreadFactory threadFactory = namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, Thread.NORM_PRIORITY);
        assertEquals(Thread.NORM_PRIORITY, threadFactory.newThread(new MockRunnable()).getPriority());
    }

    @Test()
    public void maxPriority() {
        final ThreadFactory threadFactory = namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, Thread.MAX_PRIORITY);
        assertEquals(Thread.MAX_PRIORITY, threadFactory.newThread(new MockRunnable()).getPriority());
    }

    @Test()
    public void notNullNameOnly() {
        assertNotNull(namedThreadFactory(this.getClass().getName()));
    }

    @Test()
    public void notNullNameAndType() {
        assertNotNull(namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER));
    }

    @Test()
    public void notNullNameTypePriority() {
        assertNotNull(namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, Thread.NORM_PRIORITY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullName() {
        namedThreadFactory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullType() {
        namedThreadFactory(this.getClass().getName(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void priorityTooLow() {
        namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void priorityTooHigh() {
        namedThreadFactory(this.getClass().getName(), ThreadFactories.Type.USER, 11);
    }

    static class MockRunnable implements Runnable {
        public void run() {}
    }
}
