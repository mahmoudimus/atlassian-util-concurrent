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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright 2007 Atlassian Software. All rights reserved.
 */
public class ConcurrentOperationMapImplTest {

    @Test public void runOperationsConcurrently() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch runSignal = new CountDownLatch(2);
        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch doneSignal = new CountDownLatch(2);

        final ConcurrentOperationMap<String, Integer> concurrentOperationMap = new ConcurrentOperationMapImpl<String, Integer>() {

            @Override Integer runAndGet(final FutureTask<Integer> namedFuture) throws ExecutionException {
                runSignal.countDown();
                try {
                    runSignal.await();
                }
                catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return super.runAndGet(namedFuture);
            }
        };

        // Create two threads whose job will be to call runOpertion with the
        // same name object
        new Thread(new Worker(startSignal, doneSignal) {
            @Override void doWork() {
                try {
                    assertEquals(Integer.valueOf(1), concurrentOperationMap.runOperation("same-key", new Callable<Integer>() {
                        public Integer call() {
                            return counter.incrementAndGet();
                        }
                    }));
                }
                catch (final ExecutionException e) {
                    fail(e.toString());
                }
            }
        }).start();
        new Thread(new Worker(startSignal, doneSignal) {
            @Override void doWork() {
                try {
                    assertEquals(Integer.valueOf(1), concurrentOperationMap.runOperation("same-key", new Callable<Integer>() {
                        public Integer call() {
                            return counter.incrementAndGet();
                        }
                    }));
                }
                catch (final ExecutionException e) {
                    fail(e.toString());
                }
            }
        }).start();

        // Hang them off a latch so that we can ensure they are both going to
        // run through the runOperation method
        // together

        startSignal.countDown();

        // Ensure after running that only one of the callables from one of the
        // thread was invoked and that the same
        // result is found by both threads.

        doneSignal.await();
        assertEquals(1, counter.get());
    }

    @Test public void exceptionsGetRemoved() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final ConcurrentOperationMap<String, Integer> concurrentOperationMap = new ConcurrentOperationMapImpl<String, Integer>();

        // Create two threads whose job will be to call runOpertion with the
        // same name object

        class MyException extends RuntimeException {
            private static final long serialVersionUID = 1472906008827102127L;
        }

        final Callable<Integer> operation = new Callable<Integer>() {
            public Integer call() {
                counter.incrementAndGet();
                throw new MyException();
            }
        };
        try {
            concurrentOperationMap.runOperation("same-key", operation);
            fail("MyException expected");
        }
        catch (final MyException expected) {}
        try {
            concurrentOperationMap.runOperation("same-key", operation);
            fail("MyException expected");
        }
        catch (final MyException expected) {}
        try {
            concurrentOperationMap.runOperation("same-key", operation);
            fail("MyException expected");
        }
        catch (final MyException expected) {}

        assertEquals(3, counter.get());
    }

    @Test public void runtimeExceptionGetsReThrown() throws Exception {
        final ConcurrentOperationMap<String, Integer> concurrentOperationMap = new ConcurrentOperationMapImpl<String, Integer>();

        // Create two threads whose job will be to call runOpertion with the
        // same name object

        class MyException extends RuntimeException {
            private static final long serialVersionUID = -9171222011228163934L;
        }

        final Callable<Integer> operation = new Callable<Integer>() {
            public Integer call() {
                throw new MyException();
            }
        };
        try {
            concurrentOperationMap.runOperation("same-key", operation);
            fail("MyException expected");
        }
        catch (final MyException expected) {}
    }

    @Test public void errorGetsReThrown() throws Exception {
        final ConcurrentOperationMap<String, Integer> concurrentOperationMap = new ConcurrentOperationMapImpl<String, Integer>();

        // Create two threads whose job will be to call runOpertion with the
        // same name object

        class MyError extends Error {
            private static final long serialVersionUID = -1416631799712180762L;
        }

        final Callable<Integer> operation = new Callable<Integer>() {
            public Integer call() {
                throw new MyError();
            }
        };
        try {
            concurrentOperationMap.runOperation("same-key", operation);
            fail("MyException expected");
        }
        catch (final MyError expected) {}
    }

    @Test public void checkedExceptionGetsWrapped() throws Exception {
        final ConcurrentOperationMap<String, Integer> concurrentOperationMap = new ConcurrentOperationMapImpl<String, Integer>();

        // Create two threads whose job will be to call runOpertion with the
        // same name object

        class MyException extends Exception {
            private static final long serialVersionUID = 22367874459914044L;
        }

        final Callable<Integer> operation = new Callable<Integer>() {
            public Integer call() throws MyException {
                throw new MyException();
            }
        };
        try {
            concurrentOperationMap.runOperation("same-key", operation);
            fail("MyException expected");
        }
        catch (final ExecutionException expected) {
            assertEquals(MyException.class, expected.getCause().getClass());
        }
    }

    abstract class Worker implements Runnable {
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;

        Worker(final CountDownLatch startSignal, final CountDownLatch doneSignal) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        public void run() {
            try {
                startSignal.await();
                doWork();
                doneSignal.countDown();
            }
            catch (final InterruptedException ex) {} // return;
        }

        abstract void doWork();
    }
}