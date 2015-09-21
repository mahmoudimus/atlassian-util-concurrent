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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import io.atlassian.util.concurrent.AbstractCopyOnWriteMap.View.Type;

import static java.util.Objects.requireNonNull;

/**
 * A thread-safe variant of {@link java.util.SortedMap} in which all mutative operations
 * (the "destructive" operations described by {@link java.util.SortedMap} put, remove and
 * so on) are implemented by making a fresh copy of the underlying map.
 * <p>
 * This is ordinarily too costly, but may be <em>more</em> efficient than
 * alternatives when traversal operations vastly out-number mutations, and is
 * useful when you cannot or don't want to synchronize traversals, yet need to
 * preclude interference among concurrent threads. The "snapshot" style
 * iterators on the collections returned by {@link #entrySet()},
 * {@link #keySet()} and {@link #values()} use a reference to the internal map
 * at the point that the iterator was created. This map never changes during the
 * lifetime of the iterator, so interference is impossible and the iterator is
 * guaranteed not to throw <tt>ConcurrentModificationException</tt>. The
 * iterators will not reflect additions, removals, or changes to the list since
 * the iterator was created. Removing elements via these iterators is not
 * supported. The mutable operations on these collections (remove, retain etc.)
 * may be supported if the views are live but as with the {@link java.util.Map} interface,
 * add and addAll are not and throw {@link java.lang.UnsupportedOperationException}.
 * <p>
 * The actual copy is performed by the abstract {@link #copy(Map)} method. This
 * implementation of this method is responsible for the underlying
 * {@link java.util.SortedMap} implementation (for instance a {@link java.util.TreeMap}) and
 * therefore the semantics of what this map will cope with as far as null keys
 * and values, iteration ordering etc. Standard j.u.c {@link java.util.Map} implementation
 * versions are available from the {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap.Builder}.
 * <p>
 * Collection views of the keys, values and entries are optionally
 * {@link View.Type.LIVE live} or {@link View.Type.STABLE stable}. Live views
 * are modifiable will cause a copy if a modifying method is called on them.
 * Methods on these will reflect the current state of the collection, although
 * iterators will be snapshot style. If the collection views are stable they are
 * unmodifiable, and will be a snapshot of the state of the map at the time the
 * collection was asked for. Regardless of the View policy though, all Views
 * taken using {@link #subMap(Object, Object)}, {@link #headMap(Object)} and
 * {@link #tailMap(Object)} are unmodifiable.
 * <p>
 * <strong>Please note</strong> that the thread-safety guarantees are limited to
 * the thread-safety of the non-mutative (non-destructive) operations of the
 * underlying map implementation. If the underlying map implementation does not
 * support concurrent {@link #get(Object)} calls for instance then it is
 * unsuitable as a candidate.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Jed Wesley-Smith
 */
@ThreadSafe public abstract class CopyOnWriteSortedMap<K, V> extends AbstractCopyOnWriteMap<K, V, SortedMap<K, V>> implements SortedMap<K, V> {
  private static final long serialVersionUID = 7375772978175545647L;

  /**
   * Get a {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap.Builder} for a {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap} instance.
   *
   * @param <K> key type
   * @param <V> value type
   * @return a fresh builder
   */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  /**
   * Build a {@link CopyOnWriteSortedMap} and specify all the options.
   * 
   * @param <K> key type
   * @param <V> value type
   */
  public static class Builder<K, V> {
    private View.Type viewType = View.Type.STABLE;
    private Comparator<? super K> comparator;
    private final Map<K, V> initialValues = new HashMap<K, V>();

    Builder() {}

    /**
     * Views are stable (fixed in time) and unmodifiable.
     */
    public Builder<K, V> stableViews() {
      viewType = View.Type.STABLE;
      return this;
    }

    /**
     * Views are live (reflecting concurrent updates) and mutator methods are
     * supported.
     */
    public Builder<K, V> liveViews() {
      viewType = View.Type.STABLE;
      return this;
    }

    /**
     * Views are live (reflecting concurrent updates) and mutator methods are
     * supported.
     */
    public Builder<K, V> addAll(final Map<? extends K, ? extends V> values) {
      initialValues.putAll(values);
      return this;
    }

    /**
     * Use the specified comparator.
     */
    public Builder<K, V> ordering(final Comparator<? super K> comparator) {
      this.comparator = comparator;
      return this;
    }

    /**
     * Use the keys natural ordering.
     */
    public Builder<K, V> orderingNatural() {
      this.comparator = null;
      return this;
    }

    public CopyOnWriteSortedMap<K, V> newTreeMap() {
      return (comparator == null) ? new Tree<K, V>(initialValues, viewType) : comparedTreeMap(initialValues, viewType, comparator);
    }
  }

  /**
   * Create a new {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap} where the underlying map
   * instances are {@link java.util.TreeMap} and the sort uses the key's natural order.
   * <p>
   * This map has {@link View.Type.STABLE stable} views.
   *
   * @return a {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap}.
   */
  public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap() {
    final Builder<K, V> builder = builder();
    return builder.newTreeMap();
  }

  /**
   * Create a new {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap} where the underlying map
   * instances are {@link java.util.TreeMap}, the sort uses the key's natural order and
   * the initial values are supplied.
   * <p>
   * This map has {@link View.Type.STABLE stable} views.
   *
   * @param map the map to use as the initial values.
   * @param <K> a K key type.
   * @param <V> a V value type.
   * @return a {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap}.
   */
  public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final @NotNull Map<? extends K, ? extends V> map) {
    final Builder<K, V> builder = builder();
    requireNonNull(map, "map");
    return builder.addAll(map).newTreeMap();
  }

  /**
   * Create a new {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap} where the underlying map
   * instances are {@link java.util.TreeMap}.
   * <p>
   * This map has {@link View.Type.STABLE stable} views.
   *
   * @param comparator the Comparator to use for ordering the keys. Note, should
   * be serializable if this map is to be serialized.
   * @param <K> a K key type.
   * @param <V> a V value type.
   * @return a {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap}.
   */
  public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final @NotNull Comparator<? super K> comparator) {
    final Builder<K, V> builder = builder();
    requireNonNull(comparator, "comparator");
    return builder.ordering(comparator).newTreeMap();
  }

  /**
   * Create a new {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap} where the underlying map
   * instances are {@link java.util.TreeMap}, the sort uses the key's natural order and
   * the initial values are supplied.
   * <p>
   * This map has {@link View.Type.STABLE stable} views.
   *
   * @param map to use as the initial values.
   * @param comparator for ordering.
   * @param <K> a K key type.
   * @param <V> a V value type.
   * @return a {@link io.atlassian.util.concurrent.CopyOnWriteSortedMap}.
   */
  public static <K, V> CopyOnWriteSortedMap<K, V> newTreeMap(final @NotNull Map<? extends K, ? extends V> map,
    final @NotNull Comparator<? super K> comparator) {
    final Builder<K, V> builder = builder();
    requireNonNull(comparator, "comparator");
    requireNonNull(map, "map");
    return builder.ordering(comparator).addAll(map).newTreeMap();
  }

  //
  // constructors
  //

  /**
   * Create a new empty {@link io.atlassian.util.concurrent.CopyOnWriteMap}.
   *
   * @param viewType a View.Type object.
   */
  protected CopyOnWriteSortedMap(final View.Type viewType) {
    super(Collections.<K, V> emptyMap(), viewType);
  }

  /**
   * Create a new {@link io.atlassian.util.concurrent.CopyOnWriteMap} with the supplied {@link java.util.Map} to
   * initialize the values.
   *
   * @param map the initial map to initialize with
   * @param viewType a View.Type.
   */
  protected CopyOnWriteSortedMap(final Map<? extends K, ? extends V> map, final View.Type viewType) {
    super(map, viewType);
  }

  //
  // methods
  //

  /** {@inheritDoc} */
  @Override @GuardedBy("internal-lock") protected abstract <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(N map);

  /** {@inheritDoc} */
  public Comparator<? super K> comparator() {
    return getDelegate().comparator();
  }

  /** {@inheritDoc} */
  public K firstKey() {
    return getDelegate().firstKey();
  }

  /** {@inheritDoc} */
  public K lastKey() {
    return getDelegate().lastKey();
  }

  /** {@inheritDoc} */
  public SortedMap<K, V> headMap(final K toKey) {
    return Collections.unmodifiableSortedMap(getDelegate().headMap(toKey));
  };

  /** {@inheritDoc} */
  public SortedMap<K, V> tailMap(final K fromKey) {
    return Collections.unmodifiableSortedMap(getDelegate().tailMap(fromKey));
  };

  /** {@inheritDoc} */
  public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
    return Collections.unmodifiableSortedMap(getDelegate().subMap(fromKey, toKey));
  };

  /**
   * Naturally ordered TreeMap based.
   * 
   * @param <K>
   * @param <V>
   */
  private static final class Tree<K, V> extends CopyOnWriteSortedMap<K, V> {
    private static final long serialVersionUID = 8015823768891873357L;

    Tree(final Map<? extends K, ? extends V> map, final Type viewType) {
      super(map, viewType);
    }

    @Override public final <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
      return new TreeMap<K, V>(map);
    }
  }

  private static <K, V> CopyOnWriteSortedMap<K, V> comparedTreeMap(final Map<? extends K, ? extends V> map, final Type viewType,
    final Comparator<? super K> comparator) {
    requireNonNull(comparator, "comparator");
    return new CopyOnWriteSortedMap<K, V>(map, viewType) {
      private static final long serialVersionUID = -7243810284130497340L;

      @Override public <N extends Map<? extends K, ? extends V>> SortedMap<K, V> copy(final N map) {
        final TreeMap<K, V> treeMap = new TreeMap<K, V>(comparator);
        treeMap.putAll(map);
        return treeMap;
      }
    };
  }
}
