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

package com.atlassian.util.concurrent;

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
    return new FromSupplier<>(supplier);
  }

  private static class FromSupplier<D, R> implements Function<D, R> {
    private final Supplier<R> supplier;

    FromSupplier(final Supplier<R> supplier) {
      this.supplier = requireNonNull(supplier, "supplier");
    }

    public R apply(final D input) {
      return supplier.get();
    }
  };

  /**
   * Get the value from a supplier.
   *
   * @param <T> the type returned, note the Supplier can be covariant.
   * @return a function that extracts the value from a supplier
   */
  static <T> Function<Supplier<? extends T>, T> fromSupplier() {
    return new ValueExtractor<>();
  }

  private static class ValueExtractor<T> implements
    Function<Supplier<? extends T>, T> {
    public T apply(final Supplier<? extends T> supplier) {
      return supplier.get();
    }
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

  /**
   * Map to a google-collections Function.
   *
   * @param <T> input type
   * @param <R> output type
   * @param function the function to map
   * @return the mapped function.
   */
  public static <T, R> Function<T, R> toGoogleFunction(final Function<T, R> function) {
    return new ToGoogleAdapter<>(function);
  }

  static class ToGoogleAdapter<T, R> implements Function<T, R> {
    private final Function<T, R> delegate;

    ToGoogleAdapter(final Function<T, R> delegate) {
      this.delegate = delegate;
    }

    public R apply(final T from) {
      return delegate.apply(from);
    };
  }

  // /CLOVER:OFF
  private Functions() {}
  // /CLOVER:ON
}
