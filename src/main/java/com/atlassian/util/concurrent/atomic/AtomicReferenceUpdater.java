package com.atlassian.util.concurrent.atomic;

import static com.atlassian.util.concurrent.Assertions.notNull;

import com.atlassian.util.concurrent.Function;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <strong>Experimental</strong>. Please note this class is experimental and may
 * be removed in later versions unless you <i>really, really</i> like it. If you
 * do, mail me, jed@atlassian.com
 * <p>
 * Implements the logic for updating an {@link AtomicReference} correctly, using
 * the current value, computing the update and then setting it if it hasn't
 * changed in the meantime.
 * <p>
 * This is an abstract class that has a {@link #get(Object) factory template
 * method} to implement the new value creation.
 * <p>
 * 
 * @param <T> the type of the reference.
 * @since 0.0.12
 */
public abstract class AtomicReferenceUpdater<T> implements Function<T, T> {
    private final AtomicReference<T> reference;

    public AtomicReferenceUpdater(final AtomicReference<T> reference) {
        this.reference = notNull("reference", reference);
    }

    /**
     * Do the actual update. Calls the factory method with the old value to do
     * the update logic, then sets the value to that if it hasn't changed in the
     * meantime.
     * 
     * @return the new updated value.
     */
    public final T update() {
        T oldValue, newValue;
        do {
            oldValue = reference.get();
            newValue = get(oldValue);
            // test first to implement TTAS
            if (reference.get() != oldValue) {
                continue;
            }
            // then compare and set
        } while (!reference.compareAndSet(oldValue, newValue));
        return newValue;
    }
}
