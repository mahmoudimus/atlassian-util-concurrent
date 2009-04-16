package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.isTrue;
import static com.atlassian.util.concurrent.Assertions.notNull;

import org.junit.Test;

public class AssertionsTest {

    @Test(expected = IllegalArgumentException.class)
    public void isNotNullThrowsIllegalArg() {
        notNull("something", null);
    }

    @Test
    public void isNotNull() {
        notNull("something", "notNull");
    }

    @Test(expected = IllegalArgumentException.class)
    public void isTrueThrowsIllegalArg() {
        isTrue("something", false);
    }

    @Test
    public void Passes() {
        isTrue("something", true);
    }
}