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

import static com.atlassian.util.concurrent.Assertions.notNull;
import net.jcip.annotations.ThreadSafe;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link WeakMemoizer} caches the result of another function. The result is
 * {@link WeakReference weakly referenced} internally. This is useful if the
 * result is expensive to compute or the identity of the result is particularly
 * important.
 * <p>
 * If the results from this function are further cached then they will tend to
 * stay in this cache for longer.
 * 
 * @param <K> comparable descriptor, the usual rules for any {@link HashMap} key
 * apply.
 * @param <V> the value
 */
@ThreadSafe class WeakMemoizer<K, V> implements Function<K, V> {
  static <K, V> WeakMemoizer<K, V> weakMemoizer(final Function<K, V> delegate) {
    return new WeakMemoizer<K, V>(delegate);
  }

  private final ConcurrentMap<K, MappedReference<K, V>> map;
  private final ReferenceQueue<V> queue = new ReferenceQueue<V>();
  private final Function<K, V> delegate;

  /**
   * Construct a new {@link WeakMemoizer} instance.
   * 
   * @param initialCapacity how large the internal map should be initially.
   * @param delegate for creating the initial values.
   * @throws IllegalArgumentException if the initial capacity of elements is
   * negative.
   */
  WeakMemoizer(final @NotNull Function<K, V> delegate) {
    this.map = new ConcurrentHashMap<K, MappedReference<K, V>>();
    this.delegate = notNull("delegate", delegate);
  }

  /**
   * Get a result for the supplied Descriptor.
   * 
   * @param descriptor must not be null
   * @return descriptor lock
   */
  public V get(final K descriptor) {
    expungeStaleEntries();
    notNull("descriptor", descriptor);
    while (true) {
      final MappedReference<K, V> reference = map.get(descriptor);
      if (reference != null) {
        final V value = reference.get();
        if (value != null) {
          return value;
        }
        map.remove(descriptor, reference);
      }
      map.putIfAbsent(descriptor, new MappedReference<K, V>(descriptor, delegate.get(descriptor), queue));
    }
  }

  // expunge entries whose value reference has been collected
  @SuppressWarnings("unchecked") private void expungeStaleEntries() {
    MappedReference<K, V> ref;
    // /CLOVER:OFF
    while ((ref = (MappedReference<K, V>) queue.poll()) != null) {
      final K key = ref.getDescriptor();
      if (key == null) {
        // DO NOT REMOVE! In theory this should not be necessary as it
        // should not be able to be null - but we have seen it happen!
        continue;
      }
      map.remove(key, ref);
    }
    // /CLOVER:ON
  }

  /**
   * A weak reference that maintains a reference to the key so that it can be
   * removed from the map when the value is garbage collected.
   */
  static final class MappedReference<K, V> extends WeakReference<V> {
    private final K key;

    public MappedReference(final K key, final V value, final ReferenceQueue<? super V> q) {
      super(notNull("value", value), q);
      this.key = notNull("key", key);
    }

    final K getDescriptor() {
      return key;
    }
  }
}
