package com.atlassian.util.concurrent;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockingReferenceTest {
    static final int threads = 20;

    @Test
    public void simpleSRSWReference() throws Exception {
        assertSimple(BlockingReference.<String> newSRSW());
    }

    @Test
    public void simpleMRSWReference() throws Exception {
        assertSimple(BlockingReference.<String> newMRSW());
    }

    @Test
    public void simpleSRSWReferenceNull() throws Exception {
        assertSimple(BlockingReference.<String> newSRSW(null));
    }

    @Test
    public void simpleMRSWReferenceNull() throws Exception {
        assertSimple(BlockingReference.<String> newMRSW(null));
    }

    @Test
    public void initialValueSRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW("initialValueSRSWReferenceGet");
        final Exec<String> executor = executor(factory(threads, new Callable<String>() {
            public String call() throws Exception {
                return ref.get();
            }
        }));
        try {
            for (int i = 0; i < threads; i++) {
                assertSame("initialValueSRSWReferenceGet", executor.completion.take().get());
            }
        } finally {
            executor.pool.shutdownNow();
        }
    }

    @Test
    public void initialValueMRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW("initialValueMRSWReferenceGet");
        final Exec<String> executor = executor(factory(threads, new Callable<String>() {
            public String call() throws Exception {
                return ref.get();
            }
        }));
        try {
            for (int i = 0; i < threads; i++) {
                assertSame("initialValueMRSWReferenceGet", executor.completion.take().get());
            }
        } finally {
            executor.pool.shutdownNow();
        }
    }

    @Test
    public void setMRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        final Exec<String> executor = executor(factory(threads, new Callable<String>() {
            public String call() throws Exception {
                return ref.get();
            }
        }));
        try {
            ref.set("testSRSWReferenceSetGet");
            for (int i = 0; i < threads; i++) {
                assertSame("testSRSWReferenceSetGet", executor.completion.take().get());
            }
            assertNull(executor.completion.poll());
            assertNotNull(ref.peek());
        } finally {
            executor.pool.shutdownNow();
        }
    }

    @Test
    public void setSRSWReferenceTake() throws Exception {
        final CountDownLatch running = new CountDownLatch(1);
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        final Exec<String> executor = executor(factory(threads, new Callable<String>() {
            public String call() throws Exception {
                running.await();
                return ref.take();
            }
        }));
        try {
            ref.set("testSRSWReferenceSetGet");
            running.countDown();
            assertSame("testSRSWReferenceSetGet", executor.completion.take().get());
            Thread.sleep(10);
            assertNull(executor.completion.poll());
            assertNull(ref.peek());
            ref.set("setSRSWReferenceTake2");
            assertSame("setSRSWReferenceTake2", executor.completion.take().get());
            assertNull(executor.completion.poll());
            assertNull(ref.peek());
            ref.set("setSRSWReferenceTake3");
            assertSame("setSRSWReferenceTake3", executor.completion.take().get());
            assertNull(executor.completion.poll());
            assertNull(ref.peek());
        } finally {
            executor.pool.shutdownNow();
        }
    }

    @Test
    public void setMRSWReferenceTake() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        final Exec<String> executor = executor(factory(threads, new Callable<String>() {
            public String call() throws Exception {
                return ref.take();
            }
        }));
        try {
            ref.set("setMRSWReferenceTake");
            assertSame("setMRSWReferenceTake", executor.completion.take().get());
            Thread.sleep(10);
            assertNull(executor.completion.poll());
            assertNull(ref.peek());
            ref.set("setMRSWReferenceTake2");
            assertSame("setMRSWReferenceTake2", executor.completion.take().get());
            ref.set("setMRSWReferenceTake3");
            assertSame("setMRSWReferenceTake3", executor.completion.take().get());
            ref.set("setMRSWReferenceTake4");
            assertSame("setMRSWReferenceTake4", executor.completion.take().get());
            assertNull(executor.completion.poll());
            assertNull(ref.peek());
        } finally {
            executor.pool.shutdownNow();
        }
    }

    public void noTimeoutSRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW("first");
        assertEquals("first", ref.get(1, TimeUnit.NANOSECONDS));
    }

    public void noTimeoutSRSWReferenceTake() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW("first");
        assertEquals("first", ref.take(1, TimeUnit.NANOSECONDS));
    }

    @Test(expected = TimeoutException.class)
    public void timeoutSRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        ref.get(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void timeoutSRSWReferenceTake() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        ref.take(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void timeoutMRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        ref.get(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void timeoutMRSWReferenceTake() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        ref.take(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void timeoutSRSWReferenceTakeIfTaken() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        ref.set("blah");
        assertSame("blah", ref.take(1, TimeUnit.NANOSECONDS));
        ref.take(1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void timeoutMRSWReferenceTakeIfTaken() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        ref.set("blah");
        assertSame("blah", ref.take(1, TimeUnit.NANOSECONDS));
        ref.take(1, TimeUnit.NANOSECONDS);
    }

    interface CallableFactory extends Supplier<Callable<String>> {
        int threads();

        void await();
    }

    private CallableFactory factory(final int threads, final Callable<String> delegate) {
        final CountDownLatch start = new CountDownLatch(threads);

        final Supplier<Callable<String>> supplier = new Supplier<Callable<String>>() {
            public Callable<String> get() {
                return new Callable<String>() {
                    public String call() throws Exception {
                        start.countDown();
                        start.await();
                        return delegate.call();
                    }
                };
            }
        };

        return new CallableFactory() {
            public void await() {
                try {
                    start.await();
                } catch (final InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                }
            }

            public Callable<String> get() {
                return supplier.get();
            }

            public int threads() {
                return threads;
            }
        };
    }

    private void assertSimple(final BlockingReference<String> ref) throws InterruptedException, TimeoutException {
        assertTrue(ref.isEmpty());
        assertNull(ref.peek());
        ref.set("test");
        assertFalse(ref.isEmpty());
        assertNotNull(ref.peek());
        assertSame("test", ref.peek());
        ref.clear();
        assertTrue(ref.isEmpty());
        assertNull(ref.peek());
        ref.set("test2");
        assertFalse(ref.isEmpty());
        assertSame("test2", ref.get());
        assertFalse(ref.isEmpty());
        ref.set("test3");
        assertFalse(ref.isEmpty());
        assertSame("test3", ref.peek());
        assertSame("test3", ref.take());
        assertTrue(ref.isEmpty());
        assertNull(ref.peek());
        ref.set("test4");
        assertFalse(ref.isEmpty());
        assertSame("test4", ref.get(1, TimeUnit.SECONDS));
        assertFalse(ref.isEmpty());
        assertSame("test4", ref.take(1, TimeUnit.SECONDS));
        assertTrue(ref.isEmpty());
    }

    private Exec<String> executor(final CallableFactory factory) throws InterruptedException {
        final int threads = factory.threads();
        final ExecutorService pool = newFixedThreadPool(threads, ThreadFactories.namedThreadFactory(this.getClass().getCanonicalName()));
        final ExecutorCompletionService<String> completionService = new ExecutorCompletionService<String>(pool);
        for (int i = 0; i < threads; i++) {
            completionService.submit(factory.get());
        }
        factory.await();
        return new Exec<String>(pool, completionService);
    }

    static class Exec<T> {
        final ExecutorService pool;
        final ExecutorCompletionService<T> completion;

        Exec(final ExecutorService executor, final ExecutorCompletionService<T> completion) {
            this.pool = executor;
            this.completion = completion;
        }
    }
}
