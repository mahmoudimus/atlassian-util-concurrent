package io.atlassian.util.concurrent;

import static io.atlassian.util.concurrent.Memory.Unit.Bytes;
import static io.atlassian.util.concurrent.Memory.Unit.GB;
import static io.atlassian.util.concurrent.Memory.Unit.KB;
import static io.atlassian.util.concurrent.Memory.Unit.MB;
import static io.atlassian.util.concurrent.Memory.Unit.TB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import io.atlassian.util.concurrent.Memory.Unit;
import io.atlassian.util.concurrent.Memory.UnitComparator;

public class MemoryUnitComparatorTest {
  @Test public void compareBytesToBytes() {
    assertEqual(Bytes, Bytes);
  }

  @Test public void compareBytesToKB() {
    assertLess(Bytes, KB);
  }

  @Test public void compareBytesToMB() {
    assertLess(Bytes, MB);
  }

  @Test public void compareBytesToGB() {
    assertLess(Bytes, GB);
  }

  @Test public void compareBytesToTB() {
    assertLess(Bytes, TB);
  }

  @Test public void compareKBToBytes() {
    assertGreater(KB, Bytes);
  }

  @Test public void compareKBToKB() {
    assertEqual(KB, KB);
  }

  @Test public void compareKBToMB() {
    assertLess(KB, MB);
  }

  @Test public void compareKBToGB() {
    assertLess(KB, GB);
  }

  @Test public void compareKBToTB() {
    assertLess(KB, TB);
  }

  @Test public void compareMBToBytes() {
    assertGreater(MB, Bytes);
  }

  @Test public void compareMBToKB() {
    assertGreater(MB, KB);
  }

  @Test public void compareMBToMB() {
    assertEqual(MB, MB);
  }

  @Test public void compareMBToGB() {
    assertLess(MB, GB);
  }

  @Test public void compareMBToTB() {
    assertLess(MB, TB);
  }

  @Test public void compareGBToBytes() {
    assertGreater(GB, Bytes);
  }

  @Test public void compareGBToKB() {
    assertGreater(GB, KB);
  }

  @Test public void compareGBToMB() {
    assertGreater(GB, MB);
  }

  @Test public void compareGBToGB() {
    assertEqual(GB, GB);
  }

  @Test public void compareGBToTB() {
    assertLess(GB, TB);
  }

  @Test public void compareTBToBytes() {
    assertGreater(TB, Bytes);
  }

  @Test public void compareTBToKB() {
    assertGreater(TB, KB);
  }

  @Test public void compareTBToMB() {
    assertGreater(TB, MB);
  }

  @Test public void compareTBToGB() {
    assertGreater(TB, GB);
  }

  @Test public void compareTBToTB() {
    assertEqual(TB, TB);
  }

  private void assertEqual(Unit from, Unit to) {
    assertCompare(from, to, 0);
  }

  private void assertLess(Unit from, Unit to) {
    assertCompare(from, to, -1);
  }

  private void assertGreater(Unit from, Unit to) {
    assertCompare(from, to, 1);
  }

  private void assertCompare(Unit from, Unit to, int result) {
    assertThat(UnitComparator.INSTANCE.compare(from, to), is(result));
  }
}
