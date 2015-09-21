package io.atlassian.util.concurrent;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.function.Function;

public class PromiseTest {
  Function<Throwable, String> getThrowableMessage = Throwable::getMessage;

  @Test public void flatMapPromise() {
    final Promises.AsynchronousEffect<String> fOne = Promises.newAsynchronousEffect();
    final Promises.AsynchronousEffect<Integer> fTwo = Promises.newAsynchronousEffect();
    final Promise<String> pOne = Promises.forEffect(fOne);
    final Promise<Integer> pTwo = pOne.flatMap(input -> Promises.forEffect(fTwo));

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

  @Test public void foldPromiseGood() {
    Promise<Integer> promise = Promises.promise(3);
    assertThat(promise.fold(getThrowableMessage, Object::toString).claim(), is("3"));
  }

  @Test public void foldPromiseBad() {
    Promise<Integer> promise = Promises.rejected(new RuntimeException("Oh my!"));
    assertThat(promise.fold(getThrowableMessage, Object::toString).claim(), is("Oh my!"));
  }

  @Test public void foldPromiseGoodWithError() {
    Promise<Integer> promise = Promises.promise(4);
    assertThat(promise.fold(getThrowableMessage, i -> {
      throw new RuntimeException("I lied!");
    }).claim(), is("I lied!"));
  }

  @Test public void foldPromiseBadWithError() {
    Promise<Integer> promise = Promises.promise(4);
    final FailEffect failEffect = new FailEffect();
    promise.fold(t -> {
      throw new RuntimeException(t);
    }, i -> {
      throw new RuntimeException("I lied!");
    }).fail(failEffect);
    assertNotNull(failEffect.throwable.getCause());
    assertThat(failEffect.throwable.getCause().getMessage(), is("I lied!"));
  }

  @Test public void failCanTransformException() {
    final Promises.AsynchronousEffect<String> future = Promises.newAsynchronousEffect();
    final Promise<String> promise = Promises.forEffect(future).map(input -> "Ok").recover(Throwable::getMessage);
    future.exception(new RuntimeException("Some message"));
    assertThat(promise.claim(), is("Some message"));
  }

  @Test public void recoverPromiseGood() {
    Promise<String> promise = Promises.promise("sweet!");
    assertThat(promise.recover(getThrowableMessage).claim(), is("sweet!"));
  }

  @Test public void recoverPromiseBad() {
    Promise<String> promise = Promises.rejected(new RuntimeException("Oh Noes!!!"));
    assertThat(promise.recover(getThrowableMessage).claim(), is("Oh Noes!!!"));
  }

  private static class FailEffect implements Effect<Throwable> {
    Throwable throwable;

    public void apply(Throwable throwable) {
      this.throwable = throwable;
    }
  }

  class Parent {};

  class Child extends Parent {};

  @Test public void covariantReturn() {
    Promise<Parent> some = Promises.promise(new Parent());
    Function<Parent, Promise<Child>> f = p -> Promises.promise(new Child());
    Promise<Parent> mapped = some.<Parent> flatMap(f);
    assertThat(mapped.claim(), notNullValue());
  }
}
