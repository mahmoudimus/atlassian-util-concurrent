package com.atlassian.util.concurrent;

import net.javacrumbs.completionstage.CompletableCompletionStage;
import net.javacrumbs.completionstage.CompletionStageFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Promises {

  private Promises() {}

  public static <A> Promise<A> forEffect(@Nonnull final AsynchronousEffect<A> effect) {
    Objects.requireNonNull(effect, "AsynchronousEffect");
    return new OfStage<>(effect.getCompletionStage(), null);
  }

  public static <A> Promise<A> forCompletionStage(@Nonnull final CompletionStage<A> future) {
    Objects.requireNonNull(future, "CompletionStage");
    return new OfStage<>(future, null);
  }

  public static <A> Promise<A> forCompletionStage(@Nonnull final CompletionStage<A> future, @Nonnull final Executor executor) {
    Objects.requireNonNull(future, "CompletionStage");
    Objects.requireNonNull(executor, "Executor");
    return new OfStage<>(future, executor);
  }

  public static <B, A extends B> CompletableFuture<B> toCompletableFuture(@Nonnull final Promise<A> promise) {
    if (promise instanceof OfStage) {
      //shortcut
      return ((OfStage<B>)promise).future;
    } else {
      final CompletableFuture<B> aCompletableFuture = new CompletableFuture<>();
      promise.then(callback(aCompletableFuture::complete,
        t -> {
          if (promise.isCancelled() && (!(t instanceof CancellationException))) {
            aCompletableFuture.completeExceptionally(new CancellationException(t.getMessage()));
          } else {
            aCompletableFuture.completeExceptionally(getRealException(t));
          }
        }));
      return aCompletableFuture;
    }
  }

  public @SafeVarargs static <A> Promise<List<A>> when(Promise<? extends A> promise, Promise<? extends A>... promises) {
    final List<CompletableFuture<? extends A>> futures =
      Stream.concat(
        Stream.of(Promises.toCompletableFuture(promise)), Stream.of(promises).map(Promises::toCompletableFuture)
      ).collect(Collectors.toList());

    final CompletableFuture<?> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    // short-circuit: eagerly cancel the futures if any of the leaves fail. Do not wait for the others.
    futures.forEach(cf -> cf.whenComplete((a, t) -> {
        if (t != null) futures.forEach(f -> f.cancel(true));
      }));

    final CompletableCompletionStage<List<A>>  ccs = factory.createCompletionStage();
    allFutures.whenComplete((a, t) -> {
      if (t == null) {
        ccs.doComplete(futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
      } else {
        ccs.completeExceptionally(t);
      }
    });
    // if the returning promise is cancelled, eagerly cancel the futures.
    ccs.whenComplete((a, t) -> {
      if (t instanceof CancellationException) futures.forEach(f -> f.cancel(true));
    });
    return Promises.forCompletionStage(ccs);
  }

  /**
   * Creates a new, resolved promise for the specified concrete value.
   *
   * @return The new promise
   *
   * @since 2.7
   */
  public static <A> Function<A, Promise<A>> toPromise() {
    return new ToPromise<A>();
  }

  static class ToPromise<A> implements Function<A, Promise<A>> {
    @Override public Promise<A> apply(A a) {
      return promise(a);
    }
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
   * Creates a new, resolved promise for the specified concrete value.
   *
   * @param value The value for which a promise should be created
   * @return The new promise
   */
  public static <A> Promise<A> promise(A value) {
    final CompletableFuture<A> future = new CompletableFuture<>();
    future.complete(value);
    return Promises.forCompletionStage(future);
  }
  /**
   * Creates a new, rejected promise from the given {@link Throwable} and result
   * type.
   *
   * @param throwable The throwable
   * @return The new promise
   */
  public static <A> Promise<A> rejected(final Throwable throwable) {
    final CompletableFuture<A> future = new CompletableFuture<>();
    future.completeExceptionally(throwable);
    return Promises.forCompletionStage(future);
  }

  /**
   * Creates a new, rejected promise from the given Throwable and result type.
   * <p>
   * Synonym for {@link #rejected(Throwable)}
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
  public static <A> Promise<A> forFuture(Future<A> future, Executor executor) {
    final CompletableFuture<A> newFuture = new CompletableFuture<>();
    executor.execute(() -> {
      try {
        newFuture.complete(future.get());
      } catch (final ExecutionException ee) {
        newFuture.completeExceptionally(ee.getCause());
      } catch (final InterruptedException ee) {
        newFuture.cancel(true);
      } catch (final Throwable t) {
        newFuture.completeExceptionally(t);
      }
    });
    newFuture.whenComplete((a, t) -> {
      if (t instanceof CancellationException) {
        future.cancel(true);
      }
    });
    return forCompletionStage(newFuture, executor);
  }


  /**
   * Create a {@link Promise.Callback} by composing two Effects.
   *
   * @param success To run if the Future is successful
   * @param failure To run if the Future fails
   * @return The composed futureCallback
   */
  public static <A> Promise.Callback<A> callback(final Effect<? super A> success, final Effect<Throwable> failure) {
    return new Promise.Callback<A>() {
      public void onSuccess(A result) { success.apply(result); }
      public void onFailure(Throwable t) { failure.apply(t); }
    };
  }

  /**
   * Create a {@link Promise.Callback} from an Effect to be run if there is a
   * success.
   *
   * @param effect To be passed the produced value if it happens
   * @return The FutureCallback with a no-op onFailure
   */
  public static <A> Promise.Callback<A> onSuccessDo(final Effect<? super A> effect) {
    return callback(effect, Effects.<Throwable> noop());
  }

  /**
   * Create a {@link Promise.Callback} from an Effect to be run if there is a
   * failure.
   *
   * @param effect To be passed an exception if it happens
   * @return The FutureCallback with a no-op onSuccess
   */
  public static <A> Promise.Callback<A> onFailureDo(final Effect<Throwable> effect) {
    return callback(Effects.<A> noop(), effect);
  }

  static final class OfStage<A> implements Promise<A> {

    private final CompletableFuture<A> future;
    private final Executor executor;

    public OfStage(final CompletionStage<A> delegate, final Executor ex) {
      future = toCompletableFuture(delegate, ex);
      executor = ex;
    }

    @Override public A claim() {
      try {
        return future.get();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      } catch (CompletionException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
          throw (Error) cause;
        }
        throw e;
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

    @Override public Promise<A> done(final Effect<? super A> e) {
      final BiConsumer<A, Throwable> action = (a, t) -> {
        e.apply(a);
      };
      if (executor == null) {
        future.whenComplete(action);
      } else {
        future.whenCompleteAsync(action, executor);
      }
      return this;
    }

    @Override public Promise<A> fail(final Effect<Throwable> e) {
      final BiConsumer<A, Throwable> action = (a, t) -> {
        if (t != null) {
            e.apply(getRealException(t));
          }
       };
      if (executor == null) {
        future.whenComplete(action);
      } else {
        future.whenCompleteAsync(action, executor);
      }
      return this;
    }

    @Override public Promise<A> then(final Callback<? super A> callback) {
      final BiConsumer<A, Throwable> action = (a, t) -> {
        if (t == null) {
          callback.onSuccess(a);
        } else {
          callback.onFailure(getRealException(t));
        }
      };
      if (executor == null) {
        future.whenComplete(action);
      } else {
        future.whenCompleteAsync(action, executor);
      }
      return this;
    }

    @Override public <B> Promise<B> map(final Function<? super A, ? extends B> function) {
      return forCompletionStage(future.thenApply(function));
    }

    @Override public <B> Promise<B> flatMap(final Function<? super A, ? extends Promise<? extends B>> f) {
      final java.util.function.Function<A, ? extends CompletionStage<B>> fn = a -> Promises.toCompletableFuture(f.apply(a));
      if (executor == null) {
        return forCompletionStage(future.thenCompose(fn));
      } else {
        return forCompletionStage(future.thenComposeAsync(fn, executor));
      }
    }

    @Override public Promise<A> recover(final Function<Throwable, ? extends A> handleThrowable) {
      return forCompletionStage(future.exceptionally(handleThrowable.compose(Promises::getRealException)));
    }

    @Override public <B> Promise<B> fold(final Function<Throwable, ? extends B> ft,
                                         final Function<? super A, ? extends B> fa) {
      final BiFunction<A, Throwable, B> fn = (a, t) -> {
        if (t == null) {
          try {
            return fa.apply(a);
          } catch (final Throwable t2) {
            return ft.apply(t2);
          }
        }
        return ft.apply(getRealException(t));
      };
      if (executor == null) {
        return forCompletionStage(future.handle(fn));
      } else {
        return forCompletionStage(future.handleAsync(fn, executor));
      }
    }

    @Override public boolean cancel(final boolean mayInterruptIfRunning) {
      return future.cancel(mayInterruptIfRunning);
    }

    @Override public boolean isCancelled() {
      return future.isCancelled();
    }

    @Override public boolean isDone() {
      return future.isDone();
    }

    @Override public A get() throws InterruptedException, ExecutionException {
      return future.get();
    }

    @Override public A get(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
      return future.get(timeout, unit);
    }

    private CompletableFuture<A> toCompletableFuture(final CompletionStage<A> completionStage, final Executor executor) {
      try {
        return completionStage.toCompletableFuture();
      } catch (UnsupportedOperationException uoe) {
        final CompletableFuture<A> aCompletableFuture = new CompletableFuture<>();

        BiConsumer<A, Throwable> action = (a, t) -> {
          if (t == null) {
            aCompletableFuture.complete(a);
          } else {
            // If t is instance of CancellationException, aCompletableFuture will be set as cancelled.
            aCompletableFuture.completeExceptionally(getRealException(t));
          }
        };
        if (executor == null) {
          completionStage.whenComplete(action);
        } else {
          completionStage.whenCompleteAsync(action, executor);
        }
        return aCompletableFuture;
      }
    }
  }

  public static <A> AsynchronousEffect<A> newAsynchronousEffect() {
    return new AsynchronousEffect<>();
  }

  private static Throwable getRealException(@Nonnull final Throwable t) {
    if (t instanceof CompletionException) {
      return t.getCause();
    }
    return t;
  }

  private static final CompletionStageFactory factory = new CompletionStageFactory(null);

  public static final class AsynchronousEffect<A> {
    private final CompletableCompletionStage<A> simpleCompletionStage =
            factory.createCompletionStage();

    AsynchronousEffect() {};

    public void set(final A result) {
      simpleCompletionStage.complete(result);
    }
    public void exception(final Throwable t) {
      simpleCompletionStage.completeExceptionally(t);
    }
    CompletionStage<A> getCompletionStage() {
      return simpleCompletionStage;
    }
  }
}
