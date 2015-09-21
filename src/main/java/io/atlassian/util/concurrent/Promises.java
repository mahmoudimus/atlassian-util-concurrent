/**
 * Copyright 2015 Atlassian Pty Ltd
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
package io.atlassian.util.concurrent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Promises {

  private Promises() {}

  /**
   * Create a promise for the input effect.
   *
   * @param <A> type of the successful value.
   * @return a new {@link SettablePromise} that can be fulfilled by calling the
   * {@link Callback} methods it implements. Cancelling this Promise will have
   * no consequence on the code that calls the callback.
   */
  public static <A> SettablePromise<A> settablePromise() {
    return settablePromise(Optional.empty());
  }

  /**
   * @param <A> type of the successful value.
   * @param executor to be called to run callbacks and transformations attached
   * to the returned Promise. If None, they will be executed on the caller
   * thread.
   * @return a new {@link SettablePromise} that can be fulfilled by calling the
   * {@link Callback} methods it implements. Cancelling this Promise will have
   * no consequence on the code that calls the callback.
   */
  public static <A> SettablePromise<A> settablePromise(@Nonnull final Optional<Executor> executor) {
    Objects.requireNonNull(executor, "Executor");
    return new Settable<>(executor);
  }

  /**
   * Create a promise from the input completion stage.
   *
   * @param stage a {@link java.util.concurrent.CompletionStage} used as the
   * precedent of the returned Promise.
   * @param <A> type of the successful value.
   * @return a new {@link io.atlassian.util.concurrent.Promise} that will be
   * fulfilled with the same result that the CompletionStage has. If it
   * implements
   * {@link java.util.concurrent.CompletionStage#toCompletableFuture()} then
   * cancelling this Promise will cancel that CompletableFuture.
   */
  public static <A> Promise<A> forCompletionStage(@Nonnull final CompletionStage<A> stage) {
    return forCompletionStage(stage, Optional.empty());
  }

  /**
   * Create a promise from the input completion stage.
   *
   * @param stage a {@link java.util.concurrent.CompletionStage} used as the
   * precedent of the returned Promise.
   * @param <A> type of the successful value.
   * @param executor to be called to run callbacks and transformations attached
   * to the returned Promise. If None, they will be executed on the caller
   * thread.
   * @return a new {@link io.atlassian.util.concurrent.Promise} that will be
   * fulfilled with the same result that the CompletionStage has. If it
   * implements
   * {@link java.util.concurrent.CompletionStage#toCompletableFuture()} then
   * cancelling this Promise will cancel that CompletableFuture.
   */
  public static <A> Promise<A> forCompletionStage(@Nonnull final CompletionStage<A> stage, @Nonnull final Optional<Executor> executor) {
    Objects.requireNonNull(stage, "CompletionStage");
    Objects.requireNonNull(executor, "Executor");
    return new OfStage<>(stage, executor);
  }

  /**
   * Return a completable future based on the input promise
   *
   * @param promise a {@link io.atlassian.util.concurrent.Promise} used as the
   * precedent of the returned CompletableFuture.
   * @param <B> any super type of A that may be inferred.
   * @return a new {@link java.util.concurrent.CompletableFuture} that will be
   * fulfilled with the same result that the Promise has.
   */
  public static <B, A extends B> CompletableFuture<B> toCompletableFuture(@Nonnull final Promise<A> promise) {
    if (promise instanceof OfStage) {
      // shortcut
      return ((OfStage<B>) promise).future;
    } else {
      final CompletableFuture<B> aCompletableFuture = new CompletableFuture<>();
      promise.then(compose(aCompletableFuture::complete, t -> {
        if (promise.isCancelled() && (!(t instanceof CancellationException))) {
          aCompletableFuture.completeExceptionally(new CancellationException(t.getMessage()));
        } else {
          aCompletableFuture.completeExceptionally(getRealException(t));
        }
      }));
      return aCompletableFuture;
    }
  }

  /**
   * Returns a new {@link io.atlassian.util.concurrent.Promise} representing the
   * status of a list of other promises.
   *
   * @param promises The promises that the new promise should track
   * @return The new, aggregate promise
   * @param <A> an A.
   */
  public @SafeVarargs static <A> Promise<List<A>> when(@Nonnull final Promise<? extends A>... promises) {
    return when(Stream.of(promises));
  }

  /**
   * Returns a new {@link io.atlassian.util.concurrent.Promise} representing the
   * status of a list of other promises. More generally this is known as
   * {code}sequence{code} as both List and Promise are traversable monads.
   *
   * @param promises The promises that the new promise should track
   * @return The new, aggregate promise
   * @param <A> an A.
   */
  public static <A> Promise<List<A>> when(@Nonnull final Iterable<? extends Promise<? extends A>> promises) {
    return when(StreamSupport.stream(promises.spliterator(), false).map(Function.identity()));
  }

  /**
   * Returns a new {@link io.atlassian.util.concurrent.Promise} representing the
   * status of a stream of other promises.
   *
   * @param promises The promises that the new promise should track
   * @return The new, aggregate promise
   * @param <A> an A.
   */
  public static <A> Promise<List<A>> when(@Nonnull final Stream<? extends Promise<? extends A>> promises) {
    final List<CompletableFuture<? extends A>> futures = promises.map(Promises::toCompletableFuture).collect(Collectors.toList());

    final CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    // short-circuit: eagerly cancel the futures if any of the leaves fail. Do
    // not wait for the others.
    futures.forEach(cf -> cf.whenComplete((a, t) -> {
      if (t != null)
        futures.forEach(f -> f.cancel(true));
    }));
    final Function<Void, List<A>> gatherValues = o -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    return Promises.forCompletionStage(allFutures.thenApply(gatherValues));
  }

  /**
   * Creates a new, resolved promise for the specified concrete value.
   *
   * @return The new promise
   * @since 2.7
   *
   * @deprecated use {@link Promises#promise(Object)} as a method reference.
   */
  public @Deprecated static <A> Function<A, Promise<A>> toPromise() {
    return Promises::promise;
  }

  /**
   * Creates a new, resolved promise for the specified concrete value.
   *
   * @param value The value for which a promise should be created
   * @return The new promise
   * @param <A> an A.
   */
  public static <A> Promise<A> promise(final A value) {
    final CompletableFuture<A> future = new CompletableFuture<>();
    future.complete(value);
    return Promises.forCompletionStage(future);
  }

  /**
   * Creates a new, rejected promise from the given {@link java.lang.Throwable}
   * and result type.
   *
   * @param t The throwable
   * @return The new promise
   * @param <A> an A.
   */
  public static <A> Promise<A> rejected(@Nonnull final Throwable t) {
    final CompletableFuture<A> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return Promises.forCompletionStage(future);
  }

  /**
   * Creates a promise from the given future.
   *
   * @param future The future delegate for the new promise
   * @param executor an executor where a task will run that waits for the future
   * to complete
   * @param <A> possible future value type
   * @return The new promise
   */
  public static <A> Promise<A> forFuture(@Nonnull final Future<A> future, @Nonnull final Executor executor) {
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
    return forCompletionStage(newFuture, Optional.of(executor));
  }

  /**
   * Create a {@link Promise.TryConsumer} by composing two {@link Consumer}.
   *
   * @param success To run if the Future is successful
   * @param failure To run if the Future fails
   * @return The composed Callback
   * @param <A> an A.
   */
  public static <A> Promise.TryConsumer<A> compose(@Nonnull final Consumer<? super A> success, @Nonnull final Consumer<Throwable> failure) {
    return new Promise.TryConsumer<A>() {
      public void accept(final A result) {
        success.accept(result);
      }

      public void fail(@Nonnull final Throwable t) {
        failure.accept(t);
      }
    };
  }

  static class OfStage<A> implements Promise<A> {

    private final CompletableFuture<A> future;
    private final Optional<Executor> executor;

    public OfStage(@Nonnull final CompletionStage<A> delegate, @Nonnull final Optional<Executor> ex) {
      future = buildCompletableFuture(delegate, ex);
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

    @Override public Promise<A> done(final Consumer<? super A> e) {
      return then(e, t -> {});
    }

    @Override public Promise<A> fail(final Consumer<Throwable> e) {
      return then(a -> {}, e);
    }

    @Override public Promise<A> then(final TryConsumer<? super A> callback) {
      return then(callback::accept, callback::fail);
    }

    @Override public <B> Promise<B> map(final Function<? super A, ? extends B> function) {
      return forCompletionStage(future.thenApply(function));
    }

    @Override public <B> Promise<B> flatMap(final Function<? super A, ? extends Promise<? extends B>> f) {
      final Function<A, CompletableFuture<B>> fn = a -> Promises.toCompletableFuture(f.apply(a));
      return this.<Function<A, ? extends CompletionStage<B>>, B> newPromise(future::thenCompose, future::thenComposeAsync).apply(fn);
    }

    @Override public Promise<A> recover(final Function<Throwable, ? extends A> handleThrowable) {
      return forCompletionStage(future.exceptionally(handleThrowable.compose(Promises::getRealException)));
    }

    @Override public <B> Promise<B> fold(final Function<Throwable, ? extends B> ft, final Function<? super A, ? extends B> fa) {
      final Function<? super A, ? extends B> fn = a -> {
        try {
          return fa.apply(a);
        } catch (final Throwable t) {
          return ft.apply(t);
        }
      };
      return this.<BiFunction<A, Throwable, B>, B> newPromise(future::handle, future::handleAsync).apply(biFunction(fn, ft));
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

    @Override public A get(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return future.get(timeout, unit);
    }

    private Promise<A> then(final Consumer<? super A> onSuccess, final Consumer<Throwable> onFailure) {
      return this.newPromise(future::whenComplete, future::whenCompleteAsync).apply(biConsumer(onSuccess, onFailure));
    }

    private <I, O> Function<I, Promise<O>> newPromise(final Function<I, CompletionStage<O>> f1, final BiFunction<I, Executor, CompletionStage<O>> f2) {
      return i -> {
        if (executor.isPresent()) {
          return forCompletionStage(f2.apply(i, executor.get()));
        } else {
          return forCompletionStage(f1.apply(i));
        }
      };
    }

    private CompletableFuture<A> buildCompletableFuture(final CompletionStage<A> completionStage, final Optional<Executor> executor) {
      try {
        return completionStage.toCompletableFuture();
      } catch (final UnsupportedOperationException uoe) {
        final CompletableFuture<A> aCompletableFuture = new CompletableFuture<>();

        BiConsumer<A, Throwable> action = biConsumer(aCompletableFuture::complete, aCompletableFuture::completeExceptionally);
        if (executor.isPresent()) {
          completionStage.whenCompleteAsync(action, executor.get());
        } else {
          completionStage.whenComplete(action);
        }
        return aCompletableFuture;
      }
    }
  }

  static class Settable<A> extends OfStage<A> implements SettablePromise<A> {
    private final CompletableFuture<A> completableFuture;

    public Settable(@Nonnull final Optional<Executor> ex) {
      this(new CompletableFuture<>(), ex);
    }

    private Settable(@Nonnull final CompletableFuture<A> cf, @Nonnull final Optional<Executor> ex) {
      super(cf, ex);
      completableFuture = cf;
    }

    public void set(final A result) {
      completableFuture.complete(result);
    }

    public void exception(@Nonnull final Throwable t) {
      completableFuture.completeExceptionally(t);
    }
  }

  private static Throwable getRealException(@Nonnull final Throwable t) {
    if (t instanceof CompletionException) {
      return t.getCause();
    }
    return t;
  }

  private static <A, B> BiFunction<A, Throwable, B> biFunction(final Function<? super A, ? extends B> f, final Function<Throwable, ? extends B> ft) {
    return (a, t) -> {
      if (t == null) {
        return f.apply(a);
      } else {
        return ft.apply(getRealException(t));
      }
    };
  }

  private static <A> BiConsumer<A, Throwable> biConsumer(final Consumer<? super A> c, final Consumer<Throwable> ct) {
    return (a, t) -> {
      if (t == null) {
        c.accept(a);
      } else {
        ct.accept(getRealException(t));
      }
    };
  }

  /**
   * A callback that can be completed with a successful value or a failed
   * exception.
   * 
   * @param <A> type of the successful value.
   */
  public interface Callback<A> {
    void set(A result);

    void exception(@Nonnull Throwable t);
  }

  /**
   * A promise that can be completed with a successful value or a failed
   * exception.
   * 
   * @param <A> type of the successful value.
   */
  public interface SettablePromise<A> extends Promise<A>, Callback<A> {}

}
