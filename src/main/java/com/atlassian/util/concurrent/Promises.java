package com.atlassian.util.concurrent;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.atlassian.util.concurrent.RuntimeInterruptedException;
import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Library of utility {@link Promise} functions
 * 
 * @since 1.2
 */
@Beta
public final class Promises {
  private Promises() {}

  /**
   * Returns a new {@link Promise} representing the status of a list of other
   * promises.
   * 
   * @param promises The promises that the new promise should track
   * @return The new, aggregate promise
   */
  public static <V> Promise<List<V>> when(Promise<? extends V>... promises) {
    return when(asList(promises));
  }

  /**
   * Returns a new {@link Promise} representing the status of a list of other
   * promises.
   * 
   * @param promises The promises that the new promise should track
   * @return The new, aggregate promise
   */
  public static <V> Promise<List<V>> when(Iterable<? extends Promise<? extends V>> promises) {
    return forListenableFuture(Futures.<V> allAsList(promises));
  }

  /**
   * Creates a new, resolved promise for the specified concrete value.
   * 
   * @param instance The value for which a promise should be created
   * @return The new promise
   */
  public static <V> Promise<V> ofInstance(V value) {
    return new Of<V>(Futures.immediateFuture(value));
  }

  /**
   * Creates a new, rejected promise from the given {@link Throwable} and result
   * type.
   * 
   * @param instance The throwable
   * @param resultType The result type
   * @return The new promise
   */
  public static <V> Promise<V> rejected(Throwable throwable, Class<V> resultType) {
    return new Of<V>(Futures.<V> immediateFailedFuture(throwable));
  }

  /**
   * Creates a promise from the given future.
   * 
   * @param future The future delegate for the new promise
   * @return The new promise
   */
  public static <V> Promise<V> forListenableFuture(ListenableFuture<V> future) {
    return new Of<V>(future);
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
      @Override
      public void apply(Throwable t) {
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
  public static <V> FutureCallback<V> futureCallback(final Effect<V> success, final Effect<Throwable> failure) {
    return new FutureCallback<V>() {
      @Override
      public void onSuccess(V result) {
        success.apply(result);
      }

      @Override
      public void onFailure(Throwable t) {
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
  public static <V> FutureCallback<V> onSuccessDo(final Effect<V> effect) {
    return futureCallback(effect, Effects.<Throwable> noop());
  }

  /**
   * Create a {@link FutureCallback} from an Effect to be run if there is a
   * failure.
   * 
   * @param effect To be passed an exception if it happens
   * @return The FutureCallback with a no-op onSuccess
   */
  public static <V> FutureCallback<V> onFailureDo(final Effect<Throwable> effect) {
    return futureCallback(Effects.<V> noop(), effect);
  }

  private static final class Of<V> extends ForwardingListenableFuture.SimpleForwardingListenableFuture<V> implements
    Promise<V> {
    public Of(ListenableFuture<V> delegate) {
      super(delegate);
    }

    @Override
    public V claim() {
      try {
        return delegate().get();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
          throw (Error) cause;
        }
        throw new RuntimeException(cause);
      }
    }

    @Override
    public Promise<V> then(FutureCallback<V> callback) {
      Futures.addCallback(delegate(), callback);
      return this;
    }

    @Override
    public <O> Promise<O> map(Function<? super V, ? extends O> function) {
      return forListenableFuture(Futures.transform(this, function));
    }

    @Override
    public <T> Promise<T> flatMap(final Function<? super V, Promise<T>> function) {
      final SettableFuture<T> result = SettableFuture.create();
      final Effect<Throwable> fail = reject(result);
      then(futureCallback(new Effect<V>() {
        public void apply(V v) {
          function.apply(v).then(futureCallback(new Effect<T>() {
            public void apply(T t) {
              result.set(t);
            }
          }, fail));
        }
      }, fail));
      return new Of<T>(result);
    }
  }
}
