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

import static com.atlassian.util.concurrent.LockManagers.Manager.create;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Static factory for producing {@link LockManager} instances.
 */
public class LockManagers {

    private static final class Defaults {
        private static final int CAPACITY = 128;
    }

    static final class Func {
        static final <T> Function<T, Lock> lock() {
            return new Function<T, Lock>() {
                public Lock get(final T ignored) {
                    return new ReentrantLock();
                }
            };
        }

        static final <T> Function<T, ReadWriteLock> readWriteLock() {
            return new Function<T, ReadWriteLock>() {
                public ReadWriteLock get(final T ignored) {
                    return new ReentrantReadWriteLock();
                }
            };
        }
    }

    /**
     * Convenience method that simply calls
     * {@link LockManagers#weakLockManager(Function, int)} with a default
     * initial capacity of 128.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert Ts to Ds.
     * @see LockManagers#weakLockManager(Function, int)
     */
    public static <T, D> LockManager<T> weakLockManager(final Function<T, D> stripeFunction) {
        return weakLockManager(stripeFunction, Defaults.CAPACITY);
    }

    /**
     * Convenience method that simply calls
     * {@link LockManagers#weakLockManager(Function, int)} with a default
     * initial capacity of 128 and the identity function.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert Ts to Ds.
     * @see LockManagers#weakLockManager(Function, int)
     */
    public static <T> LockManager<T> weakLockManager() {
        return weakLockManager(Functions.<T> identity(), Defaults.CAPACITY);
    }

    /**
     * The particular {@link Lock} is resolved using a {@link Function} that
     * resolves to a descriptor used to look up a Lock instance. This allows for
     * a finite set of locks to be used even if the set of T is essentially
     * unbounded.
     * <p>
     * For instance:
     * 
     * <pre>
     * LockManager&lt;Identifiable, Integer&gt; manager = LockManagers.weakLockManager(new Function&lt;Identifiable, Integer&gt;() {
     *     Integer get(Identifiable thing) {
     *         return thing.getId() % 16;
     *     }
     * };
     * </pre>
     * 
     * uses only 16 possible locks as the function returns the modulo 16 of the
     * thing's id.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert Ts to Ds.
     * @param initialCapacity the initial capacity of the internal map.
     * @return a new {@link LockManager} instance that stores created
     * {@link Lock} instances with weak references.
     */
    public static <T, D> LockManager<T> weakLockManager(final Function<T, D> stripeFunction, final int initialCapacity) {
        return create(WeakCacheFunction.create(initialCapacity, Func.<D> lock()), stripeFunction);
    }

    /**
     * Create a {@link LockManager.ReadWrite} for read and write locks. The
     * particular {@link ReadWriteLock} is resolved using a {@link Function}
     * that resolves to a descriptor used to look up a Lock instance. This
     * allows for a finite set of locks to be used even if the set of T is
     * essentially unbounded.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert the input to Ds.
     * @param initialCapacity the initial capacity of the internal map.
     * @return a new {@link LockManager.ReadWrite} instance that stores created
     * {@link Lock} instances with weak references.
     */
    public static <T, D> LockManager.ReadWrite<T> weakReadWriteLockManager(final Function<T, D> stripeFunction, final int initialCapacity) {
        return new LockManager.ReadWrite<T>() {
            private final WeakCacheFunction<D, ReadWriteLock> locks = WeakCacheFunction.create(initialCapacity, Func.<D> readWriteLock());

            private final Manager<T, D> read = create(new Function<D, Lock>() {
                public Lock get(final D input) {
                    return locks.get(input).readLock();
                };
            }, stripeFunction);

            private final Manager<T, D> write = create(new Function<D, Lock>() {
                public Lock get(final D input) {
                    return locks.get(input).writeLock();
                };
            }, stripeFunction);

            public LockManager<T> read() {
                return read;
            }

            public LockManager<T> write() {
                return write;
            }
        };
    }

    private LockManagers() {
        throw new AssertionError("cannot instantiate!");
    }

    /**
     * Default implementation of {@link LockManager}
     * 
     * @param <T> the input type
     * @param <D> the type used for the internal lock resolution.
     */
    static class Manager<T, D> implements LockManager<T> {
        static final <T, D> Manager<T, D> create(final Function<D, Lock> lockResolver, final Function<T, D> stripeFunction) {
            return new Manager<T, D>(lockResolver, stripeFunction);
        }

        private final Function<D, Lock> lockResolver;
        private final Function<T, D> stripeFunction;

        Manager(final Function<D, Lock> lockResolver, final Function<T, D> stripeFunction) {
            this.lockResolver = lockResolver;
            this.stripeFunction = stripeFunction;
        }

        public <R> R withLock(final T descriptor, final Supplier<R> supplier) {
            final Lock lock = lockResolver.get(stripeFunction.get(descriptor));
            lock.lock();
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        }

        public <R> R withLock(final T descriptor, final Callable<R> callable) throws Exception {
            final Lock lock = lockResolver.get(stripeFunction.get(descriptor));
            lock.lock();
            try {
                return callable.call();
            } finally {
                lock.unlock();
            }
        }

        public void withLock(final T descriptor, final Runnable runnable) {
            final Lock lock = lockResolver.get(stripeFunction.get(descriptor));
            lock.lock();
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        }
    }
}
