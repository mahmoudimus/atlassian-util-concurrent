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

package io.atlassian.util.concurrent;

import static java.util.Objects.requireNonNull;

/**
 * Convenience class for re-throwing {@link java.lang.InterruptedException}.
 */
public class RuntimeInterruptedException extends RuntimeException {

  private static final long serialVersionUID = -5025209597479375477L;

  /**
   * Constructor for RuntimeInterruptedException.
   *
   * @param cause a {@link java.lang.InterruptedException}.
   */
  public RuntimeInterruptedException(final InterruptedException cause) {
    super(requireNonNull(cause, "cause"));
  }

  /**
   * Constructor for RuntimeInterruptedException.
   *
   * @param message a {@link java.lang.String}.
   * @param cause a {@link java.lang.InterruptedException}.
   */
  public RuntimeInterruptedException(final String message, final InterruptedException cause) {
    super(message, requireNonNull(cause, "cause"));
  }

  /** {@inheritDoc} */
  @Override public InterruptedException getCause() {
    return (InterruptedException) super.getCause();
  }
}
