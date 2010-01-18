package com.atlassian.util.concurrent;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Utility methods for handling the specifics of correctly using the CAS
 * operations on {@link AtomicReference} classes and the like.
 * <p>
 * All methods are thread safe.
 */
@ThreadSafe
public final class Atomics {

    /**
     * Get the current value of the {@link AtomicReference reference} but if it
     * matches the oldValue argument, compare-and-set it to one created by the
     * newValue {@link Supplier}.
     * 
     * @param <T> the object type.
     * @param reference the reference to get the value from
     * @param oldValue to check the current value against (reference equality
     * check only)
     * @param newValue a {@link Supplier} for a new value. May be called more
     * than once, should always create a new value and that value should never
     * be null or match oldValue.
     * @return the current reference value if it doesn't match oldValue or a
     * newly created value.
     */
    public static <T> T getAndSetIf(final AtomicReference<T> reference, final T oldValue, final Supplier<T> newValue) {
        T result = reference.get();
        // loop until invariant is true in case some other thread resets
        // reference to oldValue (although if they are doing that then we still
        // cannot guarantee there will be no ABA problem as they could come
        // back and set it after we return)
        while (result == oldValue) {
            reference.compareAndSet(oldValue, newValue.get());
            result = reference.get();
        }
        return result;
    }

    /**
     * /** Get the current value of the {@link AtomicReference reference} but if
     * it is null, compare-and-set it to one created by the newValue
     * {@link Supplier}.
     * 
     * @param <T> the object type.
     * @param reference the reference to get the value from
     * @param newValue a {@link Supplier} for a new value. May be called more
     * than once, should always create a new value and that value should never
     * be null.
     * @return the current reference value if it doesn't match oldValue or a
     * newly created value.
     */
    public static <T> T getAndSetIfNull(final AtomicReference<T> reference, final Supplier<T> newValue) {
        return getAndSetIf(reference, null, newValue);
    }

    /*
     * do not ctor
     */
    private Atomics() {
        throw new AssertionError("cannot be instantiated!");
    }
}
