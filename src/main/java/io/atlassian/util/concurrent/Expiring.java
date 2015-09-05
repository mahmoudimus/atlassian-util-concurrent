package io.atlassian.util.concurrent;

import io.atlassian.util.concurrent.atomic.AtomicReference;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A Reference that can expire based on some strategy.
 * <p>
 * The strategy is a continuous supply of Predicates that will remain true until
 * the reference has expired, from which point they should always fail. A new
 * reference will then be
 * 
 * @param <T> the type of reference held.
 * 
 * @since 2.1
 */
final class Expiring<T> implements Supplier<T> {
  private final AtomicReference<Alive<T>> r = new AtomicReference<Alive<T>>(Dead.<T> instance());
  private final Supplier<T> factory;
  private final Supplier<Predicate<Void>> strategy;

  Expiring(final Supplier<T> factory, final Supplier<Predicate<Void>> strategy) {
    this.factory = requireNonNull(factory);
    this.strategy = requireNonNull(strategy);
  }

  @Override public T get() {
    int i = 0;
    while (true) {
      final Alive<T> e = r.get();
      if (e.alive()) {
        return e.get();
      }
      if (i++ > 100) {
        // infinite loop detection, must halt
        throw new AssertionError(100 + " attempts to CAS update the next value, aborting!");
      }
      r.compareAndSet(e, new Value());
    }
  }

  //
  // inner classes
  //

  /**
   * Get a value and let us know whether it should still be current/alive.
   */
  interface Alive<T> extends Supplier<T> {
    boolean alive();
  }

  /**
   * Holds a value and the liveness predicate.
   * <p>
   * Lazily computes the value so the construction is cheap and fast.
   */
  final class Value extends LazyReference<T> implements Alive<T> {
    final Predicate<Void> alive = requireNonNull(strategy.get());

    @Override public boolean alive() {
      return alive.test(null);
    }

    @Override public T create() {
      return factory.get();
    }
  }

  /**
   * Initial state is dead.
   */
  enum Dead implements Alive<Object> {
    DEAD;

    public boolean alive() {
      return false;
    }

    @Override public Object get() {
      throw new UnsupportedOperationException("dead");
    }

    static <T> Alive<T> instance() {
      @SuppressWarnings("unchecked")
      final Alive<T> result = (Alive<T>) DEAD;
      return result;
    }
  }
}
