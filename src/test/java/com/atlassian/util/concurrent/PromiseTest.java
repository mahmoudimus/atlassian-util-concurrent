package com.atlassian.util.concurrent;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;

public class PromiseTest {
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
}
