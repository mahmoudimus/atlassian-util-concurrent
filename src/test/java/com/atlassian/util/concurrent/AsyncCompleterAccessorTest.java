package com.atlassian.util.concurrent;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.atlassian.util.concurrent.AsyncCompleter.BlockingAccessor;
import com.atlassian.util.concurrent.AsyncCompleter.TimeoutAccessor;

import org.junit.Test;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsyncCompleterAccessorTest {
    @Test(expected = RuntimeTimeoutException.class)
    public void timesout() {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(Timeout.getMillisTimeout(1000, NANOSECONDS));
        a.apply(new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
                return null;
            }
        });
    }

    @Test
    public void returns() {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(Timeout.getMillisTimeout(1000, NANOSECONDS));
        assertEquals("hoodad!", a.apply(new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
                assertSame(TimeUnit.MILLISECONDS, unit);
                return new SettableFuture<String>().set(String.valueOf("hoodad!"));
            }
        }));
    }

    @Test
    public void usesTimeout() {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(new Timeout(5, NANOSECONDS, new MockTimeSupplier(1, NANOSECONDS)));
        final MockCompletionService completionService = new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) {
                assertSame(NANOSECONDS, unit);
                return new SettableFuture<String>().set(String.valueOf(time));
            }
        };
        assertEquals("4", a.apply(completionService));
        assertEquals("3", a.apply(completionService));
        assertEquals("2", a.apply(completionService));
        assertEquals("1", a.apply(completionService));
        assertEquals("0", a.apply(completionService));
    }

    @Test(expected = RuntimeInterruptedException.class)
    public void handlesInterrupt() {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(new Timeout(5, NANOSECONDS, new MockTimeSupplier(1, NANOSECONDS)));
        a.apply(new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
                throw new InterruptedException();
            }
        });
    }

    @Test(expected = RuntimeExecutionException.class)
    public void handlesExecution() {
        final TimeoutAccessor<String> a = new TimeoutAccessor<String>(new Timeout(5, NANOSECONDS, new MockTimeSupplier(1, NANOSECONDS)));
        a.apply(new MockCompletionService() {
            @Override
            public Future<String> poll(final long time, final TimeUnit unit) {
                return new SettableFuture<String>().setException(new SQLException());
            }
        });
    }

    @Test(expected = RuntimeInterruptedException.class)
    public void blockingHandlesIterrupt() {
        final BlockingAccessor<String> a = new BlockingAccessor<String>();
        a.apply(new MockCompletionService() {
            @Override
            public java.util.concurrent.Future<String> take() throws InterruptedException {
                throw new InterruptedException();
            }
        });
    }

    @Test(expected = RuntimeExecutionException.class)
    public void blockingHandlesExecution() {
        final BlockingAccessor<String> a = new BlockingAccessor<String>();
        a.apply(new MockCompletionService() {
            @Override
            public java.util.concurrent.Future<String> take() {
                return new SettableFuture<String>().setException(new SQLException());
            }
        });
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
