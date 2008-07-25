package com.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class PhasedLatchTest
{
    @Test
    public void TestPhases() throws Exception
    {
        final PhasedLatch latch = new PhasedLatch();
        final AtomicInteger count = new AtomicInteger();
        final Thread client = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    while (true)
                    {
                        latch.awaitPhase(latch.getPhase());
                        count.getAndIncrement();
                    }
                }
                catch (final InterruptedException ignore)
                {}
            }
        });
        client.start();

        assertEquals(0, count.get());
        Util.pause();
        assertEquals(0, count.get());
        latch.release();
        Util.pause();
        assertEquals(1, count.get());
        latch.release();
        Util.pause();
        assertEquals(2, count.get());
        latch.release();
        Util.pause();
        assertEquals(3, count.get());

        latch.release();

        client.interrupt();
    }
}
