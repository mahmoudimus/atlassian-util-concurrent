package com.atlassian.util.concurrent;

import java.util.concurrent.CountDownLatch;

/**
 * A Latch that may be reused, unlike a {@link CountDownLatch}.
 */
public interface ReusableLatch extends Awaitable {
    /**
     * Release the latch, releasing one or more threads that are waiting on it.
     */
    void release();
}
