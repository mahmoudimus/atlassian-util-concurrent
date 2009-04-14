package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.notNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PerformanceAnalysis {

    public static void main(final String[] args) {
        final List<Q> queues = new ArrayList<Q>();
        queues.add(new SRSWBlockingQueue());
        queues.add(new SyncQueue());
        queues.add(new RefQueue());
        queues.add(new BlockingReferenceQueue());
        queues.add(new LockedQueue());
        queues.add(new PhasedQueue());
        // queues.add(new LinkedQueue());
        System.out.println(new PerformanceAnalysis().runTest(queues));
    }

    public String runTest(final List<Q> queues) {
        final int warm = 100000, run = 100000000;
        // warm up
        for (final Q q : queues) {
            runTest(q, warm);
        }
        final StringBuilder builder = new StringBuilder();
        for (final Q q : queues) {
            builder.append(q).append('\t');
            builder.append(runTest(q, run)).append('\r');
        }
        return builder.toString();
    }

    public long runTest(final Q q, final int iterations) {
        final Thread reader = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        int j = 0;
                        j += q.take();
                    }
                } catch (final InterruptedException e) {}
            }
        });
        reader.start();
        long start;
        try {
            start = System.currentTimeMillis();
            for (int i = 0; i < 100000000; i++) {
                q.put(i);
            }
            return System.currentTimeMillis() - start;
        } finally {
            reader.interrupt();
            q.clear();
        }
    }

    interface Q {
        void put(final int i);

        Integer take() throws InterruptedException;

        void clear();
    }

    static class UnboundQueue implements Q {
        private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer>();

        public void put(final int i) {
            try {
                queue.put(i);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public Integer take() throws InterruptedException {
            return queue.take();
        }

        public void clear() {
            queue.clear();
        }
    }

    static class LruQueue implements Q {
        private final LRUBlockingQueue<Integer> queue = new LRUBlockingQueue<Integer>(1);

        public void put(final int i) {
            try {
                queue.put(i);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public Integer take() throws InterruptedException {
            return queue.take();
        }

        public void clear() {
            queue.clear();
        }
    }

    static class LinkedQueue implements Q {
        private final LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer>(1);

        public void put(final int i) {
            try {
                queue.clear();
                queue.put(i);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public Integer take() throws InterruptedException {
            return queue.take();
        }

        public void clear() {
            queue.clear();
        }
    }

    static class PhasedQueue implements Q {
        private final PhasedBlockingReference<Integer> queue = new PhasedBlockingReference<Integer>();

        public void put(final int i) {
            queue.set(i);
        }

        public Integer take() throws InterruptedException {
            return queue.take();
        }

        public void clear() {
        // queue.set(null);
        }
    }

    static class RefQueue implements Q {
        private final BlockingReference<Integer> ref = BlockingReference.newSRSW();

        public void put(final int i) {
            ref.set(i);
        }

        public Integer take() throws InterruptedException {
            return ref.take();
        }

        public void clear() {
            ref.clear();
        }
    }

    static class LockedQueue implements Q {
        private final LockingReference<Integer> queue = new LockingReference<Integer>();

        public void put(final int i) {
            queue.set(i);
        }

        public Integer take() throws InterruptedException {
            return queue.take();
        }

        public void clear() {
            queue.clear();
        }
    }

    static class SyncQueue implements Q {
        private final LinkedList<Integer> queue = new LinkedList<Integer>();

        public void put(final int i) {
            synchronized (queue) {
                queue.clear();
                queue.add(i);
                queue.notifyAll();
            }
        }

        public Integer take() throws InterruptedException {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    queue.wait();
                }
                return queue.getFirst();
            }
        }

        public void clear() {
            synchronized (queue) {
                queue.clear();
                queue.notifyAll();
            }
        }
    }

    static class SRSWBlockingQueue implements Q {
        final BooleanLatch latch = new BooleanLatch();
        final AtomicReference<LinkedList<Integer>> queue = new AtomicReference<LinkedList<Integer>>();

        public void put(final int i) {
            LinkedList<Integer> list;
            do {
                list = queue.getAndSet(null);
                if (list == null) {
                    list = new LinkedList<Integer>();
                }
                list.add(i);
            } while (!queue.compareAndSet(null, list));
            latch.release();
        }

        public Integer take() throws InterruptedException {
            LinkedList<Integer> list;
            do {
                latch.await();
                list = queue.getAndSet(null);
            } while (list == null);
            return list.getFirst();
        }

        public void clear() {
            queue.set(null);
        }
    }

    static class BlockingReferenceQueue implements Q {
        private final BlockingReference<Integer> ref = BlockingReference.newSRSW();

        public void put(final int i) {
            ref.set(i);
        }

        public Integer take() throws InterruptedException {
            return ref.take();
        }

        public void clear() {
            ref.clear();
        }
    }
}

class LockingReference<V> {
    private V ref;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    /**
     * Takes the current element if it is not null and replaces it with null. If
     * the current element is null then wait until it becomes non-null.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     */
    public V take() throws InterruptedException {
        lock.lock();
        try {
            while (ref == null) {
                notEmpty.await();
            }
            final V result = ref;
            ref = null;
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set the value of this reference. This method is lock-free. A thread
     * waiting in {@link #take()} or {@link #take(long, TimeUnit)} will be
     * released and given this value.
     * 
     * @param value the new value.
     */
    public void set(final V value) {
        notNull("value", value);
        lock.lock();
        try {
            ref = value;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isNull() {
        return ref == null;
    }

    void clear() {
        lock.lock();
        try {
            ref = null;
        } finally {
            lock.unlock();
        }
    }
}

class LRUBlockingQueue<E> extends LinkedBlockingQueue<E> {
    private static final long serialVersionUID = -6070900096160951474L;

    public LRUBlockingQueue(final int capacity) {
        super(capacity);
    }

    @Override
    public boolean offer(final E o) {
        while (remainingCapacity() == 0) {
            remove(peek());
        }
        return super.offer(o);
    };

    @Override
    public void put(final E o) throws InterruptedException {
        while (remainingCapacity() == 0) {
            remove(peek());
        }
        super.put(o);
    };
}

class PhasedBlockingReference<V> {
    private final AtomicReference<V> ref = new AtomicReference<V>();
    private final PhasedLatch latch = new PhasedLatch();

    /**
     * Takes the current element if it is not null and replaces it with null. If
     * the current element is null then wait until it becomes non-null.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     */
    public V take() throws InterruptedException {
        while (true) {
            latch.await();
            final V result = ref.getAndSet(null);
            if (result != null) {
                return result;
            }
        }
    }

    /**
     * Takes the current element if it is not null and replaces it with null. If
     * the current element is null then wait until it becomes non-null. The
     * method will throw a {@link TimeoutException} if the timeout is reached
     * before an element becomes available.
     * <p>
     * If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return the current element
     * @throws InterruptedException if the current thread is interrupted while
     * waiting
     * @throws TimeoutException if the timeout is reached without another thread
     * having called {@link #set(Object)}.
     */
    public V take(final long timeout, final TimeUnit unit) throws TimeoutException, InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        return ref.getAndSet(null);
    }

    /**
     * Set the value of this reference. This method is lock-free. A thread
     * waiting in {@link #take()} or {@link #take(long, TimeUnit)} will be
     * released and given this value.
     * 
     * @param value the new value.
     */
    public void set(final V value) {
        notNull("value", value);
        internalSet(value);
    }

    void clear() {
        internalSet(null);
    }

    private void internalSet(final V value) {
        ref.set(value);
        latch.release();
    }

    public boolean isNull() {
        return ref.get() == null;
    }
}
