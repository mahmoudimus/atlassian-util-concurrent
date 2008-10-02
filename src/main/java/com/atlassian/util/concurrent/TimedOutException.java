package com.atlassian.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimedOutException extends TimeoutException {
    /**
     * 
     */
    private static final long serialVersionUID = 2639693125779305458L;

    public TimedOutException(final long time, final TimeUnit unit) {
        super("Timed out after: " + time + " " + unit);
    }

    public TimedOutException(final Timeout timeout) {
        this(timeout.getTime(), timeout.getUnit());
    }
}
