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

package io.atlassian.util.concurrent.atomic;

import java.util.function.Function;

/**
 * AtomicReferenceArray with richer functionality. This class implements
 * commonly implemented patterns of use of compareAndSet such as
 * {@link #getOrSetAndGetIf(int, long, long)} and {@link #update(int, Function)}
 * .
 *
 * @since 0.0.12
 */
public class AtomicLongArray extends java.util.concurrent.atomic.AtomicLongArray {

  private static final long serialVersionUID = 4358621597645662644L;

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
   * @param initialValue the array to copy elements from
   * @throws java.lang.NullPointerException if array is null
   */
  public AtomicLongArray(final long[] initialValue) {
    super(initialValue);
  }

  //
  // methods
  //

  /**
   * Check the current value and if it matches the old value argument, set it to
   * the new value and return that instead. If the old value argument does not
   * match, ignore both and just return the current value.
   *
   * @param index the array index
   * @param oldValue to check the current value against (reference equality
   * check only)
   * @param newValue the new value to set it to
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
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
   * Do the actual update. Calls the factory method with the old value to do the
   * update logic, then sets the value to that if it hasn't changed in the
   * meantime.
   *
   * @return the new updated value.
   * @param index a int.
   * @param newValueFactory a {@link java.util.function.Function} object.
   */
  public final long update(final int index, final Function<Long, Long> newValueFactory) {
    long oldValue, newValue;
    do {
      oldValue = get(index);
      newValue = newValueFactory.apply(oldValue);
      // test first to implement TTAS optimisation
      // then compare and set
    } while ((get(index) != oldValue) || !compareAndSet(index, oldValue, newValue));
    return newValue;
  }
}
