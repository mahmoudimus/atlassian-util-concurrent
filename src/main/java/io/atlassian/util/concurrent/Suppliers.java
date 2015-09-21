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

package io.atlassian.util.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Useful {@link java.util.function.Supplier} implementations.
 */
public final class Suppliers {
  /**
   * A {@link java.util.function.Supplier} that always returns the supplied source.
   *
   * @param <T> the type
   * @param source the object that is always returned.
   * @return a supplier that always returns the supplied argument
   */
  public static <T> Supplier<T> memoize(final T source) {
    return () -> source;
  }

  /**
   * A {@link java.util.function.Supplier} that asks the argument function for the result using the
   * input argument.
   *
   * @param <D> the input type
   * @param <T> the result type
   * @param input used as the argument when calling the function.
   * @param function asked to get the result.
   * @return the result
   */
  public static <D, T> Supplier<T> fromFunction(final D input, final Function<D, T> function) {
    return () -> function.apply(input);
  }

  /**
   * Turn a {@link java.util.function.Supplier} into a {@link java.util.concurrent.Callable}
   *
   * @param supplier a {@link java.util.function.Supplier}.
   * @return a {@link java.util.concurrent.Callable}.
   */
  public static <T> Callable<T> toCallable(Supplier<T> supplier) {
    return new CallableAdapter<>(supplier);
  }

  static class CallableAdapter<T> implements Callable<T> {
    private final Supplier<T> supplier;

    CallableAdapter(final Supplier<T> supplier) {
      this.supplier = supplier;
    }

    @Override public T call() {
      return supplier.get();
    }
  }

  // /CLOVER:OFF
  private Suppliers() {
    throw new AssertionError("cannot instantiate!");
  }
  // /CLOVER:ON
}
