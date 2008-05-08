package com.atlassian.util.concurrent;

final class Util {
    static final int WAIT = 10;

    static void pause(final int millis) {
        try {
            Thread.sleep(millis);
        }
        catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void pause() {
        pause(WAIT);
    }
}