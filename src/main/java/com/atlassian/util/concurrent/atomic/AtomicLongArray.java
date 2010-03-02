package com.atlassian.util.concurrent.atomic;

import com.atlassian.util.concurrent.Function;

/**
 * AtomicReferenceArray with richer functionality. This class implements
 * commonly implemented patterns of use of compareAndSet such as
 * {@link #getAndSetIf(int, Object, Object)} and {@link #update(int, Function)}.
 * 
 * @inheritDoc
 * @since 0.0.12
 */
public class AtomicLongArray extends java.util.concurrent.atomic.AtomicLongArray {

    //
    // ctors
    //

    /**
     * Creates a new AtomicLongArray of given length.
     * 
     * @param length the length of the array
     */
    public AtomicLongArray(final int length) {
        super(length);
    }

    /**
     * Creates a new AtomicLongArray with the same length as, and all elements
     * copied from, the given array.
     * 
     * @param array the array to copy elements from
     * @throws NullPointerException if array is null
     */
    public AtomicLongArray(final long[] initialValue) {
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
     * @param index the array index
     * @param oldValue to check the current value against (reference equality
     * check only)
     * @param newValue the new value to set it to
     * @return the current reference value if it doesn't match oldValue or a
     * newly created value.
     */
    public final long getOrSetAndGetIf(final int index, final long oldValue, final long newValue) {
        long result = get(index);
        // loop until invariant is true in case some other thread resets
        // reference to oldValue (although if they are doing that then we still
        // cannot guarantee there will be no ABA problem as they could come
        // back and set it after we return)
        while (result == oldValue) {
            // abort if trying to set the same value, otherwise infinite loop
            if (result == newValue) {
                return result;
            }
            compareAndSet(index, oldValue, newValue);
            result = get(index);
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
    public final long update(final int index, final Function<Long, Long> newValueFactory) {
        long oldValue, newValue;
        do {
            oldValue = get(index);
            newValue = newValueFactory.get(oldValue);
            // test first to implement TTAS optimisation
            if (get(index) != oldValue) {
                continue;
            }
            // then compare and set
        } while (!compareAndSet(index, oldValue, newValue));
        return newValue;
    }
}
