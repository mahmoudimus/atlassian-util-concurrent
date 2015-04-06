package com.atlassian.util.concurrent;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;

public class ForwardingPromiseTest {
  private final Promise<Object> promise = Promises.promise(new Object());

  private final ForwardingPromise<Object> forwardingPromise = new ForwardingPromise<Object>() {
    @Override protected Promise<Object> delegate() {
      return promise;
    }
  };

  <A> Effect<A> doNothing(Class<A> c) {
    return a -> {};
  }

  @Test public void testDoneReturnsThis() {
    assertSame(forwardingPromise, forwardingPromise.done(doNothing(Object.class)));
  }

  @Test public void testFailReturnsThis() {
    assertSame(forwardingPromise, forwardingPromise.fail(doNothing(Throwable.class)));
  }

  @Test public void testThenReturnsThis() {
    assertSame(forwardingPromise, forwardingPromise.then(new FutureCallback<Object>() {
      @Override public void onSuccess(Object result) {}

      @Override public void onFailure(Throwable t) {}
    }));
  }
}
