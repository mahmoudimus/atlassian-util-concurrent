package io.atlassian.util.concurrent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class MemoryUnitTest {
  @Test public void bytes() {
    assertThat(Memory.Unit.Bytes.bytes, is(1L));
  }

  @Test public void kilobytes() {
    assertThat(Memory.Unit.KB.bytes, is(1024L));
  }

  @Test public void megabytes() {
    assertThat(Memory.Unit.MB.bytes, is(1024L * 1024));
  }

  @Test public void gigabytes() {
    assertThat(Memory.Unit.GB.bytes, is(1024L * 1024 * 1024));
  }

  @Test public void terabytes() {
    assertThat(Memory.Unit.TB.bytes, is(1024L * 1024 * 1024 * 1024));
  }
}
