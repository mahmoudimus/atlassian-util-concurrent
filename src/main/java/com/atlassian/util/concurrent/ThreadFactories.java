package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.isTrue;
import static com.atlassian.util.concurrent.Assertions.notNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for creating {@link ThreadFactory} instances. All factory
 * implementations produce named threads to give good stack-traces.
 */
public class ThreadFactories {
    public enum Type {
        DAEMON(true), USER(false);

        final boolean isDaemon;

        Type(final boolean isDaemon) {
            this.isDaemon = isDaemon;
        }
    }

    /**
     * Get a {@link ThreadFactory} with the required name prefix. The produced
     * threads are user threads and have normal priority.
     * 
     * @param name the prefix to use for naming the threads.
     * @return a configured {@link ThreadFactory}
     */
    public static ThreadFactory namedThreadFactory(@NotNull final String name) {
        return new Default(name, Type.USER, Thread.NORM_PRIORITY);
    }

    /**
     * Get a {@link ThreadFactory} with the required name prefix and type (user
     * or daemon). The produced threads have normal priority.
     * 
     * @param name the prefix to use for naming the threads.
     * @param type whether they are User or Daemon threads.
     * @return a configured {@link ThreadFactory}
     */
    public static ThreadFactory namedThreadFactory(@NotNull final String name, @NotNull final Type type) {
        return new Default(name, type, Thread.NORM_PRIORITY);
    }

    /**
     * Get a {@link ThreadFactory} with the required name prefix, type and
     * priority.
     * 
     * @param name the prefix to use for naming the threads.
     * @param type whether they are User or Daemon threads.
     * @param priority the thread priority, must not be lower than
     * {@link Thread#MIN_PRIORITY} or greater than {@link Thread#MAX_PRIORITY}
     * @return a configured {@link ThreadFactory}
     */
    public static ThreadFactory namedThreadFactory(@NotNull final String name, @NotNull final Type type, final int priority) {
        return new Default(name, type, priority);
    }

    private ThreadFactories() {
        throw new AssertionError("cannot instantiate!");
    }

    static class Default implements ThreadFactory {
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;
        final Type type;
        final int priority;

        Default(final String name, final Type type, final int priority) {
            notNull("name", name);
            notNull("type", type);
            isTrue("priority too low", priority >= Thread.MIN_PRIORITY);
            isTrue("priority too high", priority <= Thread.MAX_PRIORITY);
            final SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = name + ":thread-";
            this.type = type;
            this.priority = priority;
        }

        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            t.setDaemon(type.isDaemon);
            t.setPriority(priority);
            return t;
        }
    }
}
