package com.atlassian.util.concurrent.atomic;

import static com.google.common.base.Suppliers.ofInstance;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.base.Supplier;

public class AtomicReferenceArrayTest {

  @Test public void getAndSetArrayIfNormal() {
    final String from = "from";
    final String to = "to";
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
    assertEquals(to, ref.getOrSetAndGetIf(0, from, ofInstance(to)));
  }

  @Test public void getAndSetArrayIfNormalValue() {
    final String from = "from";
    final String to = "to";
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
    assertEquals(to, ref.getOrSetAndGetIf(0, from, to));
  }

  @Test public void getAndSetArrayIfNormalSupplierValue() {
    final String from = "from";
    final String to = "to";
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
    assertEquals(to, ref.getOrSetAndGetIf(0, from, ofInstance(to)));
  }

  @Test(expected = IllegalArgumentException.class) public void getAndSetIfArrayValueNPE() {
    Atomics.getAndSetIf((AtomicReferenceArray<String>) null, 0, "", "");
  }

  @Test public void getAndSetArrayIfNull() {
    final String to = "to";
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { null });
    assertEquals(to, Atomics.getAndSetIfNull(ref, 0, ofInstance(to)));
  }

  @Test public void getAndSetArrayContended() {
    final String from = "from";
    final String to = "to";
    final String different = "different";
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
    assertEquals(different, ref.getOrSetAndGetIf(0, from, new Supplier<String>() {
      public String get() {
        // being called, set the reference so the CAS fails
        ref.set(0, different);
        return to;
      }
    }));
  }

  @Test public void getAndSetArrayReturnsOldValueIfChanged() {
    final String old = "old";
    final String from = "from";
    final String to = "to";
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { old });
    assertEquals(old, ref.getOrSetAndGetIf(0, from, ofInstance(to)));
  }

  @Test public void getAndSetArraySameValue() {
    final String from = "from";
    final String to = from;
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
    assertEquals(to, ref.getOrSetAndGetIf(0, from, ofInstance(to)));
  }

  @Test public void getAndSetArraySameValueDifferent() {
    final String from = "from";
    final String to = from;
    final String different = "blah";
    final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { different });
    assertEquals(different, ref.getOrSetAndGetIf(0, from, ofInstance(to)));
  }

  @Test(expected = IndexOutOfBoundsException.class) public void getAndSetArrayThrowsIndexOutOfBounds() {
    Atomics.getAndSetIf(new AtomicReferenceArray<String>(new String[0]), 0, "test", ofInstance("blah"));
  }
}
