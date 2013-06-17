package com.atlassian.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;

/**
 * A promise which forwards all its method calls to another promise. Subclasses
 * should override one or more methods to modify the behavior of the backing
 * Promise as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 * 
 * @since 2.4
 */
@Beta public abstract class ForwardingPromise<A> extends ForwardingListenableFuture<A> implements Promise<A> {
  /**
   * Constructor for use by subclasses.
   */
  protected ForwardingPromise() {}

  @Override protected abstract Promise<A> delegate();

  @Override public A claim() {
    return delegate().claim();
  }

  @Override public Promise<A> done(Effect<? super A> e) {
    delegate().done(e);
    return this;
  }

  @Override public Promise<A> fail(Effect<Throwable> e) {
    delegate().fail(e);
    return this;
  }

  @Override public Promise<A> then(FutureCallback<? super A> callback) {
    delegate().then(callback);
    return this;
  }

  @Override public <B> Promise<B> map(Function<? super A, ? extends B> function) {
    return delegate().map(function);
  }

  @Override public <B> Promise<B> flatMap(Function<? super A, Promise<B>> function) {
    return delegate().flatMap(function);
  }

  @Override public Promise<A> recover(Function<Throwable, ? extends A> handleThrowable) {
    return delegate().recover(handleThrowable);
  }

  @Override public <B> Promise<B> fold(Function<Throwable, ? extends B> handleThrowable, Function<? super A, ? extends B> function) {
    return delegate().fold(handleThrowable, function);
  }
}