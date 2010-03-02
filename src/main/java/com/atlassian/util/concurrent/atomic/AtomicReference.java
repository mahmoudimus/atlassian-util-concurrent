package com.atlassian.util.concurrent.atomic;

import com.atlassian.util.concurrent.Function;
import com.atlassian.util.concurrent.Supplier;

/**
 * AtomicReference with richer functionality. This class implements commonly
 * implemented patterns of use of compareAndSet such as
 * {@link #getAndSetIf(Object, Object)} and {@link #update(Function)}.
 * 
 * @inheritDoc
 * @since 0.0.12
 */
public class AtomicReference<V> extends java.util.concurrent.atomic.AtomicReference<V> {

    //
    // ctors
    //

    /**
     * Creates a new AtomicReference with null initial value.
     */
    public AtomicReference() {
        super();
    }

    /**
     * Creates a new AtomicReference with the given initial value.
     * 
     * @param initialValue the initial value
     */
    public AtomicReference(final V initialValue) {
        super(initialValue);
    }

    //
    // methods
    //

    /**
     * Check the current value and if it matches the old value argument, set it
     * to the one created by the {@link Supplier new value supplier} and return
     * that instead. If the old value argument does not match, ignore both and
     * just return the current value.
     * 
     * @param <T> the object type.
     * @param oldValue to check the current value against (reference equality
     * check only).
     * @param newValue a {@link Supplier} for a new value. May be called more
     * than once.
     * @return the current reference value if it doesn't match old value or a
     * newly created value.
     */
    public final V getOrSetAndGetIf(final V oldValue, final Supplier<V> newValue) {
        V result = get();
        // loop until invariant is true in case some other thread resets
        // reference to oldValue (although if they are doing that then we still
        // cannot guarantee there will be no ABA problem as they could come
        // back and set it after we return)
        while (result == oldValue) {
            final V update = newValue.get();
            // abort if trying to set the same value, otherwise infinite loop
            if (update == oldValue) {
                return oldValue;
            }
            compareAndSet(oldValue, update);
            result = get();
        }
        return result;
    }

    /**
     * Check the current value and if it matches the old value argument, set it
     * to the new value and return that instead. If the old value argument does
     * not match, ignore both and just return the current value.
     * 
     * @param <T> the object type.
     * @param oldValue to check the current value against (reference equality
     * check only)
     * @param newValue the new value to set it to
     * @return the current reference value if it doesn't match oldValue or a
     * newly created value.
     */
    public final V getOrSetAndGetIf(final V oldValue, final V newValue) {
        V result = get();
        // loop until invariant is true in case some other thread resets
        // reference to oldValue (although if they are doing that then we still
        // cannot guarantee there will be no ABA problem as they could come
        // back and set it after we return)
        while (result == oldValue) {
            // abort if trying to set the same value, otherwise infinite loop
            if (result == newValue) {
                return result;
            }
            compareAndSet(oldValue, newValue);
            result = get();
        }
        return result;
    }

    /**
     * Do the actual update. Calls the factory method with the old value to do
     * the update logic, then sets the value to that if it hasn't changed in the
     * meantime.
     * 
     * @return the new updated value.
     */
    public final V update(final Function<V, V> newValueFactory) {
        V oldValue, newValue;
        do {
            oldValue = get();
            newValue = newValueFactory.get(oldValue);
            // test first to implement TTAS optimisation
            if (get() != oldValue) {
                continue;
            }
            // then compare and set
        } while (!compareAndSet(oldValue, newValue));
        return newValue;
    }
}
