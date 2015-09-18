package io.atlassian.util.concurrent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ExecutorSubmitterTest {
  static class ExecutorSubmitterException extends RuntimeException {
    private static final long serialVersionUID = 1167832478L;
  }

  @Test public void submitCallableGetResult() throws Exception {
    assertThat(Executors.submitter(new CallerExecutor()).submit(() -> "fred!").get(), is("fred!"));
  }

  @Test(expected = ExecutorSubmitterException.class) public void submitCallableGetException() throws Throwable {
    try {
      Executors.submitter(new CallerExecutor()).submit(() -> {
        throw new ExecutorSubmitterException();
      }).get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test public void submitSupplierGetResult() throws Exception {
    assertThat(Executors.submitter(new CallerExecutor()).submitSupplier(() -> "fred!").get(), is("fred!"));
  }

  @Test public void submitSupplierCancelComputation() throws Exception {
    final Executor singleThreadExecutor = command -> {
      Runnable runnable = () -> {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
        command.run();
      };
      new Thread(runnable).start();
    };

    final AtomicBoolean signal = new AtomicBoolean(false);
    final Promise<String> promise = Executors.submitter(singleThreadExecutor).submitSupplier(() -> {
      signal.set(true);
      return "fred";
    });
    promise.cancel(false);
    try {
      promise.get();
      fail("promise.get should have thrown a CancellationException");
    } catch (CancellationException ignored) {}
    Thread.sleep(2000);
    assertThat(signal.get(), is(false));
  }

  @Test(expected = ExecutorSubmitterException.class) public void submitSupplierGetException() throws Throwable {
    try {
      Executors.submitter(new CallerExecutor()).submitSupplier(() -> {
        throw new ExecutorSubmitterException();
      }).get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
