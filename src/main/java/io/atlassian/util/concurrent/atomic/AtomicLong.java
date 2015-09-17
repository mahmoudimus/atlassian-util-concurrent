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
 * {@link java.util.concurrent.atomic.AtomicLong} with richer functionality.
 * This class implements commonly implemented patterns of use of compareAndSet
 * such as {@link #getOrSetAndGetIf(long, long)} and {@link #update(Function)}.
 * 
 * @inheritDoc
 * @since 0.0.12
 */
public class AtomicLong extends java.util.concurrent.atomic.AtomicLong {

  private static final long serialVersionUID = -3751676505640700325L;

  //
  // ctors
  //

  /**
   * Creates a new AtomicLong with a zero initial value.
   */
  public AtomicLong() {}

  /**
   * Creates a new AtomicLong with the given initial value.
   * 
   * @param initialValue the initial value
   */
  public AtomicLong(final long initialValue) {
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
   * @param oldValue to check the current value against
   * @param newValue the new value to set it to
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   */
  public final long getOrSetAndGetIf(final long oldValue, final long newValue) {
    long result = get();
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
   * Do the actual update. Calls the factory method with the old value to do the
   * update logic, then sets the value to that if it hasn't changed in the
   * meantime.
   * 
   * @return the new updated value.
   */
  public final long update(final Function<Long, Long> newValueFactory) {
    long oldValue, newValue;
    do {
      oldValue = get();
      newValue = newValueFactory.apply(oldValue);
      // test first to implement TTAS optimisation
      // then compare and set
    } while ((get() != oldValue) || !compareAndSet(oldValue, newValue));
    return newValue;
  }
}
