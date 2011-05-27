package com.atlassian.util.concurrent;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.atlassian.util.concurrent.AsyncCompleter.TimeoutAccessor;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsyncCompleterTimeoutAccessorTest {
    @Test(expected = RuntimeTimeoutException.class)
    public void testTimesout() throws Exception {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(Timeout.getMillisTimeout(1000, NANOSECONDS));
        a.apply(new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
                return null;
            }
        });
    }

    @Test
    public void testReturns() throws Exception {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(Timeout.getMillisTimeout(1000, NANOSECONDS));
        assertEquals("hoodad!", a.apply(new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
                assertSame(TimeUnit.MILLISECONDS, unit);
                return future("hoodad!");
            }
        }));
    }

    public void testUsesTimeout() throws Exception {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(new Timeout(5, NANOSECONDS, new MockTimeSupplier(1, NANOSECONDS)));
        final MockCompletionService completionService = new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
                assertSame(NANOSECONDS, unit);
                return future(time);
            }
        };
        assertEquals("4", a.apply(completionService));
        assertEquals("3", a.apply(completionService));
        assertEquals("2", a.apply(completionService));
        assertEquals("1", a.apply(completionService));
        assertEquals("0", a.apply(completionService));
    }

    static Future<String> future(final Object arg) {
        final SettableFuture<String> result = new SettableFuture<String>();
        result.set(String.valueOf(arg));
        return result;
    }
}

// class MockFuture

class MockCompletionService implements CompletionService<String> {

    @Override
    public Future<String> take() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> submit(final Runnable task, final String result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> submit(final Callable<String> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> poll() {
        throw new UnsupportedOperationException();
    }
}
