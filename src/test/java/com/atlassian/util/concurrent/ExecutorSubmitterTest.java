package com.atlassian.util.concurrent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

public class ExecutorSubmitterTest {
  static class ExecutorSubmitterException extends RuntimeException {
    private static final long serialVersionUID = 1167832478L;
  }

  @Test public void submitCallableGetResult() throws Exception {
    assertThat(Executors.submitter(new CallerExecutor()).submit(new Callable<String>() {
      @Override public String call() throws Exception {
        return "fred!";
      }
    }).get(), is("fred!"));
  }

  @Test(expected = ExecutorSubmitterException.class) public void submitCallableGetException() throws Throwable {
    try {
      Executors.submitter(new CallerExecutor()).submit(new Callable<String>() {
        @Override public String call() throws Exception {
          throw new ExecutorSubmitterException();
        }
      }).get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test public void submitSupplierGetResult() throws Exception {
    assertThat(Executors.submitter(new CallerExecutor()).submit(new Supplier<String>() {
      @Override public String get() {
        return "fred!";
      }
    }).get(), is("fred!"));
  }

  @Test(expected = ExecutorSubmitterException.class) public void submitSupplierGetException() throws Throwable {
    try {
      Executors.submitter(new CallerExecutor()).submit(new Supplier<String>() {
        @Override public String get() {
          throw new ExecutorSubmitterException();
        }
      }).get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
