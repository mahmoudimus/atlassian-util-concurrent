package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Functions.fromSupplier;
import static com.atlassian.util.concurrent.LockManagers.Manager.createManager;
import static com.atlassian.util.concurrent.ManagedLocks.lockFactory;
import static com.atlassian.util.concurrent.ManagedLocks.managedLockFactory;
import static com.atlassian.util.concurrent.WeakMemoizer.weakMemoizer;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

/**
 * @deprecated since 0.0.7 use {@link ManagedLocks} instead
 */
@Deprecated
// /CLOVER:OFF
public class LockManagers {

    /**
     * Convenience method that simply calls
     * {@link ManagedLocks#weakLockManager(Function)} with the identity
     * function. So all inputs map directly to an individual lock.
     * 
     * @param <T> the type of the thing used to look up locks
     * @param <D> the type used to map lock instances
     * @param stripeFunction to convert Ts to Ds.
     * @see ManagedLocks#weakLockManager(Function, int)
     * @deprecated use {@link #weakManagedLockFactory()} instead.
     */
    @Deprecated
    public static <T> LockManager<T> weakLockManager() {
        return weakLockManager(Functions.<T> identity());
    }

    /**
     * Get a {@link LockManager} that is used to perform operations under a
     * particular lock. The specific lock chosen is decided by the supplied
     * striping function. The particular {@link Lock} is resolved using a
     * {@link Function} that resolves to a descriptor used to look up a Lock
     * instance. This allows for a finite set of locks to be used even if the
     * set of T is essentially unbounded.
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
     * @deprecated use {@link #weakManagedLockFactory(Function, int)} instead.
     */
    @Deprecated
    public static <T, D> LockManager<T> weakLockManager(final Function<T, D> stripeFunction) {
        final Function<D, ManagedLock> lockFactory = fromSupplier(managedLockFactory(lockFactory()));
        final WeakMemoizer<D, ManagedLock> memoizer = weakMemoizer(lockFactory);
        return createManager(memoizer, stripeFunction);
    }

    /**
     * Default implementation of {@link LockManager}
     * 
     * @param <T> the input type
     * @param <D> the type used for the internal lock resolution.
     */
    static class Manager<T, D> implements LockManager<T> {
        static final <T, D> Manager<T, D> createManager(final Function<D, ManagedLock> lockFactory, final Function<T, D> stripeFunction) {
            return new Manager<T, D>(lockFactory, stripeFunction);
        }

        private final Function<D, ManagedLock> lockFactory;
        private final Function<T, D> stripeFunction;

        Manager(final Function<D, ManagedLock> lockFactory, final Function<T, D> stripeFunction) {
            this.lockFactory = lockFactory;
            this.stripeFunction = stripeFunction;
        }

        public <R> R withLock(final T descriptor, final Supplier<R> supplier) {
            return lockFactory.get(stripeFunction.get(descriptor)).withLock(supplier);
        }

        public <R> R withLock(final T descriptor, final Callable<R> callable) throws Exception {
            return lockFactory.get(stripeFunction.get(descriptor)).withLock(callable);
        }

        public void withLock(final T descriptor, final Runnable runnable) {
            lockFactory.get(stripeFunction.get(descriptor)).withLock(runnable);
        }
    }
}
// /CLOVER:ON
