package com.atlassian.util.concurrent;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.atlassian.util.concurrent.AsyncCompleter.Accessor;
import com.atlassian.util.concurrent.AsyncCompleter.BlockingAccessor;
import com.atlassian.util.concurrent.AsyncCompleter.TimeoutAccessor;

import org.junit.Test;

import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncCompleterAccessorTest {
  @Test(expected = RuntimeTimeoutException.class) public void timesout() {
    final Accessor<String> a = new TimeoutAccessor<String>(Timeout.getMillisTimeout(1000, NANOSECONDS));
    a.apply(new MockCompletionService() {
      @Override public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
        return null;
      }
    });
  }

  @Test public void returns() {
    final Accessor<String> a = new TimeoutAccessor<String>(Timeout.getMillisTimeout(1000, NANOSECONDS));
    assertEquals("hoodad!", a.apply(new MockCompletionService() {
      @Override public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
        assertSame(TimeUnit.MILLISECONDS, unit);
        return new SettableFuture<String>().set(String.valueOf("hoodad!"));
      }
    }));
  }

  @Test public void usesTimeout() {
    final Accessor<String> a = new TimeoutAccessor<String>(new Timeout(5, NANOSECONDS, new MockTimeSupplier(1, NANOSECONDS)));
    final MockCompletionService completionService = new MockCompletionService() {
      @Override public Future<String> poll(final long time, final TimeUnit unit) {
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

  @Test(expected = RuntimeInterruptedException.class) public void handlesInterrupt() {
    final Accessor<String> a = new TimeoutAccessor<String>(new Timeout(5, NANOSECONDS, new MockTimeSupplier(1, NANOSECONDS)));
    a.apply(new MockCompletionService() {
      @Override public Future<String> poll(final long time, final TimeUnit unit) throws InterruptedException {
        throw new InterruptedException();
      }
    });
  }

  @Test(expected = RuntimeExecutionException.class) public void handlesExecution() {
    final Accessor<String> a = new TimeoutAccessor<String>(new Timeout(5, NANOSECONDS, new MockTimeSupplier(1, NANOSECONDS)));
    a.apply(new MockCompletionService() {
      @Override public Future<String> poll(final long time, final TimeUnit unit) {
        return new SettableFuture<String>().setException(new SQLException());
      }
    });
  }

  @Test(expected = RuntimeInterruptedException.class) public void blockingHandlesInterrupt() {
    final Accessor<String> a = new BlockingAccessor<String>();
    a.apply(new MockCompletionService() {
      @Override public java.util.concurrent.Future<String> take() throws InterruptedException {
        throw new InterruptedException();
      }
    });
  }

  @Test(expected = RuntimeExecutionException.class) public void blockingHandlesExecution() {
    final Accessor<String> a = new BlockingAccessor<String>();
    a.apply(new MockCompletionService() {
      @Override public java.util.concurrent.Future<String> take() {
        return new SettableFuture<String>().setException(new SQLException());
      }
    });
  }

  @Test public void timeoutCancels() {
    final Accessor<String> t = new TimeoutAccessor<String>(Timeout.getNanosTimeout(1, NANOSECONDS));
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    final Future<String> f = new MockFuture<String>() {
      @Override public boolean cancel(final boolean mayInterruptIfRunning) {
        cancelled.set(mayInterruptIfRunning);
        return true;
      }
    };
    t.register(f);
    final MockCompletionService completionService = new MockCompletionService() {
      @Override public Future<String> poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        return null;
      }
    };
    try {
      t.apply(completionService);
      fail("RuntimeTimeoutException expected");
    } catch (final RuntimeTimeoutException expected) {}
    assertTrue(cancelled.get());
    // check that it doesn't get cancelled again
    cancelled.set(false);
    try {
      t.apply(completionService);
      fail("RuntimeTimeoutException expected");
    } catch (final RuntimeTimeoutException expected) {}
    assertFalse(cancelled.get());
  }
}
