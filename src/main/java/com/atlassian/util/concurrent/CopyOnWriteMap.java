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
import sun.security.pkcs11.wrapper.Functions;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A thread-safe variant of {@link Map} in which all mutative operations (the "destructive"
 * operations described by {@link Map} put, remove and so on) are implemented by making a fresh copy
 * of the underlying map.
 * <p>
 * This is ordinarily too costly, but may be <em>more</em> efficient than alternatives when
 * traversal operations vastly out-number mutations, and is useful when you cannot or don't want to
 * synchronize traversals, yet need to preclude interference among concurrent threads. The
 * "snapshot" style iterators on the collections returned by {@link #entrySet()}, {@link #keySet()}
 * and {@link #values()} use a reference to the internal map at the point that the iterator was
 * created. This map never changes during the lifetime of the iterator, so interference is
 * impossible and the iterator is guaranteed not to throw <tt>ConcurrentModificationException</tt>.
 * The iterators will not reflect additions, removals, or changes to the list since the iterator was
 * created. Removing elements via these iterators is not supported. The mutable operations on these
 * collections (remove, retain etc.) are supported but as with the {@link Map} interface, add and
 * addAll are not and throw {@link UnsupportedOperationException}.
 * <p>
 * The actual copy is performed by a supplied {@link CopyFunction} object. The Factory is
 * responsible for the underlying Map implementation (for instance a HashMap, TreeMap,
 * ListOrderedMap etc.) and therefore the semantics of what this map will cope with as far as null
 * keys and values, iteration ordering etc.
 * <p>
 * There are supplied {@link Functions} for the common Collections {@link Map} implementations.
 * <p>
 * Views of the keys, values and entries are modifiable and will cause a copy.
 * <p>
 * <strong>Please note</strong> that the thread-safety guarantees are limited to the thread-safety
 * of the non-mutative (non-destructive) operations of the underlying map implementation. For
 * instance some implementations such as {@link WeakHashMap} and {@link LinkedHashMap} with access
 * ordering are actually structurally modified by the {@link #get(Object)} method and are therefore
 * not suitable candidates as delegates for this class.
 * 
 * @param <K> the key type
 * @param <V> the value type
 * @author Jed Wesley-Smith
 */
@ThreadSafe public abstract class CopyOnWriteMap<K, V> extends AbstractCopyOnWriteMap<K, V, Map<K, V>> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 7935514534647505917L;

    public interface CopyFunction<M extends Map<?, ?>> extends AbstractCopyOnWriteMap.CopyFunction<M> {}

    //
    // constructors
    //

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to initialize the values
     * and the {@link CopyFunction} for creating our actual delegate instances.
     * 
     * @param map the initial map to initialize with
     * @param factory the copy function
     */
    public CopyOnWriteMap(final Map<? extends K, ? extends V> map) {
        super(map);
    }

    /**
     * Create a new empty {@link CopyOnWriteMap} with the {@link CopyFunction} for creating our
     * actual delegate instances.
     * 
     * @param factory the copy function
     */
    public CopyOnWriteMap() {
        super(Collections.<K, V> emptyMap());
    }

    @Override public abstract <N extends Map<? extends K, ? extends V>> Map<K, V> copy(N map);
}