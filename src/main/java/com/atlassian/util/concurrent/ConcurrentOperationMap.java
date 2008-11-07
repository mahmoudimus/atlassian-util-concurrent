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

import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * This will allow you to submit an operation, encapsulated by a {@link Callable}, and keyed by an
 * Object <K>, such that the result of the Callable will be available to any concurrent callers with
 * the same Object key. The result of the Callable is discarded after the operation has succeeded.
 * <p>
 * There is an assumption that the callers that are using this will submit the same Operations
 * associated with the same keyed Object AND that the operations will be idempotent. This structure
 * only ensures that if Concurrent operations with the same key are submitted that ONLY ONE of the
 * operations will be executed. If the operations are not concurrent then both operations will be
 * executed, hence the need for them to be idempotent.
 * <p>
 * Essentially, this is a Map whose elements expire very quickly. It is particularly useful for
 * situations where you need to get or create the result, and you want to prevent concurrent
 * creation of the result.
 */
@ThreadSafe public interface ConcurrentOperationMap<K, R> {
    /**
     * The operation <R> will be keyed on the name <K>.
     * 
     * @param key the key, like any map key this should be an immutable object that correctly
     *            implements {@link #hashCode()} and {@link #equals(Object)}
     * @param operation is the operation to execute whose result will be accessible to any
     *            concurrent callers with the same key.
     * @return result of the operation
     * @throws ExecutionException if the callable generated a checked exception, otherwise a runtime
     *             exception or error will be thrown
     */
    R runOperation(K key, Callable<R> operation) throws ExecutionException;
}
