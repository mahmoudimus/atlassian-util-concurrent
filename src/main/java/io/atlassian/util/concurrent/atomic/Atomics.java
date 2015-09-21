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

import java.util.function.Supplier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * Utility methods for handling the specifics of correctly using the CAS
 * operations on {@link java.util.concurrent.atomic.AtomicReference} classes and the like.
 * <p>
 * All methods are thread safe.
 *
 * @since 0.0.12
 */
@ThreadSafe public final class Atomics {

  /**
   * Get the current value of the {@link AtomicReference reference} but if it
   * matches the oldValue argument, compare-and-set it to one created by the
   * {@link Supplier new value supplier}.
   *
   * @param <T> the object type.
   * @param reference the reference to get the value from.
   * @param oldValue to check the current value against (reference equality
   * check only).
   * @param newValue a {@link java.util.function.Supplier} for a new value. May be called more than
   * once.
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   */
  public static <T> T getAndSetIf(final AtomicReference<T> reference, final T oldValue, final Supplier<T> newValue) {
    T result = requireNonNull(reference, "reference").get();
    // loop until invariant is true in case some other thread resets
    // reference to oldValue (although if they are doing that then we still
    // cannot guarantee there will be no ABA problem as they could come
    // back and set it after we return)
    while (result == oldValue) {
      final T update = newValue.get();
      // abort if trying to set the same value, otherwise infinite loop
      if (update == oldValue) {
        return oldValue;
      }
      reference.compareAndSet(oldValue, update);
      result = reference.get();
    }
    return result;
  }

  /**
   * Check the current value of the {@link AtomicReference reference} and if it
   * matches the old value argument, compare-and-set it to the new value and
   * return that instead. If the old value argument does not match, ignore both
   * and return the current value.
   *
   * @param <T> the object type.
   * @param reference the reference to get the value from
   * @param oldValue to check the current value against (reference equality
   * check only)
   * @param newValue the new value to set it to
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   */
  public static <T> T getAndSetIf(final AtomicReference<T> reference, final T oldValue, final T newValue) {
    T result = requireNonNull(reference, "reference").get();
    // loop until invariant is true in case some other thread resets
    // reference to oldValue (although if they are doing that then we still
    // cannot guarantee there will be no ABA problem as they could come
    // back and set it after we return)
    while (result == oldValue) {
      // abort if trying to set the same value, otherwise infinite loop
      if (newValue == oldValue) {
        return oldValue;
      }
      reference.compareAndSet(oldValue, newValue);
      result = reference.get();
    }
    return result;
  }

  /**
   * Get the current value of the {@link AtomicReference reference} but if it is
   * null, compare-and-set it to one created by the new value {@link java.util.function.Supplier}.
   *
   * @param <T> the object type.
   * @param reference the reference to get the value from
   * @param newValue a {@link java.util.function.Supplier} for a new value. May be called more than
   * once.
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   */
  public static <T> T getAndSetIfNull(final AtomicReference<T> reference, final Supplier<T> newValue) {
    return getAndSetIf(reference, null, newValue);
  }

  /**
   * Get the current value of the {@link AtomicReference reference} but if it is
   * null, compare-and-set it the new value.
   *
   * @param <T> the object type.
   * @param reference the reference to get the value from
   * @param newValue the new value.
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   */
  public static <T> T getAndSetIfNull(final AtomicReference<T> reference, final T newValue) {
    Supplier<T> supplier = () -> newValue;
    return getAndSetIf(reference, null, supplier);
  }

  /**
   * Get the current value of the {@link AtomicReferenceArray array reference}
   * but if it matches the oldValue argument, compare-and-set it to one created
   * by the {@link Supplier new value supplier}.
   *
   * @param <T> the object type.
   * @param index the index to the item
   * @param reference the reference to get the value from
   * @param oldValue to check the current value against (reference equality
   * check only)
   * @param newValue a {@link java.util.function.Supplier} for a new value. May be called more than
   * once.
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   * @throws java.lang.IndexOutOfBoundsException if the index is less than 0 or equal or
   * greater than the array size
   */
  public static <T> T getAndSetIf(final AtomicReferenceArray<T> reference, final int index, final T oldValue, final Supplier<T> newValue) {
    T result = requireNonNull(reference, "reference").get(index);
    // loop until invariant is true in case some other thread resets
    // reference to oldValue (although if they are doing that then we still
    // cannot guarantee there will be no ABA problem as they could come
    // back and set it after we return)
    while (result == oldValue) {
      final T update = newValue.get();
      // abort if trying to set the same value, otherwise infinite loop
      if (update == oldValue) {
        return oldValue;
      }
      reference.compareAndSet(index, oldValue, update);
      result = reference.get(index);
    }
    return result;
  }

  /**
   * Get the current value of the {@link AtomicReferenceArray array reference}
   * but if it matches the oldValue argument, compare-and-set it the new value.
   *
   * @param <T> the object type.
   * @param index the index to the item
   * @param reference the reference to get the value from
   * @param oldValue to check the current value against (reference equality
   * check only)
   * @param newValue the new value.
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   * @throws java.lang.IndexOutOfBoundsException if the index is less than 0 or equal or
   * greater than the array size
   */
  public static <T> T getAndSetIf(final AtomicReferenceArray<T> reference, final int index, final T oldValue, final T newValue) {
    T result = requireNonNull(reference, "reference").get(index);
    // loop until invariant is true in case some other thread resets
    // reference to oldValue (although if they are doing that then we still
    // cannot guarantee there will be no ABA problem as they could come
    // back and set it after we return)
    while (result == oldValue) {
      // abort if trying to set the same value, otherwise infinite loop
      if (newValue == oldValue) {
        return oldValue;
      }
      reference.compareAndSet(index, oldValue, newValue);
      result = reference.get(index);
    }
    return result;
  }

  /**
   * Get the current value of the {@link AtomicReferenceArray array reference}
   * but if it is null, compare-and-set it to one created by the new value
   * {@link java.util.function.Supplier}.
   *
   * @param <T> the object type.
   * @param index the index to the item.
   * @param reference the reference to get the value from.
   * @param newValue a {@link java.util.function.Supplier} for a new value. May be called more than
   * once.
   * @return the current reference value if it doesn't match oldValue or a newly
   * created value.
   * @throws java.lang.IndexOutOfBoundsException if the index is less than 0 or equal or
   * greater than the array size.
   */
  public static <T> T getAndSetIfNull(final AtomicReferenceArray<T> reference, final int index, final Supplier<T> newValue) {
    return getAndSetIf(reference, index, null, newValue);
  }

  /**
   * Get the current value of the {@link AtomicReferenceArray array reference}
   * but if it is null, compare-and-set it the new value
   *
   * @param <T> the object type.
   * @param index the index to the item.
   * @param reference the reference to get the value from.
   * @param newValue the new value.
   * @return the current reference value if it doesn't match oldValue or the new
   * value.
   * @throws java.lang.IndexOutOfBoundsException if the index is less than 0 or equal or
   * greater than the array size.
   */
  public static <T> T getAndSetIfNull(final AtomicReferenceArray<T> reference, final int index, final T newValue) {
    Supplier<T> supplier = () -> newValue;
    return getAndSetIf(reference, index, null, supplier);
  }

  /**
   * Get the current value of the {@link AtomicLong reference} but if it matches
   * the oldValue argument, compare-and-set it the new value.
   *
   * @param reference the reference to get the value from
   * @param oldValue to check the current value against.
   * @param newValue the new value.
   * @return the current value if it doesn't match the old value otherwise the
   * new value.
   */
  public static long getAndSetIf(final AtomicLong reference, final long oldValue, final long newValue) {
    long result = requireNonNull(reference, "reference").get();
    // abort if trying to set the same value, otherwise infinite loop
    if (newValue == oldValue) {
      return result;
    }
    // loop until invariant is true in case some other thread resets
    // reference to oldValue (although if they are doing that then we still
    // cannot guarantee there will be no ABA problem as they could come
    // back and set it after we return)
    while (result == oldValue) {
      reference.compareAndSet(oldValue, newValue);
      result = reference.get();
    }
    return result;
  }

  /**
   * Get the current value of the {@link AtomicInteger reference} but if it
   * matches the oldValue argument, compare-and-set it to one created by the
   * newValue {@link java.util.function.Supplier}.
   *
   * @param reference the reference to get the value from
   * @param oldValue to check the current value against.
   * @param newValue the new value.
   * @return the current value if it doesn't match the old value otherwise the
   * new value.
   */
  public static long getAndSetIf(final AtomicInteger reference, final int oldValue, final int newValue) {
    int result = requireNonNull(reference, "reference").get();
    // abort if trying to set the same value, otherwise infinite loop
    if (newValue == oldValue) {
      return result;
    }
    // loop until invariant is true in case some other thread resets
    // reference to oldValue (although if they are doing that then we still
    // cannot guarantee there will be no ABA problem as they could come
    // back and set it after we return)
    while (result == oldValue) {
      reference.compareAndSet(oldValue, newValue);
      result = reference.get();
    }
    return result;
  }

  /**
   * Get the current value of the {@link AtomicBoolean reference} but if it
   * matches the oldValue argument, compare-and-set it to one created by the
   * newValue {@link java.util.function.Supplier}.
   *
   * @param reference the reference to get the value from
   * @param oldValue to check the current value against.
   * @param newValue the new value.
   * @return the current value if it doesn't match the old value otherwise the
   * new value.
   */
  public static boolean getAndSetIf(final AtomicBoolean reference, final boolean oldValue, final boolean newValue) {
    boolean result = requireNonNull(reference, "reference").get();
    // abort if trying to set the same value, otherwise infinite loop
    if (newValue == oldValue) {
      return result;
    }
    // loop until invariant is true in case some other thread resets
    // reference to oldValue (although if they are doing that then we still
    // cannot guarantee there will be no ABA problem as they could come
    // back and set it after we return)
    while (result == oldValue) {
      reference.compareAndSet(oldValue, newValue);
      result = reference.get();
    }
    return result;
  }

  /*
   * do not ctor
   */
  private Atomics() {
    throw new AssertionError("cannot be instantiated!");
  }
}
