package com.atlassian.util.concurrent;

import java.util.concurrent.Executor;

class CallerExecutor implements Executor {
  public void execute(final Runnable command) {
    command.run();
  }
}