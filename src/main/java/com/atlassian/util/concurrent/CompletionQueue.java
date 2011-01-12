package com.atlassian.util.concurrent;

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Supplier;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Convenient encapsulation of {@link CompletionService} usage that allows a
 * collection of jobs to be issued to an {@link Executor} and return an
 * {@link Iterable} of the results that is in the order that the results return.
 */
public final class CompletionQueue {
    /**
     * Exception handling policies
     */
    enum Exceptions {
        IGNORE_EXCEPTIONS {
            @Override
            public <T> Function<Supplier<T>, Supplier<T>> handler() {
                return Functions.ignoreExceptions();
            }
        },
        THROW {
            @Override
            public <T> Function<Supplier<T>, Supplier<T>> handler() {
                return Functions.identity();
            }
        };
        abstract <T> Function<Supplier<T>, Supplier<T>> handler();
    }

    private CompletionQueue() {
        throw new AssertionError("Cannot instantiate");
    }

    /**
     * Convenience method for calling
     * {@link #completionQueue(Iterable, Executor, Exceptions)} where
     * {@link Exceptions#THROW exceptions are thrown}.
     */
    public static <T> Iterable<T> completionQueue(final Iterable<? extends Callable<T>> callables, final Executor executor) {
        return completionQueue(callables, executor, Exceptions.THROW);
    }

    /**
     * Queue the {@link Callable jobs} on the {@link Executor} and return a
     * lazily evaluated {@link Iterable} of the results in the order they return
     * in (fastest first).
     * 
     * @param <T> the result type
     * @param callables the jobs to run
     * @param executor the pool to run them on
     * @return an Iterable that returns the results in the order in which they
     * return
     */
    public static <T> Iterable<T> completionQueue(final Iterable<? extends Callable<T>> callables, final Executor executor, final Exceptions handle) {
        // we must copy the resulting Iterable<Suppliers> so each iterator
        // doesn't resubmit the jobs
        final Iterable<Supplier<T>> lazyAsyncSuppliers = copyOf(transform(callables, new AsyncCompletionFunction<T>(executor)));
        final Iterable<Supplier<T>> handled = transform(lazyAsyncSuppliers, handle.<T> handler());
        final Function<Supplier<? extends T>, T> fromSupplier = Functions.<T> fromSupplier();
        return transform(handled, fromSupplier);
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
        // the result gets memoized, so we only need one
        private final Supplier<T> nextCompleteItem = new Supplier<T>() {
            public T get() {
                try {
                    return completionService.take().get();
                } catch (final ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (final InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                }
            }
        };

        AsyncCompletionFunction(final Executor executor) {
            this.completionService = new ExecutorCompletionService<T>(executor);
        }

        public Supplier<T> apply(final Callable<T> task) {
            completionService.submit(task);
            // never call get twice as it gets a new element from the queue
            return memoize(nextCompleteItem);
        }
    }
}
