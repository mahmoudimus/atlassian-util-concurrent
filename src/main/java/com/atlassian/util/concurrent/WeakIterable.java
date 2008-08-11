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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simple {@link Iterable} that holds {@link WeakReference weak references} to content elements. For
 * convenience there are {@link #add(Object)} and {@link #isEmpty()} methods.
 * <p>
 * {@link Iterator Iterators} returned by this object maintain a hard reference to the next object.
 * They are otherwise unstable as references may be garbage collected at any time
 */
public class WeakIterable<E> implements Iterable<E> {
    private final Queue<WeakReference<E>> queue = new LinkedBlockingQueue<WeakReference<E>>();

    E add(final E e) {
        queue.add(new WeakReference<E>(e));
        return e;
    }

    boolean isEmpty() {
        return !iterator().hasNext();
    }

    public Iterator<E> iterator() {
        final Iterator<WeakReference<E>> iterator = queue.iterator();
        return new Iterator<E>() {
            E next = getNext();

            public boolean hasNext() {
                return next != null;
            }

            public E next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                try {
                    return next;
                }
                finally {
                    next = getNext();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            E getNext() {
                E result = null;
                while (result == null) {
                    if (!iterator.hasNext()) {
                        return null;
                    }
                    final WeakReference<E> ref = iterator.next();
                    final E e = ref.get();
                    if (e == null) {
                        iterator.remove();
                        continue;
                    }
                    result = e;
                }
                return result;
            }
        };
    }
}
