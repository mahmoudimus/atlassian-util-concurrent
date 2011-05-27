/**
 * Copyright 2011 Atlassian Pty Ltd 
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

import static com.atlassian.util.concurrent.Assertions.notNull;
import static com.atlassian.util.concurrent.Timeout.getNanosTimeout;
import static com.google.common.base.Functions.identity;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import net.jcip.annotations.ThreadSafe;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Callables;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Convenient encapsulation of {@link CompletionService} usage that allows a
 * collection of jobs to be issued to an {@link Executor} and return an
 * {@link Iterable} of the results that is in the order that the results return.
 * <p>
 * Unlike {@link ExecutorService#invokeAll(java.util.Collection)}
 * {@link #invokeAll(Iterable)} here does not itself block, rather the
 * {@link Iterator#next()} calls to the returned {@link Iterable} will block the
 * first time it is iterated. This allows the client to defer the reification of
 * the result until it is ready to use it.
 * <p>
 * To create an instance of this class, please use the supplied {@link Builder}.
 * 
 * @since 1.0
 */
@ThreadSafe
public final class AsyncCompleter {
    private final Executor executor;
    private final Exceptions policy;

    AsyncCompleter(final Executor executor, final Exceptions policy) {
        this.executor = notNull("executor", executor);
        this.policy = notNull("policy", policy);
    }

    /**
     * Queue the {@link Callable jobs} on the contained {@link Executor} and
     * return a lazily evaluated {@link Iterable} of the results in the order
     * they return in (fastest first).
     * <p>
     * Note that if any of the jobs return null then nulls WILL BE included in
     * the results. Similarly if an exception is thrown and exceptions are being
     * ignored then there will be a NULL result returned. If you want to filter
     * nulls this is trivial, but be aware that filtering of the results forces
     * {@link Iterator#next()} to be called while calling
     * {@link Iterator#hasNext()} (which may block).
     * 
     * @param <T> the result type
     * @param callables the jobs to run
     * @return an Iterable that returns the results in the order in which they
     * return, may return nulls.
     */
    public <T> Iterable<T> invokeAll(final Iterable<? extends Callable<T>> callables) {
        return invokeAllTasks(callables, new BlockingAccessor<T>());
    }

    /**
     * Version of {@link #invokeAll(Iterable)} that supports a timeout. Any jobs
     * that are not complete by the timeout are discarded.
     * 
     * @param <T> the result type
     * @param callables the jobs to run
     * @param time the max time spent per job, in milliseconds
     * @param unit the TimeUnit time is specified in
     * @return an Iterable that returns the results in the order in which they
     * return, may return nulls.
     * 
     * @see #invokeAll(Iterable)
     * @since 2.1
     */
    public <T> Iterable<T> invokeAll(final Iterable<? extends Callable<T>> callables, final long time, final TimeUnit unit) {
        return invokeAllTasks(callables, new TimeoutAccessor<T>(getNanosTimeout(time, unit)));
    }

    /**
     * Implementation for the invokeAll methods, needs to be passed an accessor
     * function that is responsible for getting things from the
     * CompletionService.
     */
    <T> Iterable<T> invokeAllTasks(final Iterable<? extends Callable<T>> callables, final Function<CompletionService<T>, T> accessor) {
        // we must copy the resulting Iterable<Supplier> so
        // each iteration doesn't resubmit the jobs
        final Iterable<Supplier<T>> lazyAsyncSuppliers = copyOf(transform(callables, new AsyncCompletionFunction<T>(executor, accessor)));
        final Iterable<Supplier<T>> handled = transform(lazyAsyncSuppliers, policy.<T> handler());
        return filter(transform(handled, Functions.<T> fromSupplier()), notNull());
    }

    /**
     * For creating instances of a {@link AsyncCompleter}.
     */
    public static class Builder {
        Executor executor;
        Exceptions policy = Exceptions.THROW;

        /**
         * Create a Builder with the supplied Executor
         * 
         * @param executor
         */
        public Builder(@NotNull final Executor executor) {
            this.executor = notNull("executor", executor);
        }

        /**
         * Ignore exceptions thrown by any {@link Callables}, note will cause
         * nulls in the resulting iterable!
         */
        public Builder ignoreExceptions() {
            return handleExceptions(Exceptions.IGNORE_EXCEPTIONS);
        }

        public Builder handleExceptions(final Exceptions policy) {
            this.policy = policy;
            return this;
        }

        /**
         * Create a {@link AsyncCompleter} that limits the number of jobs
         * executed to the underlying executor to a hard limit.
         * <p>
         * Note: this only makes sense if the underlying executor does not have
         * a limit on the number of threads it will create, or the limit is much
         * higher than this limit.
         * 
         * @param limit the number of parallel jobs to execute at any one time
         * @see LimitedExecutor for more discussion of how this limit is
         * relevant
         */
        public AsyncCompleter limitParallelExecutionTo(final int limit) {
            return new AsyncCompleter(new LimitedExecutor(executor, limit), policy);
        }

        public AsyncCompleter build() {
            return new AsyncCompleter(executor, policy);
        }
    }

    /**
     * Exception handling policies
     */
    public enum Exceptions {
        IGNORE_EXCEPTIONS {
            @Override
            public <T> Function<Supplier<T>, Supplier<T>> handler() {
                return Functions.<T> ignoreExceptions();
            }
        },
        THROW {
            @Override
            public <T> Function<Supplier<T>, Supplier<T>> handler() {
                return identity();
            }
        };
        abstract <T> Function<Supplier<T>, Supplier<T>> handler();
    }

    /**
     * Function for submitting {@link Callable} instances to an executor and
     * asynchronously waiting for the first result. Instances of this should not
     * be shared as each has its own {@link CompletionService} instance (and
     * therefore its own queue) so anything subsequently submitted to this
     * Function may end up as the result of the supplier.
     * 
     * @param <T> the result type.
     */
    private static class AsyncCompletionFunction<T> implements Function<Callable<T>, Supplier<T>> {
        private final CompletionService<T> completionService;
        private final Function<CompletionService<T>, T> accessor;

        // the result gets memoized, so we only need one
        private final Supplier<T> nextCompleteItem = new Supplier<T>() {
            public T get() {
                return accessor.apply(completionService);
            }
        };

        AsyncCompletionFunction(final Executor executor, final Function<CompletionService<T>, T> accessor) {
            this.completionService = new ExecutorCompletionService<T>(executor);
            this.accessor = accessor;
        }

        public Supplier<T> apply(final Callable<T> task) {
            completionService.submit(task);
            // never call get twice as it gets a new element from the queue
            return memoize(nextCompleteItem);
        }
    }

    static final class TimeoutAccessor<T> implements Function<CompletionService<T>, T> {
        private final Timeout timeout;

        TimeoutAccessor(final Timeout timeout) {
            this.timeout = timeout;
        }

        @Override
        public T apply(final CompletionService<T> completionService) {
            try {
                final Future<T> future = completionService.poll(timeout.getTime(), timeout.getUnit());
                if (future == null) {
                    throw timeout.getTimeoutException();
                }
                return future.get();
            } catch (final InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            } catch (final ExecutionException e) {
                throw new RuntimeExecutionException(e);
            }
        }
    }

    static final class BlockingAccessor<T> implements Function<CompletionService<T>, T> {
        @Override
        public T apply(final CompletionService<T> completionService) {
            try {
                return completionService.take().get();
            } catch (final InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            } catch (final ExecutionException e) {
                throw new RuntimeExecutionException(e);
            }
        }
    }
}
