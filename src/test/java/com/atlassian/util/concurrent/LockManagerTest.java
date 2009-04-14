package com.atlassian.util.concurrent;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

public class LockManagerTest {
    private final Function<String, String> reverser = new Function<String, String>() {
        public String get(final String input) {
            final StringBuilder result = new StringBuilder();
            for (int i = input.length(); i > 0; i--) {
                result.append(input.charAt(i - 1));
            }
            return result.toString();
        };
    };

    @Test
    public void testLockManagerLocksAndUnlocksAndReturnsValue() throws Exception {
        final AtomicReference<String> ref = new AtomicReference<String>();
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean locked = new AtomicBoolean();
        final AtomicBoolean unlocked = new AtomicBoolean();
        final LockManager<String> manager = new LockManagers.Manager<String, String>(new Function<String, Lock>() {
            public Lock get(final String input) {
                ref.set(input);
                return new ReentrantLock() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void lock() {
                        locked.set(true);
                        super.lock();
                    }

                    @Override
                    public void unlock() {
                        unlocked.set(true);
                        super.unlock();
                    }
                };
            };
        }, reverser);

        assertEquals("blah", manager.withLock("input", new Callable<String>() {
            public String call() {
                called.set(true);
                return "blah";
            }
        }));

        assertTrue(called.get());
        assertTrue(locked.get());
        assertTrue(unlocked.get());
        assertEquals("tupni", ref.get());
    }

    @Test
    public void testLockManagerLocksAndUnlocksAndThrows() throws Exception {
        final AtomicReference<String> ref = new AtomicReference<String>();
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean locked = new AtomicBoolean();
        final AtomicBoolean unlocked = new AtomicBoolean();
        final LockManager<String> manager = new LockManagers.Manager<String, String>(new Function<String, Lock>() {
            public Lock get(final String input) {
                ref.set(input);
                return new ReentrantLock() {
                    private static final long serialVersionUID = 5047219203514192915L;

                    @Override
                    public void lock() {
                        locked.set(true);
                        super.lock();
                    }

                    @Override
                    public void unlock() {
                        unlocked.set(true);
                        super.unlock();
                    }
                };
            };
        }, reverser);

        class StupidException extends Exception {
            private static final long serialVersionUID = -885206738173390512L;
        }

        try {
            manager.withLock("input", new Callable<String>() {
                public String call() throws StupidException {
                    called.set(true);
                    throw new StupidException();
                }
            });
            fail("StupidException expected");
        } catch (final StupidException ignore) {}

        assertTrue(called.get());
        assertTrue(locked.get());
        assertTrue(unlocked.get());
        assertEquals("tupni", ref.get());
    }

    @Test
    public void testLockManagerLocksAndUnlocksAndRuns() throws Exception {
        final AtomicReference<String> ref = new AtomicReference<String>();
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean locked = new AtomicBoolean();
        final AtomicBoolean unlocked = new AtomicBoolean();
        final LockManager<String> manager = new LockManagers.Manager<String, String>(new Function<String, Lock>() {
            public Lock get(final String input) {
                ref.set(input);
                return new ReentrantLock() {
                    private static final long serialVersionUID = -5545182312276280121L;

                    @Override
                    public void lock() {
                        locked.set(true);
                        super.lock();
                    }

                    @Override
                    public void unlock() {
                        unlocked.set(true);
                        super.unlock();
                    }
                };
            };
        }, reverser);

        manager.withLock("input", new Runnable() {
            public void run() {
                called.set(true);
            }
        });

        assertTrue(called.get());
        assertTrue(locked.get());
        assertTrue(unlocked.get());
        assertEquals("tupni", ref.get());
    }

    @Test
    public void testLockManagerLocksAndUnlocksAndThrowsRuntime() throws Exception {
        final AtomicReference<String> ref = new AtomicReference<String>();
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean locked = new AtomicBoolean();
        final AtomicBoolean unlocked = new AtomicBoolean();
        final LockManager<String> manager = new LockManagers.Manager<String, String>(new Function<String, Lock>() {
            public Lock get(final String input) {
                ref.set(input);
                return new ReentrantLock() {
                    private static final long serialVersionUID = 4219143544310279745L;

                    @Override
                    public void lock() {
                        locked.set(true);
                        super.lock();
                    }

                    @Override
                    public void unlock() {
                        unlocked.set(true);
                        super.unlock();
                    }
                };
            };
        }, reverser);

        class StupidException extends RuntimeException {
            private static final long serialVersionUID = 8829097977007580221L;
        }

        try {
            manager.withLock("input", new Runnable() {
                public void run() throws StupidException {
                    called.set(true);
                    throw new StupidException();
                }
            });
            fail("StupidException expected");
        } catch (final StupidException ignore) {}

        assertTrue(called.get());
        assertTrue(locked.get());
        assertTrue(unlocked.get());
        assertEquals("tupni", ref.get());
    }
}
