package com.atlassian.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simple Latch-like structure where threads jump in a queue and are let off rally-style, one after the other.
 */
interface LatchQueue {
    /**
     * await until a {@link #go()} is called
     */
    void await();

    /**
     * cause any threads backed up in {@link #await()} to go
     */
    void go();

    /**
     * How many threads are waiting.
     * 
     * @return the number of threads waiting.
     */
    int size();

    /**
     * Implementation where threads are single pass. Threads jump in a queue and are let off rally-style, one after the other.
     */
    class SinglePass implements LatchQueue {
        private final BlockingQueue<CountDownLatch> queue = new LinkedBlockingQueue<CountDownLatch>();

        /**
         * Construct a new SinglePass LatchQueue, optionally with the first call to not wait by prepping the first latch.
         * 
         * @param prep
         *            if you want the first call to await to get a latch.
         */
        SinglePass(final boolean prep) {
            final CountDownLatch head = new CountDownLatch(1);
            queue.offer(head);
            if (prep) {
                head.countDown();
            }
        }

        public void await() {
            try {
                final CountDownLatch latch = queue.take();
                if (!queue.offer(latch)) {
                    throw new IllegalStateException("LatchQueue is full!");
                }
                latch.await();
            }
            catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void go() {
            try {
                final CountDownLatch latch = queue.take();
                if (latch == null) {
                    throw new IllegalStateException("LatchQueue is empty!");
                }
                latch.countDown();
            }
            catch (final InterruptedException e) {}
            if (!queue.offer(new CountDownLatch(1))) {
                throw new IllegalStateException("LatchQueue is full!");
            }
        }

        public int size() {
            return queue.size();
        }
    }
}