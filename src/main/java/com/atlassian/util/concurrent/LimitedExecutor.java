package com.atlassian.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Class that limits the number submitted jobs to an Executor and stores the
 * overflow in a queue. This is to get around the fact that the
 * {@link ThreadPoolExecutor} can either be limited in pool size or be limited
 * in queue size, but not both. This class implements a limit on the pool size
 * if using a cached thread pool.
 * <p>
 * This class only makes sense in conjunction with a cached thread pool that
 * uses a bound queue such as <code>SynchronousQueue</code>. This kind of pool
 * is not for high-throughput jobs as submission depends on a thread being
 * available to execute the job, otherwise a new thread is constructed. Wrapping
 * this Executor around a fixed thread executor doesn't make sense as the limit
 * is already applied by the available thread count.
 */
final class LimitedExecutor implements Executor {
    private final Executor delegate;
    private final BlockingQueue<Runnable> overflow = new LinkedBlockingQueue<Runnable>();
    private final Semaphore semaphore;

    LimitedExecutor(final Executor delegate, final int limit) {
        this.delegate = delegate;
        semaphore = new Semaphore(limit);
    }

    @Override
    public void execute(final Runnable command) {
        if (semaphore.tryAcquire()) {
            try {
                delegate.execute(new Runner(command));
            } catch (final RejectedExecutionException rej) {
                semaphore.release();
                throw rej;
            }
        } else {
            overflow.add(command);
            while (semaphore.availablePermits() > 0) {
                if (!resubmit()) {
                    return;
                }
            }
        }
    }

    private boolean resubmit() {
        final Runnable next = overflow.poll();
        if (next != null) {
            execute(next);
            return true;
        }
        return false;
    }

    class Runner implements Runnable {
        private final Runnable delegate;

        Runner(final Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } finally {
                semaphore.release();
                resubmit();
            }
        }
    }
}