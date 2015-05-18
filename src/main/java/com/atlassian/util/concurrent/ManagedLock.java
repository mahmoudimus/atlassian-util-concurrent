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

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;

/**
 * {@link ManagedLock} allows {@link Callable callables}, {@link Runnable
 * runnables} and {@link Supplier suppliers} to be run under a lock that is
 * resolved against an input object.
 * 
 * @since 0.0.7
 */
public interface ManagedLock {
  /**
   * Execute the supplied {@link Callable} under a lock determined by the
   * descriptor.
   * 
   * @param <R> the result type
   * @param callable the operation to perform under lock
   * @return whatever the supplied {@link Callable} returns
   * @throws Exception if the supplied {@link Callable} throws an exception
   */
  <R> R withLock(final @NotNull Callable<R> callable) throws Exception;

  /**
   * Execute the supplied {@link Supplier} under a lock determined by the
   * descriptor.
   * <p>
   * Unlike {@link #withLock(Callable)} this version returns a result and does
   * not declare a checked exception.
   * 
   * @param <R> the result type
   * @param supplier the operation to perform under lock
   * @return whatever the supplied {@link Callable} returns
   */
  <R> R withLock(final @NotNull Supplier<R> supplier);

  /**
   * Execute the supplied {@link Runnable} under a lock determined by the
   * descriptor.
   * 
   * @param runnable the operation to perform under lock
   */
  void withLock(final @NotNull Runnable runnable);

  /**
   * Maintains two managed locks that internally use the same
   * {@link ReadWriteLock read/write locks}
   */
  interface ReadWrite {
    /**
     * For performing operations that require read locks
     * 
     * @return a lock manager that uses read locks
     */
    ManagedLock read();

    /**
     * For performing operations that require write locks
     * 
     * @return a lock manager that uses write locks
     */
    ManagedLock write();
  }
}