package com.atlassian.util.concurrent.atomic;

import com.atlassian.util.concurrent.Function;

/**
 * {@link java.util.concurrent.atomic.AtomicInteger} with richer functionality.
 * This class implements commonly implemented patterns of use of compareAndSet
 * such as {@link #getOrSetAndGetIf(int, int)} and {@link #update(Function)}.
 */
public class AtomicInteger extends java.util.concurrent.atomic.AtomicInteger {

    //
    // ctors
    //

    /**
     * Creates a new AtomicInteger with a zero initial value.
     */
    public AtomicInteger() {
        super();
    }

    /**
     * Creates a new AtomicInteger with the given initial value.
     * 
     * @param initialValue the initial value
     */
    public AtomicInteger(final int initialValue) {
        super(initialValue);
    }

    //
    // methods
    //

    /**
     * Check the current value and if it matches the old value argument, set it
     * to the new value and return that instead. If the old value argument does
     * not match, ignore both and just return the current value.
     * 
     * @param oldValue to check the current value against.
     * @param newValue the new value to set it to.
     * @return the current reference value if it doesn't match oldValue or a
     * newly created value.
     */
    public final int getOrSetAndGetIf(final int oldValue, final int newValue) {
        int result = get();
        // loop until invariant is true in case some other thread resets
        // reference to oldValue (although if they are doing that then we still
        // cannot guarantee there will be no ABA problem as they could come
        // back and set it after we return)
        while (result == oldValue) {
            // abort if trying to set the same value, otherwise infinite loop
            if (result == newValue) {
                return oldValue;
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
    public final int update(final Function<Integer, Integer> newValueFactory) {
        int oldValue, newValue;
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
