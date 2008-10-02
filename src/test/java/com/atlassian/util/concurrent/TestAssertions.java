package com.atlassian.util.concurrent;

import static org.junit.Assert.fail;

import org.junit.Test;

public class TestAssertions {

    @Test public void isNotNullThrowsNull() {
        try {
            Assertions.notNull("something", null);
            fail("Should have thrown IllegalArgumentEx");
        }
        catch (final IllegalArgumentException expected) {
            // yay
        }
    }

    @Test public void isNotNull() {
        Assertions.notNull("something", "notNull");
    }
}