package io.atlassian.util.concurrent;

import io.atlassian.util.concurrent.Timeout.TimeSupplier;

import java.util.concurrent.TimeUnit;

class MockTimeSupplier implements TimeSupplier {
  private int currentTimeCalled;
  private final long time;
  private final TimeUnit unit;

  MockTimeSupplier(final long time, final TimeUnit unit) {
    this.time = time;
    this.unit = unit;
  }

  public long currentTime() {
    return time + currentTimeCalled++;
  }

  public TimeUnit precision() {
    return unit;
  }
}