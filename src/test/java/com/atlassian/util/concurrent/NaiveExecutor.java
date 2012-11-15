package com.atlassian.util.concurrent;

import java.util.concurrent.Executor;

class NaiveExecutor implements Executor {
  public void execute(final Runnable command) {
    new Thread(command).start();
  }
}