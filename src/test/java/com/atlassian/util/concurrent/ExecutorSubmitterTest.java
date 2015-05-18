package com.atlassian.util.concurrent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.junit.Test;

public class ExecutorSubmitterTest {
  static class ExecutorSubmitterException extends RuntimeException {
    private static final long serialVersionUID = 1167832478L;
  }

  @Test public void submitCallableGetResult() throws Exception {
    assertThat(Executors.submitter(new CallerExecutor()).submit((Callable<String>) () -> "fred!").get(), is("fred!"));
  }

  @Test(expected = ExecutorSubmitterException.class) public void submitCallableGetException() throws Throwable {
    try {
      Executors.submitter(new CallerExecutor()).submit((Callable<String>) () -> {
        throw new ExecutorSubmitterException();
      }).get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test public void submitSupplierGetResult() throws Exception {
    assertThat(Executors.submitter(new CallerExecutor()).submit((Supplier<String>) () -> "fred!").get(), is("fred!"));
  }

  @Test(expected = ExecutorSubmitterException.class) public void submitSupplierGetException() throws Throwable {
    try {
      Executors.submitter(new CallerExecutor()).submit((Supplier<String>) () -> {
        throw new ExecutorSubmitterException();
      }).get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
