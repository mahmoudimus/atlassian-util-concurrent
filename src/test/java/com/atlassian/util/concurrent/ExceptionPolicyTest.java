package com.atlassian.util.concurrent;

import static org.junit.Assert.assertNull;

import com.atlassian.util.concurrent.ExceptionPolicy.Policies;

import org.junit.Test;

import com.google.common.base.Supplier;

public class ExceptionPolicyTest
{
    @Test(expected = TestException.class)
    public void exceptionsThrow() {
        exception(Policies.THROW);
    }

    @Test
    public void exceptionsIgnored() {
        assertNull(exception(Policies.IGNORE_EXCEPTIONS));
    }

    private Object exception(final Policies handler) {
        final Supplier<Object> wrapped = handler.handler().apply(new Supplier<Object>() {
            public Object get() {
                throw new TestException();
            }
        });
        final Object result = wrapped.get();
        return result;
    }

    public class TestException extends RuntimeException {
        private static final long serialVersionUID = -2371420516340597047L;
    }
}
