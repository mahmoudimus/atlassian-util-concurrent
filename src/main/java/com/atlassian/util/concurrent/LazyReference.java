/**
 * Copyright 2008 Atlassian Pty Ltd 
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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import net.jcip.annotations.ThreadSafe;

/**
 * Lazily loaded reference that is not constructed until required. This class is
 * used to maintain a reference to an object that is expensive to create and
 * must be constructed once and once only. This reference behaves as though the
 * <code>final</code> keyword has been used (you cannot reset it once it has
 * been constructed).
 * <p>
 * Usage: clients need to implement the {@link #create()} method to return the
 * object this reference will hold.
 * <p>
 * For instance:
 * <p>
 * 
 * <pre>
 * final LazyReference&lt;MyObject&gt; ref = new LazyReference() {
 *     protected MyObject create() throws Exception {
 *         // Do expensive object construction here
 *         return new MyObject();
 *     }
 * };
 * </pre>
 * 
 * Then call {@link #get()} to get a reference to the referenced object:
 * 
 * <pre>
 * MyObject myLazyLoadedObject = ref.get()
 * </pre>
 * 
 * NOTE: Interruption policy is that if you want to be cancellable while waiting
 * for another thread to create the value, instead of calling {@link #get()}
 * call {@link #getInterruptibly()}. However, If your {@link #create()} method
 * is interrupted and throws an {@link InterruptedException}, it is treated as
 * an application exception and will be the causal exception inside the runtime
 * {@link InitializationException} that {@link #get()} or
 * {@link #getInterruptibly()} throws and your {@link #create()} will not be
 * called again.
 * <p>
 * Implementation note. This class extends {@link WeakReference} as
 * {@link Reference} does not have a public constructor. WeakReference is
 * preferable as it does not have any members and therefore doesn't increase the
 * memory footprint. As we never pass a referent through to the super-class and
 * override {@link #get()}, the garbage collection semantics of WeakReference
 * are irrelevant. The referenced object will not become eligible for GC unless
 * the object holding the reference to this object is collectible.
 */
@ThreadSafe
public abstract class LazyReference<T> extends WeakReference<T> {
    private final FutureTask<T> future = new FutureTask<T>(new Callable<T>() {
        public T call() throws Exception {
            return create();
        }
    });

    public LazyReference() {
        super(null);
    }

    /**
     * The object factory method, guaranteed to be called once and only once.
     * 
     * @return the object that {@link #get()} and {@link #getInterruptibly()}
     * will return.
     * @throws Exception if anything goes wrong, rethrown as an
     * InitializationException from {@link #get()} and
     * {@link #getInterruptibly()}
     */
    protected abstract T create() throws Exception;

    /**
     * Get the lazily loaded reference in a non-cancellable manner. If your
     * <code>create()</code> method throws an Exception calls to
     * <code>get()</code> will throw an InitializationException which wraps the
     * previously thrown exception.
     * 
     * @return the object that {@link #create()} created.
     * @throws InitializationException if the {@link #create()} method throws an
     * exception. The {@link InitializationException#getCause()} will contain
     * the exception thrown by the {@link #create()} method
     */
    @Override
    public final T get() {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return getInterruptibly();
                } catch (final InterruptedException ignore) {
                    // ignore and try again
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get the lazily loaded reference in a cancellable manner. If your
     * <code>create()</code> method throws an Exception, calls to
     * <code>get()</code> will throw a RuntimeException which wraps the
     * previously thrown exception.
     * 
     * @return the object that {@link #create()} created.
     * @throws InitializationException if the {@link #create()} method throws an
     * exception. The {@link InitializationException#getCause()} will contain
     * the exception thrown by the {@link #create()} method
     * @throws InterruptedException If the calling thread is Interrupted while
     * waiting for another thread to create the value (if the creating thread is
     * interrupted while blocking on something, the {@link InterruptedException}
     * will be thrown as the causal exception of the
     * {@link InitializationException} to everybody calling this method).
     */
    public final T getInterruptibly() throws InterruptedException {
        if (!future.isDone()) {
            future.run();
        }

        try {
            return future.get();
        } catch (final ExecutionException e) {
            throw new InitializationException(e);
        }
    }

    /**
     * Has the {@link #create()} reference been initialized.
     * 
     * @return true if the task is complete
     */
    public boolean isInitialized() {
        return future.isDone();
    }

    /**
     * Cancel the initializing operation if it has not already run. Will try and
     * interrupt if it is currently running.
     */
    public void cancel() {
        future.cancel(true);
    }

    /**
     * If the factory {@link LazyReference#create()} method threw an exception,
     * this wraps it.
     */
    public static class InitializationException extends RuntimeException {
        private static final long serialVersionUID = 3638376010285456759L;

        InitializationException(final ExecutionException e) {
            super((e.getCause() != null) ? e.getCause() : e);
        }
    }
}
