package com.atlassian.util.concurrent;

import java.util.concurrent.CountDownLatch;

/**
 * A Latch that may be reused, unlike a {@link CountDownLatch}.
 */
public interface ReusableLatch extends TimedAwaitable {
    void release();
}
