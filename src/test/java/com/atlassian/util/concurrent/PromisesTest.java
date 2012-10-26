package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Promises.futureCallback;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.util.concurrent.atomic.AtomicReference;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;

@RunWith(MockitoJUnitRunner.class)
public class PromisesTest {

    @Mock
    private Effect<Object> done;
    @Mock
    private Effect<Throwable> fail;
    @Mock
    private FutureCallback<Object> futureCallback;

    @Test
    public void promiseValue() {
        final Object instance = new Object();
        final Promise<Object> promise = Promises.promise(instance);

        assertThat(promise.isDone(), is(true));
        assertThat(promise.isCancelled(), is(false));
        assertThat(promise.claim(), is(instance));

        promise.then(futureCallback(done, fail));
        verify(done).apply(instance);
        verifyZeroInteractions(fail);

        promise.then(futureCallback);
        verify(futureCallback).onSuccess(instance);
        verifyNoMoreInteractions(futureCallback);
    }

    @Test
    public void promiseRejected() {
        final Throwable instance = new Throwable();
        final Promise<Object> promise = Promises.rejected(instance, Object.class);

        assertThat(promise.isDone(), is(true));
        assertThat(promise.isCancelled(), is(false));
        try {
            promise.claim();
        } catch (RuntimeException e) {
            assertSame(instance, e.getCause());
        }

        promise.then(futureCallback(done, fail));
        verifyZeroInteractions(done);
        verify(fail).apply(instance);

        promise.then(futureCallback);
        verify(futureCallback).onFailure(instance);
        verifyNoMoreInteractions(futureCallback);
    }

    @Test
    public void promiseOfListenableFutureSettingValue() {

        final SettableFuture<Object> future = SettableFuture.create();
        final Promise<Object> promise = Promises.forListenableFuture(future);

        // register call backs
        promise.then(futureCallback(done, fail));
        promise.then(futureCallback);

        assertThat(promise.isDone(), is(false));
        assertThat(promise.isCancelled(), is(false));

        final Object instance = new Object();
        future.set(instance);

        assertThat(promise.isDone(), is(true));
        assertThat(promise.isCancelled(), is(false));
        assertThat(promise.claim(), is(instance));

        verify(done).apply(instance);
        verifyZeroInteractions(fail);

        verify(futureCallback).onSuccess(instance);
        verifyNoMoreInteractions(futureCallback);
    }

    @Test
    public void promiseOfListenableFutureSettingException() {

        final SettableFuture<Object> future = SettableFuture.create();
        final Promise<Object> promise = Promises.forListenableFuture(future);

        // register call backs
        promise.then(futureCallback(done, fail));
        promise.then(futureCallback);

        assertThat(promise.isDone(), is(false));
        assertThat(promise.isCancelled(), is(false));

        final Throwable instance = new Throwable();
        future.setException(instance);

        assertThat(promise.isDone(), is(true));
        assertThat(promise.isCancelled(), is(false));
        try {
            promise.claim();
        } catch (RuntimeException t) {
            assertSame(instance, t.getCause());
        }

        verifyZeroInteractions(done);
        verify(fail).apply(instance);

        verify(futureCallback).onFailure(instance);
        verifyNoMoreInteractions(futureCallback);
    }

    @Test
    public void mapPromiseSettingValue() {
        final SettableFuture<Object> future = SettableFuture.create();

        final Promise<Object> originalPromise = Promises.forListenableFuture(future);
        final Promise<SomeObject> transformedPromise = originalPromise.map(new Function<Object, SomeObject>() {
            @Override
            public SomeObject apply(Object input) {
                return new SomeObject(input);
            }
        });

        assertThat(originalPromise.isDone(), is(false));
        assertThat(originalPromise.isCancelled(), is(false));

        assertThat(transformedPromise.isDone(), is(false));
        assertThat(transformedPromise.isCancelled(), is(false));

        final Object instance = new Object();
        future.set(instance);
        assertThat(originalPromise.isDone(), is(true));
        assertThat(originalPromise.isCancelled(), is(false));

        assertThat(originalPromise.claim(), is(instance));

        assertThat(transformedPromise.isDone(), is(true));
        assertThat(transformedPromise.isCancelled(), is(false));

        @SuppressWarnings("unchecked")
        final Effect<SomeObject> someObjectCallback = mock(Effect.class);
        transformedPromise.then(futureCallback(someObjectCallback, fail));

        assertThat(transformedPromise.claim().object, is(instance));
        verify(someObjectCallback).apply(argThat(new SomeObjectMatcher(instance)));
        verifyZeroInteractions(fail);
    }

    @Test
    public void mapPromiseSettingException() {
        final SettableFuture<Object> future = SettableFuture.create();

        final Promise<Object> originalPromise = Promises.forListenableFuture(future);
        final Promise<SomeObject> transformedPromise = originalPromise.map(new Function<Object, SomeObject>() {
            @Override
            public SomeObject apply(Object input) {
                return new SomeObject(input);
            }
        });

        assertThat(originalPromise.isDone(), is(false));
        assertThat(originalPromise.isCancelled(), is(false));

        assertThat(transformedPromise.isDone(), is(false));
        assertThat(transformedPromise.isCancelled(), is(false));

        final Throwable instance = new Throwable();
        future.setException(instance);
        assertThat(originalPromise.isDone(), is(true));
        assertThat(originalPromise.isCancelled(), is(false));

        try {
            originalPromise.claim();
        } catch (RuntimeException e) {
            assertSame(instance, e.getCause());
        }

        assertThat(transformedPromise.isDone(), is(true));
        assertThat(transformedPromise.isCancelled(), is(false));

        @SuppressWarnings("unchecked")
        final Effect<SomeObject> someObjectCallback = mock(Effect.class);
        transformedPromise.then(futureCallback(someObjectCallback, fail));

        try {
            transformedPromise.claim();
        } catch (RuntimeException e) {
            assertSame(instance, e.getCause());
        }
        verifyZeroInteractions(someObjectCallback);
        verify(fail).apply(instance);
    }

    @Test(expected = IllegalStateException.class)
    public void flatMapFunctionThrowsException() {
        final SettableFuture<Object> future = SettableFuture.create();

        final Promise<Object> originalPromise = Promises.forListenableFuture(future);
        final Promise<SomeObject> transformedPromise = originalPromise.flatMap(new Function<Object, Promise<SomeObject>>() {
            @Override
            public Promise<SomeObject> apply(Object input) {
                throw new IllegalStateException();
            }
        });

        future.set(new SomeObject("hi"));
        transformedPromise.claim();
    }

    @Test
    public void whenPromiseSettingValue() {

        final SettableFuture<Object> f1 = SettableFuture.create();
        final SettableFuture<Object> f2 = SettableFuture.create();

        final Promise<Object> p1 = Promises.forListenableFuture(f1);
        final Promise<Object> p2 = Promises.forListenableFuture(f2);

        @SuppressWarnings("unchecked")
        final Promise<List<Object>> seq = Promises.when(p1, p2);
        @SuppressWarnings("unchecked")
        final Effect<List<Object>> doneCallback = mock(Effect.class);
        seq.then(futureCallback(doneCallback, fail));

        assertThat(p1.isDone(), is(false));
        assertThat(p1.isCancelled(), is(false));

        assertThat(p2.isDone(), is(false));
        assertThat(p2.isCancelled(), is(false));

        assertThat(seq.isDone(), is(false));
        assertThat(seq.isCancelled(), is(false));

        final Object instance1 = new Object();
        f1.set(instance1);

        assertThat(p1.isDone(), is(true));
        assertThat(p1.isCancelled(), is(false));

        assertThat(p2.isDone(), is(false));
        assertThat(p2.isCancelled(), is(false));

        assertThat(seq.isDone(), is(false));
        assertThat(seq.isCancelled(), is(false));

        verifyZeroInteractions(doneCallback);
        verifyZeroInteractions(fail);

        final Object instance2 = new Object();
        f2.set(instance2);

        assertThat(p1.isDone(), is(true));
        assertThat(p1.isCancelled(), is(false));

        assertThat(p2.isDone(), is(true));
        assertThat(p2.isCancelled(), is(false));

        assertThat(seq.isDone(), is(true));
        assertThat(seq.isCancelled(), is(false));

        assertThat(seq.claim(), contains(instance1, instance2));
        verify(doneCallback).apply(Lists.newArrayList(instance1, instance2));
        verifyZeroInteractions(fail);
    }

    @Test
    public void sequencePromiseSettingException() {

        final SettableFuture<Object> f1 = SettableFuture.create();
        final SettableFuture<Object> f2 = SettableFuture.create();

        final Promise<Object> p1 = Promises.forListenableFuture(f1);
        final Promise<Object> p2 = Promises.forListenableFuture(f2);

        @SuppressWarnings("unchecked")
        final Promise<List<Object>> sequenced = Promises.when(p1, p2);
        @SuppressWarnings("unchecked")
        final Effect<List<Object>> doneCallback = mock(Effect.class);
        sequenced.then(futureCallback(doneCallback, fail));

        assertThat(p1.isDone(), is(false));
        assertThat(p1.isCancelled(), is(false));

        assertThat(p2.isDone(), is(false));
        assertThat(p2.isCancelled(), is(false));

        assertThat(sequenced.isDone(), is(false));
        assertThat(sequenced.isCancelled(), is(false));

        final Throwable throwable = new Throwable();
        f1.setException(throwable);

        assertThat(p1.isDone(), is(true));
        assertThat(p1.isCancelled(), is(false));

        assertThat(p2.isDone(), is(false));
        assertThat(p2.isCancelled(), is(false));

        assertThat(sequenced.isDone(), is(true));
        assertThat(sequenced.isCancelled(), is(false));

        verifyZeroInteractions(doneCallback);
        verify(fail).apply(throwable);

        final Throwable instance2 = new Throwable();
        f2.setException(instance2);

        assertThat(p1.isDone(), is(true));
        assertThat(p1.isCancelled(), is(false));

        assertThat(p2.isDone(), is(true));
        assertThat(p2.isCancelled(), is(false));

        assertThat(sequenced.isDone(), is(true));
        assertThat(sequenced.isCancelled(), is(false));

        verifyZeroInteractions(doneCallback);
        verifyNoMoreInteractions(fail);

        try {
            sequenced.claim();
        } catch (RuntimeException e) {
            assertSame(throwable, e.getCause());
        }
    }

    @Test
    public void doneAddsFutureCallback() {
        final AtomicReference<String> ref = new AtomicReference<String>();
        final SettableFuture<String> f = SettableFuture.<String> create();
        Promise<String> p = Promises.forListenableFuture(f);
        p.done(new Effect<String>() {
            @Override
            public void apply(String s) {
                ref.getAndSet("called: " + s);
            }
        });

        assertThat(ref.get(), is(nullValue()));
        f.set("done!");
        assertThat(ref.get(), is("called: done!"));
    }

    @Test
    public void failAddsFutureCallback() {
        final AtomicReference<Throwable> ref = new AtomicReference<Throwable>();
        final SettableFuture<String> f = SettableFuture.<String> create();
        Promise<String> p = Promises.forListenableFuture(f);
        p.fail(new Effect<Throwable>() {
            @Override
            public void apply(Throwable t) {
                ref.getAndSet(t);
            }
        });

        assertThat(ref.get(), is(nullValue()));
        Throwable ex = new RuntimeException("boo yaa!");
        f.setException(ex);
        assertThat(ref.get(), is(ex));
    }

    private static class SomeObject {
        public final Object object;

        private SomeObject(Object object) {
            this.object = object;
        }
    }

    private static final class SomeObjectMatcher extends BaseMatcher<SomeObject> {
        private final Object instance;

        public SomeObjectMatcher(Object instance) {
            this.instance = instance;
        }

        @Override
        public boolean matches(Object item) {
            return ((SomeObject) item).object.equals(instance);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("SomeObject matcher");
        }
    }
}
