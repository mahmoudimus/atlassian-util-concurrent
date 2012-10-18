package com.atlassian.util.concurrent;

import static com.google.common.base.Functions.toStringFunction;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;

public class PromiseTest {
    Function<Throwable, String> getThrowableMessage = new Function<Throwable, String>() {
        @Override
        public String apply(Throwable t) {
            return t.getMessage();
        }
    };

    @Test
    public void flatMapPromise() {
        final SettableFuture<String> fOne = SettableFuture.<String> create();
        final SettableFuture<Integer> fTwo = SettableFuture.<Integer> create();
        final Promise<String> pOne = Promises.forListenableFuture(fOne);
        final Promise<Integer> pTwo = pOne.flatMap(new Function<String, Promise<Integer>>() {
            public Promise<Integer> apply(String input) {
                return Promises.forListenableFuture(fTwo);
            };
        });

        assertThat(pOne.isDone(), is(false));
        assertThat(pTwo.isDone(), is(false));
        fOne.set("hey");
        assertThat(pOne.isDone(), is(true));
        assertThat(pTwo.isDone(), is(false));
        assertThat(pOne.claim(), is("hey"));
        fTwo.set(42);
        assertThat(pOne.isDone(), is(true));
        assertThat(pTwo.isDone(), is(true));
        assertThat(pTwo.claim(), is(42));
    }

    @Test
    public void foldPromiseGood() {
        Promise<Integer> promise = Promises.promise(3);
        assertThat(promise.fold(getThrowableMessage, toStringFunction()).claim(), is("3"));
    }

    @Test
    public void foldPromiseBad() {
        Promise<Integer> promise = Promises.rejected(new RuntimeException("Oh my!"), Integer.class);
        assertThat(promise.fold(getThrowableMessage, toStringFunction()).claim(), is("Oh my!"));
    }

    @Test
    public void foldPromiseGoodWithError() {
        Promise<Integer> promise = Promises.promise(4);
        assertThat(promise.fold(getThrowableMessage, new Function<Integer, String>() {
            public String apply(Integer i) {
                throw new RuntimeException("I lied!");
            }
        }).claim(), is("I lied!"));
    }

    @Test
    public void foldPromiseBadWithError() {
        Promise<Integer> promise = Promises.promise(4);
        final FailEffect failEffect = new FailEffect();
        promise.fold(new Function<Throwable, String>() {
            public String apply(Throwable input) {
                throw new RuntimeException(input);
            }
        }, new Function<Integer, String>() {
            public String apply(Integer i) {
                throw new RuntimeException("I lied!");
            }
        }).fail(failEffect);

        assertThat(failEffect.throwable.getCause().getMessage(), is("I lied!"));
    }

    @Test
    public void recoverPromiseGood() {
        Promise<String> promise = Promises.promise("sweet!");
        assertThat(promise.recover(getThrowableMessage).claim(), is("sweet!"));
    }

    @Test
    public void recoverPromiseBad() {
        Promise<String> promise = Promises.rejected(new RuntimeException("Oh Noes!!!"), String.class);
        assertThat(promise.recover(getThrowableMessage).claim(), is("Oh Noes!!!"));
    }

    private static class FailEffect implements Effect<Throwable> {
        Throwable throwable;

        public void apply(Throwable throwable) {
            this.throwable = throwable;
        }
    }
}
