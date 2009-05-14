/**
 * Copyright 2008 Atlassian Pty Ltd 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.notNull;
import static com.atlassian.util.concurrent.Functions.fromSupplier;
import static com.atlassian.util.concurrent.ManagedLocks.ManagedFactory.managedFactory;
import static com.atlassian.util.concurrent.WeakMemoizer.weakMemoizer;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.atlassian.util.concurrent.ManagedLock.ReadWrite;

/**
 * Static factory for producing {@link ManagedLock} and {@link ReadWrite}
 * instances. Also contains factory methods for getting striping functions for
 * these as well.
 * <p>
 * All Parameters and returns values do no allow nulls. All supplied
 * {@link Function functions} should not allow null values are permit null
 * returns.
 * <p>
 * Several methods take stripe functions. These functions should return the same
 * value for the life of their input, ie they should be based on immutable
 * properties of the input value - otherwise the stripe function might point to
 * a different lock depending on the state of the input.
 * 
 * @since 0.0.7
 */
public class ManagedLocks {
    /**
     * Get a {@link ManagedLock} that manages the supplied {@link Lock}.
     * 
     * @param lock the lock to use.
     * @return a managed lock
     */
    public static @NotNull
    ManagedLock manage(final @NotNull Lock lock) {
        return new ManagedLockImpl(lock);
    }

    /**
     * Get a {@link ManagedLock.ReadWrite} that manages the supplied
     * {@link ReadWriteLock}.
     * 
     * @param lock the lock to use.
     * @return a managed read write lock
     */
    public static @NotNull
    ManagedLock.ReadWrite manageReadWrite(final @NotNull ReadWriteLock lock) {
        return new ReadWriteManagedLock(lock);
    }

    /**
     * Create a {@link Function} for resolving {@link ManagedLock managed locks}
     * . The particular lock is resolved using a striping {@link Function} that
     * is used look up a lock instance. This allows for a finite set of locks to
     * be used even if the set of T is essentially unbounded. The locks are
     * stored using weak references so infrequently accessed locks should not
     * use excess memory.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert the input to the thing used to look up
     * the individual locks
     * @param the factory for creating the individual locks
     * @return a new {@link Function} that provides {@link ManagedLock}
     * instances that stores created instances with weak references.
     */
    public static @NotNull
    <T, D> Function<T, ManagedLock> weakManagedLockFactory(final @NotNull Function<T, D> stripeFunction, final @NotNull Supplier<Lock> lockSupplier) {
        final Function<D, ManagedLock> lockFactory = fromSupplier(managedLockFactory(lockSupplier));
        return managedFactory(weakMemoizer(lockFactory), stripeFunction);
    }

    /**
     * Convenience method that simply calls
     * {@link ManagedLocks#weakManagedLockFactory(Function, Supplier)} with the
     * {@link #lockFactory() default lock factory}.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert Ts to Ds.
     * @see ManagedLocks#weakLockManager(Function, int)
     */
    public static @NotNull
    <T, D> Function<T, ManagedLock> weakManagedLockFactory(final @NotNull Function<T, D> stripeFunction) {
        return weakManagedLockFactory(stripeFunction, lockFactory());
    }

    /**
     * Convenience method that calls
     * {@link ManagedLocks#weakManagedLockFactory(Function)} using the
     * {@link Functions#identity() identity function} for striping, essentially
     * meaning that unique input will have its own lock.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert Ts to Ds.
     * @see ManagedLocks#weakLockManager(Function, int)
     */
    public static @NotNull
    <T> Function<T, ManagedLock> weakManagedLockFactory() {
        return weakManagedLockFactory(Functions.<T> identity());
    }

    /**
     * Create a {@link Function} for resolving {@link ManagedLock.ReadWrite
     * managed read-write locks}. The particular lock is resolved using a
     * striping {@link Function} that is used look up a lock instance. This
     * allows for a finite set of locks to be used even if the set of T is
     * essentially unbounded. The locks are stored using weak references so
     * infrequently accessed locks should not use excess memory.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert the input to the thing used to look up
     * the individual locks
     * @param the factory for creating the individual locks
     * @return a new {@link Function} that provides
     * {@link ManagedLock.ReadWrite} instances that stores created instances
     * with weak references.
     */
    public static @NotNull
    <T, D> Function<T, ManagedLock.ReadWrite> weakReadWriteManagedLockFactory(final @NotNull Function<T, D> stripeFunction,
        final @NotNull Supplier<ReadWriteLock> lockSupplier) {
        notNull("stripeFunction", stripeFunction);
        final Function<D, ReadWrite> readWriteManagedLockFactory = fromSupplier(managedReadWriteLockFactory(lockSupplier));
        final WeakMemoizer<D, ManagedLock.ReadWrite> locks = weakMemoizer(readWriteManagedLockFactory);
        return new Function<T, ManagedLock.ReadWrite>() {
            public ManagedLock.ReadWrite get(final T input) {
                return locks.get(stripeFunction.get(input));
            };
        };
    }

    /**
     * A convenience method for calling
     * {@link #weakReadWriteManagedLockFactory(Function, Supplier)} that uses
     * default {@link ReentrantReadWriteLock locks}
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction
     * @return a new {@link Function} that provides
     * {@link ManagedLock.ReadWrite} instances that stores created instances
     * with weak references.
     */
    public static @NotNull
    <T, D> Function<T, ManagedLock.ReadWrite> weakReadWriteManagedLockFactory(final Function<T, D> stripeFunction) {
        return weakReadWriteManagedLockFactory(stripeFunction, readWriteLockFactory());
    }

    /**
     * A convenience method for calling
     * {@link #weakReadWriteManagedLockFactory(Function)} that uses the
     * {@link Functions#identity() identity function} for striping, essentially
     * meaning that unique input will have its own lock.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @return a new {@link Function} that provides
     * {@link ManagedLock.ReadWrite} instances that stores created instances
     * with weak references.
     */
    public static @NotNull
    <T> Function<T, ManagedLock.ReadWrite> weakReadWriteManagedLockFactory() {
        return weakReadWriteManagedLockFactory(Functions.<T> identity());
    }

    /**
     * A {@link Supplier} of {@link ReentrantLock locks}.
     * 
     * @return lock factory
     */
    static @NotNull
    Supplier<Lock> lockFactory() {
        return new Supplier<Lock>() {
            public Lock get() {
                return new ReentrantLock();
            }
        };
    }

    /**
     * A {@link Supplier} of {@link ReentrantReadWriteLock read write locks}.
     * 
     * @return lock factory
     */
    static @NotNull
    Supplier<ReadWriteLock> readWriteLockFactory() {
        return new Supplier<ReadWriteLock>() {
            public ReadWriteLock get() {
                return new ReentrantReadWriteLock();
            }
        };
    }

    /**
     * A {@link Supplier} of {@link ManagedLock managed locks}.
     * 
     * @return lock factory
     */
    static @NotNull
    Supplier<ManagedLock> managedLockFactory(final @NotNull Supplier<Lock> supplier) {
        notNull("supplier", supplier);
        return new Supplier<ManagedLock>() {
            public ManagedLock get() {
                return new ManagedLockImpl(supplier.get());
            }
        };
    }

    /**
     * A {@link Supplier} of {@link ManagedLock.ReadWrite managed read write
     * locks}.
     * 
     * @return lock factory
     */
    static @NotNull
    Supplier<ManagedLock.ReadWrite> managedReadWriteLockFactory(final @NotNull Supplier<ReadWriteLock> supplier) {
        notNull("supplier", supplier);
        return new Supplier<ManagedLock.ReadWrite>() {
            public ManagedLock.ReadWrite get() {
                return new ReadWriteManagedLock(supplier.get());
            }
        };
    }

    /**
     * Implement {@link ReadWrite}
     */
    static class ReadWriteManagedLock implements ManagedLock.ReadWrite {
        private final ManagedLock read;
        private final ManagedLock write;

        ReadWriteManagedLock(final ReadWriteLock lock) {
            notNull("lock", lock);
            read = new ManagedLockImpl(lock.readLock());
            write = new ManagedLockImpl(lock.writeLock());
        }

        public ManagedLock read() {
            return read;
        }

        public ManagedLock write() {
            return write;
        }
    }

    static class ManagedFactory<T, D> implements Function<T, ManagedLock> {
        static final <T, D> ManagedFactory<T, D> managedFactory(final Function<D, ManagedLock> lockResolver, final Function<T, D> stripeFunction) {
            return new ManagedFactory<T, D>(lockResolver, stripeFunction);
        }

        private final Function<D, ManagedLock> lockResolver;
        private final Function<T, D> stripeFunction;

        ManagedFactory(final Function<D, ManagedLock> lockResolver, final Function<T, D> stripeFunction) {
            this.lockResolver = notNull("lockResolver", lockResolver);
            this.stripeFunction = notNull("stripeFunction", stripeFunction);
        }

        public ManagedLock get(final T descriptor) {
            return lockResolver.get(stripeFunction.get(descriptor));
        };
    }

    /**
     * Default implementation of {@link ManagedLock}
     * 
     * @param <T> the input type
     * @param <D> the type used for the internal lock resolution.
     */
    static class ManagedLockImpl implements ManagedLock {
        private final Lock lock;

        ManagedLockImpl(final @NotNull Lock lock) {
            this.lock = notNull("lock", lock);
        }

        public <R> R withLock(final Supplier<R> supplier) {
            lock.lock();
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        }

        public <R> R withLock(final Callable<R> callable) throws Exception {
            lock.lock();
            try {
                return callable.call();
            } finally {
                lock.unlock();
            }
        }

        public void withLock(final Runnable runnable) {
            lock.lock();
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        }
    }

    // /CLOVER:OFF
    private ManagedLocks() {
        throw new AssertionError("cannot instantiate!");
    }
    // /CLOVER:ON
}