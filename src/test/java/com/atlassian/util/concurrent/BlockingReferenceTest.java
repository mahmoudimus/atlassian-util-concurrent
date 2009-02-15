package com.atlassian.util.concurrent;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class BlockingReferenceTest {
    final int threads = 20;

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
    public void setSRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        final ExecutorCompletionService<String> completionService = getCompletionService(factory(threads, ref, new Callable<String>() {
            public String call() throws Exception {
                return ref.get();
            }
        }));
        ref.set("testSRSWReferenceSetGet");
        final Future<String> take = completionService.take();
        assertNotNull(take.get());
        assertSame("testSRSWReferenceSetGet", take.get());
        Thread.sleep(10);
        assertNull(completionService.poll());
        Thread.sleep(1);
        assertNull(completionService.poll());
        Thread.sleep(1);
        assertNull(completionService.poll());
        Thread.sleep(1);
        assertNull(completionService.poll());
        Thread.sleep(1);
        assertNull(completionService.poll());
        Thread.sleep(1);
        assertNull(completionService.poll());
    }

    @Test
    public void setMRSWReferenceGet() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        final ExecutorCompletionService<String> completionService = getCompletionService(factory(threads, ref, new Callable<String>() {
            public String call() throws Exception {
                return ref.get();
            }
        }));
        ref.set("testSRSWReferenceSetGet");
        for (int i = 0; i < threads; i++) {
            assertSame("testSRSWReferenceSetGet", completionService.take().get());
        }
        assertNull(completionService.poll());
        assertNotNull(ref.peek());
    }

    @Test
    public void setSRSWReferenceTake() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        final ExecutorCompletionService<String> completionService = getCompletionService(factory(threads, ref, new Callable<String>() {
            public String call() throws Exception {
                return ref.take();
            }
        }));
        ref.set("testSRSWReferenceSetGet");
        assertSame("testSRSWReferenceSetGet", completionService.take().get());
        Thread.sleep(10);
        assertNull(completionService.poll());
        assertNull(ref.peek());
        ref.set("setSRSWReferenceTake2");
        assertSame("setSRSWReferenceTake2", completionService.take().get());
        assertNull(completionService.poll());
        assertNull(ref.peek());
        ref.set("setSRSWReferenceTake3");
        assertSame("setSRSWReferenceTake3", completionService.take().get());
        assertNull(completionService.poll());
        assertNull(ref.peek());
    }

    @Test
    public void setMRSWReferenceTake() throws Exception {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        final ExecutorCompletionService<String> completionService = getCompletionService(factory(threads, ref, new Callable<String>() {
            public String call() throws Exception {
                return ref.take();
            }
        }));
        ref.set("setMRSWReferenceTake");
        assertSame("setMRSWReferenceTake", completionService.take().get());
        Thread.sleep(10);
        assertNull(completionService.poll());
        assertNull(ref.peek());
        ref.set("setMRSWReferenceTake2");
        assertSame("setMRSWReferenceTake2", completionService.take().get());
        ref.set("setMRSWReferenceTake3");
        assertSame("setMRSWReferenceTake3", completionService.take().get());
        ref.set("setMRSWReferenceTake4");
        assertSame("setMRSWReferenceTake4", completionService.take().get());
        assertNull(completionService.poll());
        assertNull(ref.peek());
    }

    @Test
    public void timeoutSRSWReferenceGet() throws InterruptedException {
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        try {
            ref.get(1, TimeUnit.NANOSECONDS);
            fail("TimeoutException expected");
        } catch (final TimeoutException expected) {}
    }

    @Test
    public void timeoutSRSWReferenceTake() throws InterruptedException {
        final BlockingReference<String> ref = BlockingReference.newSRSW();
        try {
            ref.take(1, TimeUnit.NANOSECONDS);
            fail("TimeoutException expected");
        } catch (final TimeoutException expected) {}
    }

    @Test
    public void timeoutMRSWReferenceGet() throws InterruptedException {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        try {
            ref.get(1, TimeUnit.NANOSECONDS);
            fail("TimeoutException expected");
        } catch (final TimeoutException expected) {}
    }

    @Test
    public void timeoutMRSWReferenceTake() throws InterruptedException {
        final BlockingReference<String> ref = BlockingReference.newMRSW();
        try {
            ref.take(1, TimeUnit.NANOSECONDS);
            fail("TimeoutException expected");
        } catch (final TimeoutException expected) {}
    }

    private ExecutorCompletionService<String> getCompletionService(final CallableFactory factory) throws InterruptedException {
        final int threads = factory.threads();
        final ExecutorCompletionService<String> completionService = new ExecutorCompletionService<String>(newFixedThreadPool(threads));
        for (int i = 0; i < threads; i++) {
            completionService.submit(factory.get());
        }
        factory.await();
        return completionService;
    }

    interface CallableFactory extends Supplier<Callable<String>> {
        int threads();

        void await();
    }

    private CallableFactory factory(final int threads, final BlockingReference<String> ref, final Callable<String> delegate) {
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

    private void assertSimple(final BlockingReference<String> ref) throws InterruptedException {
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
    }
}
