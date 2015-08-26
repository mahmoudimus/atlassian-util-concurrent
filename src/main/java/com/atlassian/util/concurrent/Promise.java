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

import javax.annotation.Nonnull;
import java.util.concurrent.Future;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A promise that presents a nicer interface to {@link Future}. It can be
 * claimed without needing to catch checked exceptions, and it may be mapped to
 * new types of Promise via the {@link #map(Function)} and
 * {@link #flatMap(Function)} methods.
 * <p>
 * For instance, if you have a <code>Promise&lt;A&gt;</code> and you want to do
 * some operation on the value (an A) you can use {@link #map(Function)} to turn
 * this into a Promise of some other type. Let's say you get back a
 * <code>Person</code> and you really only need their surname:
 * <p>
 * 
 * <pre>
 * public Promise&lt;String&gt; fetchSurname(PersonId id) {
 *   Promise&lt;Person&gt; promise asyncClient.fetchPerson(id);
 *   return promise.map(new Function&ltPerson, String&gt;() {
 *     public String apply(Person p) {
 *       return p.surname();
 *     }
 *   };
 * }
 * </pre>
 * <p>
 * If you want to do some further asynchronous operation using the value, you
 * can use {@link #flatMap(Function)} to turn this into a Promise of some other
 * type. Let's say you get back a <code>Person</code> and you really only need
 * to perform a further query to get their address:
 * 
 * <pre>
 * public Promise&lt;Address&gt; fetchAddress(PersonId id) {
 *   Promise&lt;Person&gt; promise asyncClient.fetchPerson(id);
 *   return promise.flatMap(new Function&ltPerson, Promise&lt;Address&gt;&gt;() {
 *     public Promise&lt;Address&gt; apply(Person p) {
 *       return asyncClient.fetchAddress(p.addressId());
 *     }
 *   };
 * }
 * </pre>
 * <p>
 * Note that there are a number of handy utility functions for creating
 * <code>Promise</code> objects on the {@link Promises} companion.
 * 
 * @since 2.4
 */
public interface Promise<A> extends Future<A> {
  /**
   * Blocks the thread waiting for a result. Exceptions are thrown as runtime
   * exceptions.
   * 
   * @return The promised object
   */
  A claim();

  /**
   * Registers a callback to be called when the promised object is available.
   * May not be executed in the same thread as the caller.
   * 
   * @param e The effect to perform with the result
   * @return This object for chaining
   */
  Promise<A> done(Effect<? super A> e);

  /**
   * Registers a callback to be called when an exception is thrown. May not be
   * executed in the same thread as the caller.
   * 
   * @param e The effect to perform with the throwable
   * @return This object for chaining
   */
  Promise<A> fail(Effect<Throwable> e);

  /**
   * Registers a FutureCallback to handle both success and failure (exception)
   * cases. May not be executed in the same thread as the caller.
   * <p>
   * See {@link Promises#callback(Effect, Effect)}
   * {@link Promises#onSuccessDo(Effect)} and
   * {@link Promises#onFailureDo(Effect)} for easy ways of turning an
   * {@link Effect} into a {@link BiConsumer}
   * 
   * @param callback The future callback
   * @return This object for chaining
   */
  Promise<A> then(Callback<? super A> callback);

  /**
   * Transforms this {@link Promise} from one type to another by way of a
   * transformation function.
   * <p>
   *
   * @param function The transformation function
   * @return A new promise resulting from the transformation
   */
  <B> Promise<B> map(Function<? super A, ? extends B> function);

  /**
   * Transforms this promise from one type to another by way of a transformation
   * function that returns a new Promise, leaving the strategy for that promise
   * production up to the function.
   * <p>
   * Note this is known as flatMap as it first maps to a
   * <code>Promise&lt;Promise&lt;A&gt;&gt;</code> and then flattens that out
   * into a single layer Promise.
   * 
   * @param function The transformation function to a new Promise value
   * @return A new promise resulting from the transformation
   */
  <B> Promise<B> flatMap(Function<? super A, ? extends Promise<? extends B>> function);

  /**
   * Recover from an exception using the supplied exception strategy
   * 
   * @param handleThrowable rehabilitate the exception with a value of type B
   * @return A new promise that will not throw an exception (unless
   * handleThrowable itself threw).
   */
  Promise<A> recover(Function<Throwable, ? extends A> handleThrowable);

  /**
   * Transform this promise from one type to another, also providing a strategy
   * for dealing with any exceptions encountered.
   * 
   * @param handleThrowable rehabilitate the exception with a value of type B
   * @param function mapping function
   * @return A new promise resulting from the catamorphic transformation. This
   * promise will not throw an exception (unless handleThrowable itself threw).
   */
  <B> Promise<B> fold(Function<Throwable, ? extends B> handleThrowable, Function<? super A, ? extends B> function);

  /**
   * Callback interface to be called after a promise is fulfilled.
   * @param <A> type of the successful value.
   */
  interface Callback<A> {
    void onSuccess(A value);
    void onFailure(@Nonnull Throwable t);
  }
}
