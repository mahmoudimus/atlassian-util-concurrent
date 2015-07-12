package com.atlassian.util.concurrent;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class ManagedLocksTest {
  @Test(expected = NullPointerException.class) public void weakManagedLockFactoryNullStripe() throws Exception {
    ManagedLocks.weakManagedLockFactory(null);
  }

  @Test(expected = NullPointerException.class) public void weakManagedLockFactoryNullLockSupplier() throws Exception {
    ManagedLocks.weakManagedLockFactory(input -> new Object(), null);
  }

  @Test public void weakManagedLockFactory() throws Exception {
    ManagedLocks.weakManagedLockFactory();
  }

  @Test(expected = NullPointerException.class) public void weakRWManagedLockFactoryNullStripe() throws Exception {
    ManagedLocks.weakReadWriteManagedLockFactory(null);
  }

  @Test(expected = NullPointerException.class) public void weakRWManagedLockFactoryNullLockSupplier() throws Exception {
    ManagedLocks.weakReadWriteManagedLockFactory(input -> new Object(), null);
  }

  @Test public void weakRWManagedLockFactory() throws Exception {
    assertNotNull(ManagedLocks.weakReadWriteManagedLockFactory());
  }

  @Test public void lockFactory() throws Exception {
    final Supplier<Lock> lockFactory = ManagedLocks.lockFactory;
    assertNotNull(lockFactory);
    assertNotNull(lockFactory.get());
  }

  @Test public void readWriteLockFactory() throws Exception {
    final Supplier<ReadWriteLock> lockFactory = ManagedLocks.readWriteLockFactory;
    assertNotNull(lockFactory);
    assertNotNull(lockFactory.get());
  }

  @Test public void manage() throws Exception {
    final Supplier<ManagedLock> lockFactory = ManagedLocks.managedLockFactory(ManagedLocks.lockFactory);
    assertNotNull(lockFactory);
    assertNotNull(lockFactory.get());
  }

  @Test public void manageReadWrite() throws Exception {
    final Supplier<ManagedLock.ReadWrite> lockFactory = ManagedLocks.managedReadWriteLockFactory(ManagedLocks.readWriteLockFactory);
    assertNotNull(lockFactory);
    assertNotNull(lockFactory.get());
  }

  @Test(expected = NullPointerException.class) public void manageNull() throws Exception {
    ManagedLocks.managedLockFactory(null);
  }

  @Test(expected = NullPointerException.class) public void manageReadWriteNull() throws Exception {
    ManagedLocks.managedReadWriteLockFactory(null);
  }

  @Test public void managedFactory() throws Exception {
    final Function<Integer, ManagedLock> lockFactory = ManagedLocks.weakManagedLockFactory();
    assertNotNull(lockFactory.apply(1));
  }

  @Test public void managedReadWriteFactory() throws Exception {
    final Function<Integer, ManagedLock.ReadWrite> lockFactory = ManagedLocks.weakReadWriteManagedLockFactory();
    assertNotNull(lockFactory.apply(1));
  }
}
