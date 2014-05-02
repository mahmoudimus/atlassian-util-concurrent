/**
 * Copyright 2012 Atlassian Pty Ltd 
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

import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Library of utility {@link Promise} functions
 * 
 * @since 2.4
 */
@Beta public final class Promises {
  private Promises() {}

  /**
   * Returns a new {@link Promise} representing the status of a list of other
   * promises.
   * 
   * @param promises The promises that the new promise should track
   * @return The new, aggregate promise
   */
  public static <A> Promise<List<A>> when(Promise<? extends A>... promises) {
    return when(asList(promises));
  }

  /**
   * Returns a new {@link Promise} representing the status of a list of other
   * promises. More generally this is known as {code}sequence{code} as both List
   * and Promise are traversable monads.
   * 
   * @param promises The promises that the new promise should track
   * @return The new, aggregate promise
   */
  public static <A> Promise<List<A>> when(Iterable<? extends Promise<? extends A>> promises) {
    return forListenableFuture(Futures.<A> allAsList(promises));
  }

  /**
   * Creates a new, resolved promise for the specified concrete value.
   * 
   * @param value The value for which a promise should be created
   * @return The new promise
   */
  public static <A> Promise<A> promise(A value) {
    return new Of<A>(Futures.immediateFuture(value));
  }

  /**
   * Creates a new, resolved promise for the specified concrete value.
   * <p>
   * Synonym for {@link #promise(Object)}.
   * 
   * @param value The value for which a promise should be created
   * @return The new promise
   */
  public static <A> Promise<A> toResolvedPromise(A value) {
    return promise(value);
  }

  /**
   * Creates a new, rejected promise from the given {@link Throwable} and result
   * type.
   * 
   * @param throwable The throwable
   * @param resultType The result type
   * @return The new promise
   * @deprecated. Use {@link #rejected(Throwable)}
   */
  public static <A> Promise<A> rejected(Throwable throwable, Class<A> resultType) {
    return rejected(throwable);
  }

  /**
   * Creates a new, rejected promise from the given {@link Throwable} and result
   * type.
   *
   * @param throwable The throwable
   * @return The new promise
   */
  public static <A> Promise<A> rejected(Throwable throwable) {
    return new Of<A>(Futures.<A> immediateFailedFuture(throwable));
  }

  /**
   * Creates a new, rejected promise from the given Throwable and result type.
   * <p>
   * Synonym for {@link #rejected(Throwable, Class)}
   * 
   * @param t The throwable
   * @param resultType The result type
   * @return The new promise
   * @deprecated Use {@link #toRejectedPromise(Throwable)}
   */
  public static <A> Promise<A> toRejectedPromise(Throwable t, Class<A> resultType) {
    return rejected(t);
  }

  /**
   * Creates a new, rejected promise from the given Throwable and result type.
   * <p>
   * Synonym for {@link #rejected(Throwable, Class)}
   *
   * @param t The throwable
   * @return The new promise
   */
  public static <A> Promise<A> toRejectedPromise(Throwable t) {
    return rejected(t);
  }

  /**
   * Creates a promise from the given future.
   * 
   * @param future The future delegate for the new promise
   * @return The new promise
   */
  public static <A> Promise<A> forListenableFuture(ListenableFuture<A> future) {
    return new Of<A>(future);
  }

  /**
   * Creates a promise from the given future.
   * 
   * @param future The future delegate for the new promise
   * @return The new promise
   */
  public static <A> Promise<A> forFuture(Future<A> future) {
    return new Of<A>(JdkFutureAdapters.listenInPoolThread(future));
  }

  /**
   * Creates a new {@link Effect} that forwards a promise's fail events to the
   * specified future delegate's {@link SettableFuture#setException(Throwable)}
   * method -- that is, the new callback rejects the delegate future if invoked.
   * 
   * @param delegate The future to be rejected on a fail event
   * @return The fail callback
   */
  public static Effect<Throwable> reject(final SettableFuture<?> delegate) {
    return new Effect<Throwable>() {
      @Override public void apply(Throwable t) {
        delegate.setException(t);
      }
    };
  }

  /**
   * Create a {@link FutureCallback} by composing two Effects.
   * 
   * @param success To run if the Future is successful
   * @param failure To run if the Future fails
   * @return The composed futureCallback
   */
  public static <A> FutureCallback<A> futureCallback(final Effect<? super A> success, final Effect<Throwable> failure) {
    return new FutureCallback<A>() {
      @Override public void onSuccess(A result) {
        success.apply(result);
      }

      @Override public void onFailure(Throwable t) {
        failure.apply(t);
      }
    };
  }

  /**
   * Create a {@link FutureCallback} from an Effect to be run if there is a
   * success.
   * 
   * @param effect To be passed the produced value if it happens
   * @return The FutureCallback with a no-op onFailure
   */
  public static <A> FutureCallback<A> onSuccessDo(final Effect<? super A> effect) {
    return futureCallback(effect, Effects.<Throwable> noop());
  }

  /**
   * Create a {@link FutureCallback} from an Effect to be run if there is a
   * failure.
   * 
   * @param effect To be passed an exception if it happens
   * @return The FutureCallback with a no-op onSuccess
   */
  public static <A> FutureCallback<A> onFailureDo(final Effect<Throwable> effect) {
    return futureCallback(Effects.<A> noop(), effect);
  }

  private static final class Of<A> extends SimpleForwardingListenableFuture<A> implements Promise<A> {
    public Of(ListenableFuture<A> delegate) {
      super(delegate);
    }

    @Override public A claim() {
      try {
        return delegate().get();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      } catch (ExecutionException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
          throw (Error) cause;
        }
        throw new RuntimeException(cause);
      }
    }

    @Override public Promise<A> done(Effect<? super A> e) {
      then(onSuccessDo(e));
      return this;
    }

    @Override public Promise<A> fail(Effect<Throwable> e) {
      then(Promises.<A> onFailureDo(e));
      return this;
    }

    @Override public Promise<A> then(FutureCallback<? super A> callback) {
      Futures.addCallback(delegate(), callback);
      return this;
    }

    @Override public <B> Promise<B> map(Function<? super A, ? extends B> function) {
      return forListenableFuture(Futures.transform(this, function));
    }

    @Override public <B> Promise<B> flatMap(final Function<? super A, ? extends Promise<? extends B>> f) {
      final SettableFuture<B> result = SettableFuture.create();
      final Effect<Throwable> failResult = reject(result);
      done(new Effect<A>() {
        @Override public void apply(A v) {
          try {
            @SuppressWarnings("unchecked") Promise<B> next = (Promise<B>) f.apply(v);
            next.done(new Effect<B>() {
              @Override public void apply(B t) {
                result.set(t);
              }
            }).fail(failResult);
          } catch (Throwable t) {
            result.setException(t);
          }
        }
      }).fail(failResult);
      return new Of<B>(result);
    }

    @Override public Promise<A> recover(Function<Throwable, ? extends A> handleThrowable) {
      return this.fold(handleThrowable, Functions.<A> identity());
    }

    @Override public <B> Promise<B> fold(final Function<Throwable, ? extends B> ft, final Function<? super A, ? extends B> fa) {
      final SettableFuture<B> result = SettableFuture.create();
      final Effect<Throwable> error = new Effect<Throwable>() {
        @Override public void apply(Throwable t) {
          try {
            result.set(ft.apply(t));
          } catch (Throwable inner) {
            result.setException(inner);
          }
        }
      };
      done(new Effect<A>() {
        public void apply(A a) {
          try {
            result.set(fa.apply(a));
          } catch (Throwable t) {
            error.apply(t);
          }
        }
      }).fail(error);
      return new Of<B>(result);
    }
  }
}
