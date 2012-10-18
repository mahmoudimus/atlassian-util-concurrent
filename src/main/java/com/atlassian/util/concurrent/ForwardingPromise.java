package com.atlassian.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.FutureCallback;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A promise which forwards all its method calls to another promise. Subclasses should override one or more methods to
 * modify the behavior of the backing cache as desired per the
 * <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @since 2.4
 */
@Beta
public abstract class ForwardingPromise<A> extends ForwardingObject implements Promise<A> {
    /**
     * Constructor for use by subclasses.
     */
    protected ForwardingPromise() {}

    @Override
    protected abstract Promise<A> delegate();

    @Override
    public A claim() {
        return delegate().claim();
    }

    @Override
    public Promise<A> done(Effect<A> e) {
        delegate().done(e);
        return this;
    }

    @Override
    public Promise<A> fail(Effect<Throwable> e) {
        delegate().fail(e);
        return this;
    }

    @Override
    public Promise<A> then(FutureCallback<A> callback) {
        delegate().then(callback);
        return this;
    }

    @Override
    public <B> Promise<B> map(Function<? super A, ? extends B> function) {
        return delegate().map(function);
    }

    @Override
    public <B> Promise<B> flatMap(Function<? super A, Promise<B>> function) {
        return delegate().flatMap(function);
    }

    @Override
    public Promise<A> recover(Function<Throwable, ? extends A> handleThrowable) {
        return delegate().recover(handleThrowable);
    }

    @Override
    public <B> Promise<B> fold(Function<Throwable, ? extends B> handleThrowable, Function<? super A, ? extends B> function) {
        return delegate().fold(handleThrowable, function);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        delegate().addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean b) {
        return delegate().cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return delegate().isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate().isDone();
    }

    @Override
    public A get() throws InterruptedException, ExecutionException {
        return delegate().get();
    }

    @Override
    public A get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate().get(l, timeUnit);
    }
}