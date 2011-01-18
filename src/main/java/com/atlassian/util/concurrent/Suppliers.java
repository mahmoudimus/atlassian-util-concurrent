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

package com.atlassian.util.concurrent;


/**
 * Useful {@link Supplier} implementations.
 * 
 * @deprecated Since 1.0. Use {@link com.google.common.base.Suppliers} instead,
 * see the methods for the alternatives provided.
 */
@Deprecated
public final class Suppliers {
    /**
     * A {@link Supplier} that always returns the supplied source.
     * 
     * @param <T> the type
     * @param source the object that is always returned.
     * @return a supplier that always returns the supplied argument
     * @deprecated use
     * {@link com.google.common.base.Suppliers#ofInstance(Object)} instead.
     */
    @Deprecated
    public static <T> Supplier<T> memoize(final T source) {
        return new Supplier<T>() {
            public T get() {
                return source;
            }
        };
    }

    /**
     * A {@link Supplier} that asks the argument function for the result using
     * the input argument.
     * 
     * @param <D> the input type
     * @param <T> the result type
     * @param input used as the argument when calling the function.
     * @param function asked to get the result.
     * @return the result
     * @deprecated use
     * {@link com.google.common.base.Suppliers#compose(Function, com.google.common.base.Supplier)}
     * where the supplier is
     * {@link com.google.common.base.Suppliers#ofInstance(Object)}
     */
    @Deprecated
    public static <D, T> Supplier<T> fromFunction(final D input, final Function<D, T> function) {
        return new Supplier<T>() {
            public T get() {
                return function.get(input);
            }
        };
    }

    /**
     * Map to a google-collections Supplier.
     * 
     * @param <T> type
     * @param function the function to map
     * @return the mapped function.
     */
    public static <T> com.google.common.base.Supplier<T> toGoogleSupplier(final Supplier<T> supplier) {
        return new ToGoogleAdapter<T>(supplier);
    }

    static class ToGoogleAdapter<T> implements com.google.common.base.Supplier<T> {
        private final Supplier<T> delegate;

        ToGoogleAdapter(final Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            return delegate.get();
        }
    }

    // /CLOVER:OFF
    private Suppliers() {
        throw new AssertionError("cannot instantiate!");
    }
    // /CLOVER:ON
}
