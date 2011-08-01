package com.atlassian.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class MockCompletionService implements CompletionService<String> {
    @Override
    public Future<String> take() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> submit(final Runnable task, final String result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> submit(final Callable<String> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<String> poll() {
        throw new UnsupportedOperationException();
    }
}
