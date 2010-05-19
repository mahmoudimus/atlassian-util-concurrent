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

package com.atlassian.util.concurrent.atomic;

import com.atlassian.util.concurrent.Function;
import com.atlassian.util.concurrent.Supplier;

/**
 * AtomicReferenceArray with richer functionality. This class implements
 * commonly implemented patterns of use of compareAndSet such as
 * {@link #getAndSetIf(int, Object, Object)} and {@link #update(int, Function)}.
 * 
 * @param E the element type of the array.
 * @since 0.0.12
 */
public class AtomicReferenceArray<E> extends java.util.concurrent.atomic.AtomicReferenceArray<E> {

    private static final long serialVersionUID = 6669693075971189L;

    //
    // ctors
    //

    /**
     * Creates a new AtomicReferenceArray of given length.
     * 
     * @param length the length of the array
     */
    public AtomicReferenceArray(final int length) {
        super(length);
    }

    /**
     * Creates a new AtomicReferenceArray with the same length as, and all
     * elements copied from, the given array.
     * 
     * @param array the array to copy elements from
     * @throws NullPointerException if array is null
     */
    public AtomicReferenceArray(final E[] initialValue) {
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
    public final E getOrSetAndGetIf(final int index, final E oldValue, final Supplier<E> newValue) {
        E result = get(index);
        // loop until invariant is true in case some other thread resets
        // reference to oldValue (although if they are doing that then we still
        // cannot guarantee there will be no ABA problem as they could come
        // back and set it after we return)
        while (result == oldValue) {
            final E update = newValue.get();
            // abort if trying to set the same value, otherwise infinite loop
            if (update == oldValue) {
                return oldValue;
            }
            compareAndSet(index, oldValue, update);
            result = get(index);
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
    public final E getOrSetAndGetIf(final int index, final E oldValue, final E newValue) {
        E result = get(index);
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
    public final E update(final int index, final Function<E, E> newValueFactory) {
        E oldValue, newValue;
        do {
            oldValue = get(index);
            newValue = newValueFactory.get(oldValue);
            // test first to implement TTAS optimisation
            // then compare and set
        } while ((get(index) != oldValue) || !compareAndSet(index, oldValue, newValue));
        return newValue;
    }
}
