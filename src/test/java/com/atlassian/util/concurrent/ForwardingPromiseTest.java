package com.atlassian.util.concurrent;

import com.google.common.util.concurrent.FutureCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ForwardingPromiseTest
{
    @Mock
    private Promise<Object> promise;

    private ForwardingPromise<Object> forwardingPromise;

    @Before
    public void setUp() {
        forwardingPromise = new ForwardingPromise<Object>()
        {
            @Override
            protected Promise<Object> delegate()
            {
                return promise;
            }
        };
    }

    @Test
    public void testDoneReturnsThis() {
        assertSame(forwardingPromise, forwardingPromise.done(mock(Effect.class)));
    }

    @Test
    public void testFailReturnsThis() {
        assertSame(forwardingPromise, forwardingPromise.fail(mock(Effect.class)));
    }

    @Test
    public void testThenReturnsThis() {
        assertSame(forwardingPromise, forwardingPromise.then(mock(FutureCallback.class)));
    }
}
