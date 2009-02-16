package com.atlassian.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * Something that can be awaited upon.
 * 
 * @author Jed Wesley-Smith
 */
public interface Awaitable {

    /**
     * Await for the condition to become true.
     * 
     * @throw {@link InterruptedException} if {@link Thread#interrupt()
     * interrupted}
     */
    void await() throws InterruptedException;

    /**
     * Await for the specified time for the condition to become true.
     * 
     * @param time the amount to wait.
     * @param unit the unit to wait in.
     * @throw {@link InterruptedException} if {@link Thread#interrupt()
     * interrupted}
     * @return true if the condition became true within the time limit, false
     * otherwise.
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;
}
