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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@ThreadSafe
public class ConcurrentOperationMapImpl<K, R> implements ConcurrentOperationMap<K, R> {

    private final ConcurrentMap<K, FutureTask<R>> map = new ConcurrentHashMap<K, FutureTask<R>>();

    public R runOperation(final K key, final Callable<R> operation) throws ExecutionException {
        FutureTask<R> future = map.get(key);
        while (future == null) {
            map.putIfAbsent(key, new FutureTask<R>(operation));
            future = map.get(key);
        }
        try {
            return runAndGet(future);
        } finally {
            map.remove(key, future);
        }
    }

    R runAndGet(final FutureTask<R> future) throws ExecutionException {
        // Concurrent calls to run do not matter as run will be a no-op if
        // already running or run in another thread
        future.run();
        try {
            return future.get();
        } catch (final InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw e;
            }
        }
    }
}
