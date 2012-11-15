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

import net.jcip.annotations.ThreadSafe;

/**
 * A Function that resolves Descriptors (of type D) to a Resource (of type R).
 * <p>
 * Semantically, this could be a Factory, Generator, Builder, Closure,
 * Transformer, Resolver or something else entirely. No particular guarantees
 * are implied by this interface apart from idempotence (see below).
 * Specifically, implementations may or may not return or accept null and can
 * optionally block until elements are available or throw
 * {@link RuntimeException runtime exceptions} if the input is not acceptable
 * for some reason. Any clients that require a firmer contract should subclass
 * this interface and document their requirements.
 * <p>
 * It is expected that for any two calls to {@link #get(Object)} D the returned
 * resource will be semantically the same, ie. that the call is effectively
 * idempotent. Any implementation that violates this should document the fact.
 * It is not necessary that the resolved object is the same instance or even
 * implements {@link Object#equals(Object)} and {@link Object#hashCode()}
 * however.
 * <p>
 * As this interface requires idempotence implementations should be reentrant
 * and thread-safe.
 * <p>
 * Generally though, prefer to use the {com.google.common.base.Function Google
 * version} instead.
 * 
 * @param <D> the descriptor type.
 * @param <R> the resource type it resolves to.
 */
@ThreadSafe public interface Function<D, R> {
  /**
   * Resolves an output <R> where an input <D> is given.
   * 
   * @param input an object of type D.
   * @return the output of type R.
   */
  R get(D input);
}
