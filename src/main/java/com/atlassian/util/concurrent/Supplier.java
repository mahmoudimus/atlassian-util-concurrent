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

import java.util.NoSuchElementException;

/**
 * A Supplier of objects of a single type. Semantically, this could be a
 * Factory, Generator, Builder, Closure, Producer or something else entirely. No
 * guarantees are implied by this interface. Implementations may return null if
 * no objects are available, can optionally block until elements are available
 * or throw {@link NoSuchElementException}.
 * <p>
 * Thread safety of a Supplier is not mandated by this interface, although
 * serious care and consideration should be taken with any implementations that
 * are not.
 * 
 * @param <T> the type of object supplied.
 */
public interface Supplier<T> extends com.google.common.base.Supplier<T> {
    /**
     * Produce an object. Retrieve an instance of the appropriate type. The
     * returned object may or may not be a new instance, depending on the
     * implementation.
     * 
     * @return the product, may be null if there are no objects available.
     * @throws NoSuchElementException if the supply has been exhausted.
     */
    T get();
}