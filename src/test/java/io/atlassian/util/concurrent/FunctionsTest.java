package io.atlassian.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionsTest {
  @Test public void fromSupplier() {
    final Supplier<Integer> supplier = new Supplier<Integer>() {
      int count = 0;

      public Integer get() {
        return count++;
      }
    };
    final Function<String, Integer> function = Functions.fromSupplier(supplier);
    assertEquals(Integer.valueOf(0), function.apply("some"));
    assertEquals(Integer.valueOf(1), supplier.get());
  }

  @Test(expected = NullPointerException.class) public void fromSupplierNotNull() {
    Functions.fromSupplier(null);
  }

  @Test public void identity() {
    final Function<String, String> function = Function.identity();
    assertSame("same", function.apply("same"));
  }
}
