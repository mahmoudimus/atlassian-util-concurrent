package com.atlassian.util.concurrent;

final class Util {
    static final int WAIT = 10;

    static void pause(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            // /CLOVER:OFF
            throw new RuntimeException(e);
            // /CLOVER:ON
        }
    }

    static void pause() {
        pause(WAIT);
    }
}