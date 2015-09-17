package io.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.junit.Test;

public class ReadWriteManagedLockTest {
  @Test public void testSupplierReturnsValue() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    final TrackedReadWriteLock lock = new TrackedReadWriteLock();
    final ManagedLock.ReadWrite managedLock = ManagedLocks.manageReadWrite(lock);
    assertEquals("blah", managedLock.read().withLock((Supplier<String>) () -> {
      called.set(true);
      return "blah";
    }));
    assertTrue(called.get());
    lock.read.check();
    called.set(false);
    assertEquals("blah", managedLock.write().withLock((Supplier<String>) () -> {
      called.set(true);
      return "blah";
    }));
    assertTrue(called.get());
    lock.write.check();
  }

  @Test public void testCallableReturnsValue() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    final TrackedReadWriteLock lock = new TrackedReadWriteLock();
    final ManagedLock.ReadWrite managedLock = ManagedLocks.manageReadWrite(lock);

    assertEquals("blah", managedLock.read().withLock((Callable<String>) () -> {
      called.set(true);
      return "blah";
    }));
    assertTrue(called.get());
    lock.read.check();
    called.set(false);
    assertEquals("blah", managedLock.write().withLock((Callable<String>) () -> {
      called.set(true);
      return "blah";
    }));
    assertTrue(called.get());
    lock.write.check();
  }

  @Test public void testRunnableRuns() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    final TrackedReadWriteLock lock = new TrackedReadWriteLock();
    final ManagedLock.ReadWrite managedLock = ManagedLocks.manageReadWrite(lock);

    managedLock.read().withLock(() -> {
      called.set(true);
    });
    assertTrue(called.get());
    lock.read.check();
    called.set(false);
    managedLock.write().withLock(() -> {
      called.set(true);
    });
    assertTrue(called.get());
    lock.write.check();
  }

  static class TrackedReadWriteLock implements ReadWriteLock {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    final TrackedLock read = new TrackedLock(lock.readLock());
    final TrackedLock write = new TrackedLock(lock.writeLock());

    public Lock readLock() {
      return read;
    }

    public Lock writeLock() {
      return write;
    }
  }

  static class TrackedLock implements Lock {
    private final Lock delegate;

    boolean locked;
    boolean unlocked;

    TrackedLock(final Lock delegate) {
      this.delegate = delegate;
    }

    void check() {
      assertTrue(locked);
      assertTrue(unlocked);
    }

    public void lock() {
      delegate.lock();
      locked = true;
    }

    public void lockInterruptibly() throws InterruptedException {
      delegate.lockInterruptibly();
    }

    public Condition newCondition() {
      return delegate.newCondition();
    }

    public boolean tryLock() {
      return delegate.tryLock();
    }

    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
      return delegate.tryLock(time, unit);
    }

    public void unlock() {
      delegate.unlock();
      unlocked = true;
    }
  }
}
