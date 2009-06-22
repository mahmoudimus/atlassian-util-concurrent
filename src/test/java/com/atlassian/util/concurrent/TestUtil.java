package com.atlassian.util.concurrent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

final class TestUtil {
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

    @SuppressWarnings("unchecked")
    static <T> T serialize(final T map) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(bytes).writeObject(map);
            return (T) new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}