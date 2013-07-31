package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Memory.bytes;
import static com.atlassian.util.concurrent.Memory.gigabytes;
import static com.atlassian.util.concurrent.Memory.kilobytes;
import static com.atlassian.util.concurrent.Memory.megabytes;
import static com.atlassian.util.concurrent.Memory.terabytes;
import static com.atlassian.util.concurrent.Memory.Unit.Bytes;
import static com.atlassian.util.concurrent.Memory.Unit.GB;
import static com.atlassian.util.concurrent.Memory.Unit.KB;
import static com.atlassian.util.concurrent.Memory.Unit.MB;
import static com.atlassian.util.concurrent.Memory.Unit.TB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.Test;


public class MemoryTest {
  @Test public void createBytes() {
    assertThat(bytes(1), is(new Memory(1, Bytes)));
  }
  @Test public void createKBs() {
    assertThat(kilobytes(2), is(new Memory(2, KB)));
  }
  @Test public void createMBs() {
    assertThat(megabytes(3), is(new Memory(3, MB)));
  }
  @Test public void createGBs() {
    assertThat(gigabytes(4), is(new Memory(4, GB)));
  }
  @Test public void createTBs() {
    assertThat(terabytes(5), is(new Memory(5, TB)));
  }

  //
  // bytes conversions
  //
  
  @Test public void convertBytesToBytes() {
    assertThat(bytes(45636).to(Bytes), is(equalTo(new Memory(45636, Bytes))));
  }

  @Test public void convertBytesToKB() {
    assertThat(bytes(1200).to(KB), is(equalTo(new Memory(1, KB))));
  }

  @Test public void convertBytesToMB() {
    assertThat(bytes(4203740).to(MB), is(equalTo(new Memory(4, MB))));
  }

  @Test public void convertBytesToGB() {
    assertThat(bytes(6500067500L).to(GB), is(equalTo(new Memory(6, GB))));
  }

  @Test public void convertBytesToTB() {
    assertThat(bytes(13500000000123L).to(TB), is(equalTo(new Memory(12, TB))));
  }

  //
  // KB conversions
  //
  
  @Test public void convertKBToBytes() {
    assertThat(kilobytes(12).to(Bytes), is(equalTo(new Memory(12288, Bytes))));
  }

  @Test public void convertKBToKB() {
    assertThat(kilobytes(1232).to(KB), is(equalTo(new Memory(1232, KB))));
  }

  @Test public void convertKBToMB() {
    assertThat(kilobytes(2300).to(MB), is(equalTo(new Memory(2, MB))));
  }

  @Test public void convertKBToGB() {
    assertThat(kilobytes(6500067L).to(GB), is(equalTo(new Memory(6, GB))));
  }

  @Test public void convertKBToTB() {
    assertThat(kilobytes(12900000123L).to(TB), is(equalTo(new Memory(12, TB))));
  }

  //
  // MB conversions
  //
  
  @Test public void convertMBToBytes() {
    assertThat(megabytes(34).to(Bytes), is(equalTo(new Memory(35651584, Bytes))));
  }

  @Test public void convertMBToKB() {
    assertThat(megabytes(122).to(KB), is(equalTo(new Memory(124928, KB))));
  }

  @Test public void convertMBToMB() {
    assertThat(megabytes(2392).to(MB), is(equalTo(new Memory(2392, MB))));
  }

  @Test public void convertMBToGB() {
    assertThat(megabytes(6500067L).to(GB), is(equalTo(new Memory(6347, GB))));
  }

  @Test public void convertMBToTB() {
    assertThat(megabytes(15550123L).to(TB), is(equalTo(new Memory(14, TB))));
  }

  //
  // GB conversions
  //
  
  @Test public void convertGBToBytes() {
    assertThat(gigabytes(31).to(Bytes), is(equalTo(new Memory(33285996544L, Bytes))));
  }

  @Test public void convertGBToKB() {
    assertThat(gigabytes(121).to(KB), is(equalTo(new Memory(126877696, KB))));
  }

  @Test public void convertGBToMB() {
    assertThat(gigabytes(2391).to(MB), is(equalTo(new Memory(2448384, MB))));
  }

  @Test public void convertGBToGB() {
    assertThat(gigabytes(650).to(GB), is(equalTo(new Memory(650, GB))));
  }

  @Test public void convertGBToTB() {
    assertThat(gigabytes(2234L).to(TB), is(equalTo(new Memory(2, TB))));
  }

  //
  // TB conversions
  //
  
  @Test public void convertTBToBytes() {
    assertThat(terabytes(13).to(Bytes), is(equalTo(new Memory(14293651161088L, Bytes))));
  }

  @Test public void convertTBToKB() {
    assertThat(terabytes(11).to(KB), is(equalTo(new Memory(11811160064L, KB))));
  }

  @Test public void convertTBToMB() {
    assertThat(terabytes(91).to(MB), is(equalTo(new Memory(95420416, MB))));
  }

  @Test public void convertTBToGB() {
    assertThat(terabytes(65).to(GB), is(equalTo(new Memory(66560, GB))));
  }

  @Test public void convertTBToTB() {
    assertThat(terabytes(17).to(TB), is(equalTo(new Memory(17, TB))));
  }
}
