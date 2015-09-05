/**
 * Copyright 2009 Atlassian Pty Ltd 
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

package io.atlassian.util.concurrent;

import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class Functions {
  /**
   * Get a function that uses the Supplier as a factory for all inputs.
   *
   * @param <D> the key type, ignored
   * @param <R> the result type
   * @param supplier called for all inputs
   * @return the function
   */
  public static <D, R> Function<D, R> fromSupplier(final @NotNull Supplier<R> supplier) {
    return d -> supplier.get();
  }

  /**
   * Get the value from a supplier.
   *
   * @param <T> the type returned, note the Supplier can be covariant.
   * @return a function that extracts the value from a supplier
   */
  static <T> Function<Supplier<? extends T>, T> fromSupplier() {
    return Supplier::get;
  }

  /**
   * Get a function that weakly memoizes the output – ie. subsequent calls for
   * the same input value will return the same reference if it has not been
   * garbage collected because there are no external strong referents to it.
   * 
   * @param <T> the input or key type for the function. Must be a value
   * (immutable) and have a well behaved hashcode implementation.
   * @param <R> the output type of the for the function.
   * @param f the function to call if the value is not already cached.
   * @return the function that will .
   */
  public static <T, R> Function<T, R> weakMemoize(Function<T, R> f) {
    return WeakMemoizer.weakMemoizer(f);
  }

  /**
   * Function that can be used to ignore any RuntimeExceptions that a
   * {@link Supplier} may produce and return null instead.
   * 
   * @param <T> the result type
   * @return a Function that transforms an exception into a null
   */
  static <T> Function<Supplier<T>, Supplier<T>> ignoreExceptions() {
    return new ExceptionIgnorer<>();
  }

  static class ExceptionIgnorer<T> implements
    Function<Supplier<T>, Supplier<T>> {
    public Supplier<T> apply(final Supplier<T> from) {
      return new IgnoreAndReturnNull<>(from);
    }
  }

  static class IgnoreAndReturnNull<T> implements Supplier<T> {
    private final Supplier<T> delegate;

    IgnoreAndReturnNull(final Supplier<T> delegate) {
      this.delegate = requireNonNull(delegate, "delegate");
    }

    public T get() {
      try {
        return delegate.get();
      } catch (final RuntimeException ignore) {
        return null;
      }
    }
  }

  // /CLOVER:OFF
  private Functions() {}
  // /CLOVER:ON
}
