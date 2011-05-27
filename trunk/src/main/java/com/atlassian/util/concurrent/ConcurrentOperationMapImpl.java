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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@ThreadSafe
public class ConcurrentOperationMapImpl<K, R> implements ConcurrentOperationMap<K, R> {

    private final ConcurrentMap<K, CallerRunsFuture<R>> map = new ConcurrentHashMap<K, CallerRunsFuture<R>>();
    private final Function<Callable<R>, CallerRunsFuture<R>> futureFactory;

    public ConcurrentOperationMapImpl() {
        this(new Function<Callable<R>, CallerRunsFuture<R>>() {
            public CallerRunsFuture<R> get(final Callable<R> input) {
                return new CallerRunsFuture<R>(input);
            }
        });
    }

    ConcurrentOperationMapImpl(final Function<Callable<R>, CallerRunsFuture<R>> futureFactory) {
        this.futureFactory = Assertions.notNull("futureFactory", futureFactory);
    }

    public R runOperation(final K key, final Callable<R> operation) throws ExecutionException {
        CallerRunsFuture<R> future = map.get(key);
        while (future == null) {
            map.putIfAbsent(key, futureFactory.get(operation));
            future = map.get(key);
        }
        try {
            return future.get();
        } finally {
            map.remove(key, future);
        }
    }

    static class CallerRunsFuture<T> extends FutureTask<T> {
        CallerRunsFuture(final Callable<T> callable) {
            super(callable);
        }

        @Override
        public T get() throws ExecutionException {
            run();
            try {
                return super.get();
            } catch (final InterruptedException e) {
                // /CLOVER:OFF
                // how to test?
                throw new RuntimeInterruptedException(e);
                // /CLOVER:ON
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
}
