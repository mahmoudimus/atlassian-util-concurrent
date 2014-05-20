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

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import com.atlassian.util.concurrent.LazyReference.InitializationException;

import net.jcip.annotations.ThreadSafe;

/**
 * Lazily loaded reference that is not constructed until required. This class is
 * used to maintain a reference to an object that is expensive to create, but
 * may need to be reset and recomputed at a later time. Object creation is
 * guaranteed to be thread-safe and the first thread that calls {@link #get()}
 * will be the one that creates it.
 * <p>
 * Usage: clients need to implement the {@link #create()} method to return the
 * object this reference will hold.
 * <p>
 * For instance:
 * <p>
 * 
 * <pre>
 * final ResettableLazyReference&lt;MyObject&gt; ref = new ResettableLazyReference() {
 *   protected MyObject create() throws Exception {
 *     // Do expensive object construction here
 *     return new MyObject();
 *   }
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
 * 
 * @param T the type of the contained element.
 */
@ThreadSafe public abstract class ResettableLazyReference<T> implements Supplier<T> {
  @SuppressWarnings("rawtypes") private static final AtomicReferenceFieldUpdater<ResettableLazyReference, InternalReference> updater = AtomicReferenceFieldUpdater
    .newUpdater(ResettableLazyReference.class, InternalReference.class, "referrent");

  private volatile InternalReference<T> referrent = new InternalReference<T>(this);

  /**
   * The object factory method, guaranteed to be called once and only once.
   * 
   * @return the object that {@link #get()} and {@link #getInterruptibly()} will
   * return.
   * @throws Exception if anything goes wrong, rethrown as an
   * InitializationException from {@link #get()} and {@link #getInterruptibly()}
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
   * exception. The {@link InitializationException#getCause()} will contain the
   * exception thrown by the {@link #create()} method
   */
  public final T get() {
    return referrent.get();
  }

  /**
   * Get the lazily loaded reference in a cancellable manner. If your
   * <code>create()</code> method throws an Exception, calls to
   * <code>get()</code> will throw a RuntimeException which wraps the previously
   * thrown exception.
   * 
   * @return the object that {@link #create()} created.
   * @throws InitializationException if the {@link #create()} method throws an
   * exception. The {@link InitializationException#getCause()} will contain the
   * exception thrown by the {@link #create()} method
   * @throws InterruptedException If the calling thread is Interrupted while
   * waiting for another thread to create the value (if the creating thread is
   * interrupted while blocking on something, the {@link InterruptedException}
   * will be thrown as the causal exception of the
   * {@link InitializationException} to everybody calling this method).
   */
  public final T getInterruptibly() throws InterruptedException {
    return referrent.getInterruptibly();
  }

  /**
   * Reset the internal reference. Anyone currently in the process of calling
   * {@link #get()} will still force that and receive the old reference.
   * 
   * Note: this method was made final in 3.0. Override
   * {@link #onReset(LazyReference)} to implement custom reset behavior.
   */
  public final void reset() {
    resets();
  }

  /**
   * Reset the internal reference and returns a LazyReference of the old value.
   * Anyone currently in the process of calling {@link #get()} will still force
   * that and receive the old reference however.
   * 
   * @return A lazy reference of the old value that may or may not already be
   * initialized. Calling the supplier may block and cause the initialization to
   * occur.
   */
  public final LazyReference<T> resets() {
    @SuppressWarnings("unchecked")
    LazyReference<T> result = updater.getAndSet(this, new InternalReference<T>(this));
    onReset(result);
    return result;
  }

  /**
   * Template extension method for providing custom reset behavior.
   * 
   * @param oldValue the old LazyReference, guaranteed that nobody else has
   * access to it anymore.
   * @since 2.6
   */
  protected void onReset(LazyReference<T> oldValue) {}

  /**
   *   Has the {@link #create()} reference been initialized.
   * 
   * @return true if the task is complete and has not been reset.
   */
  public final boolean isInitialized() {
    return referrent.isInitialized();
  }

  /**
   * Cancel the initializing operation if it has not already run. Will try and
   * interrupt if it is currently running.
   */
  public final void cancel() {
    referrent.cancel();
  }

  /**
   * The internal LazyReference that may get thrown away
   */
  static class InternalReference<T> extends LazyReference<T> {
    private final ResettableLazyReference<T> ref;

    InternalReference(ResettableLazyReference<T> ref) {
      this.ref = ref;
    }

    @Override protected T create() throws Exception {
      return ref.create();
    }
  }
}
