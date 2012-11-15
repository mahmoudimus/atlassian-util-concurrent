package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.ManagedLocks.manage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

public class ManagedLockTest {
  @Test public void supplierReturnsValue() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    assertEquals("blah", manage(new ReentrantLock()).withLock(new Supplier<String>() {
      public String get() {
        called.set(true);
        return "blah";
      }
    }));
    assertTrue(called.get());
  }

  @Test public void callableReturnsValue() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    assertEquals("blah", manage(new ReentrantLock()).withLock(new Callable<String>() {
      public String call() {
        called.set(true);
        return "blah";
      }
    }));
    assertTrue(called.get());
  }

  @Test public void runnableRuns() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    manage(new ReentrantLock()).withLock(new Runnable() {
      public void run() {
        called.set(true);
      }
    });
    assertTrue(called.get());
  }

  @Test public void locksAndUnlocksAndThrows() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    final AtomicBoolean locked = new AtomicBoolean();
    final AtomicBoolean unlocked = new AtomicBoolean();
    final ManagedLock manager = new ManagedLocks.ManagedLockImpl(new ReentrantLock() {
      private static final long serialVersionUID = 5047219203514192915L;

      @Override public void lock() {
        locked.set(true);
        super.lock();
      }

      @Override public void unlock() {
        unlocked.set(true);
        super.unlock();
      }
    });

    class StupidException extends Exception {
      private static final long serialVersionUID = -885206738173390512L;
    }

    try {
      manager.withLock(new Callable<String>() {
        public String call() throws StupidException {
          called.set(true);
          throw new StupidException();
        }
      });
    } catch (final StupidException ignore) {}

    assertTrue(called.get());
    assertTrue(locked.get());
    assertTrue(unlocked.get());
  }

  @Test public void locksAndUnlocksAndRuns() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    final AtomicBoolean locked = new AtomicBoolean();
    final AtomicBoolean unlocked = new AtomicBoolean();
    final ManagedLock manager = new ManagedLocks.ManagedLockImpl(new ReentrantLock() {
      private static final long serialVersionUID = -5545182312276280121L;

      @Override public void lock() {
        locked.set(true);
        super.lock();
      }

      @Override public void unlock() {
        unlocked.set(true);
        super.unlock();
      }
    });

    manager.withLock(new Runnable() {
      public void run() {
        called.set(true);
      }
    });

    assertTrue(called.get());
    assertTrue(locked.get());
    assertTrue(unlocked.get());
  }

  @Test public void locksAndUnlocksAndThrowsRuntime() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    final AtomicBoolean locked = new AtomicBoolean();
    final AtomicBoolean unlocked = new AtomicBoolean();
    final ManagedLock manager = new ManagedLocks.ManagedLockImpl(new ReentrantLock() {
      private static final long serialVersionUID = 4219143544310279745L;

      @Override public void lock() {
        locked.set(true);
        super.lock();
      }

      @Override public void unlock() {
        unlocked.set(true);
        super.unlock();
      }
    });

    class StupidException extends RuntimeException {
      private static final long serialVersionUID = 8829097977007580221L;
    }

    try {
      manager.withLock(new Runnable() {
        public void run() throws StupidException {
          called.set(true);
          throw new StupidException();
        }
      });
      fail("StupidException expected");
    } catch (final StupidException ignore) {}

    assertTrue(called.get());
    assertTrue(locked.get());
    assertTrue(unlocked.get());
  }

  @Test(expected = Exception.class) public void reThrowsCallableException() throws Exception {
    final ManagedLock manager = new ManagedLocks.ManagedLockImpl(new ReentrantLock());

    manager.withLock(new Callable<Void>() {
      public Void call() throws Exception {
        throw new Exception();
      }
    });
  }
}
