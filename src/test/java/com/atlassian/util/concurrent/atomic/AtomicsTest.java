package com.atlassian.util.concurrent.atomic;

import static com.google.common.base.Suppliers.ofInstance;
import static org.junit.Assert.assertEquals;

import com.atlassian.util.concurrent.Supplier;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class AtomicsTest {

    //
    // AtomicReference tests
    //

    @Test
    public void getAndSetIfNormal() {
        final String from = "from";
        final String to = "to";
        final AtomicReference<String> ref = new AtomicReference<String>(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, ofInstance(to)));
    }

    @Test
    public void getAndSetIfNormalValue() {
        final String from = "from";
        final String to = "to";
        final AtomicReference<String> ref = new AtomicReference<String>(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, to));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndSetIfRefNPE() {
        Atomics.getAndSetIf((AtomicReference<String>) null, "", ofInstance(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndSetIfRefValueNPE() {
        Atomics.getAndSetIf((AtomicReference<String>) null, "", "");
    }

    @Test
    public void getAndSetIfNull() {
        final String to = "to";
        final AtomicReference<String> ref = new AtomicReference<String>(null);
        assertEquals(to, Atomics.getAndSetIfNull(ref, ofInstance(to)));
    }

    @Test
    public void getAndSetContended() {
        final String from = "from";
        final String to = "to";
        final String different = "different";
        final AtomicReference<String> ref = new AtomicReference<String>(from);
        assertEquals(different, Atomics.getAndSetIf(ref, from, new Supplier<String>() {
            public String get() {
                // being called, set the reference so the CAS fails
                ref.set(different);
                return to;
            }
        }));
    }

    @Test
    public void getAndSetReturnsOldValueIfChanged() {
        final String old = "old";
        final String from = "from";
        final String to = "to";
        final AtomicReference<String> ref = new AtomicReference<String>(old);
        assertEquals(old, Atomics.getAndSetIf(ref, from, ofInstance(to)));
    }

    @Test
    public void getAndSetSameValue() {
        final String from = "from";
        final String to = from;
        final AtomicReference<String> ref = new AtomicReference<String>(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, ofInstance(to)));
    }

    @Test
    public void getAndSetSameValueDifferent() {
        final String from = "from";
        final String to = from;
        final String different = "blah";
        final AtomicReference<String> ref = new AtomicReference<String>(different);
        assertEquals(different, Atomics.getAndSetIf(ref, from, ofInstance(to)));
    }

    //
    // AtomicReferenceArray tests
    //

    @Test
    public void getAndSetArrayIfNormal() {
        final String from = "from";
        final String to = "to";
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
        assertEquals(to, Atomics.getAndSetIf(ref, 0, from, ofInstance(to)));
    }

    @Test
    public void getAndSetArrayIfNormalValue() {
        final String from = "from";
        final String to = "to";
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
        assertEquals(to, Atomics.getAndSetIf(ref, 0, from, to));
    }

    @Test
    public void getAndSetArrayIfNormalSupplierValue() {
        final String from = "from";
        final String to = "to";
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
        assertEquals(to, Atomics.getAndSetIf(ref, 0, from, ofInstance(to)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndSetIfArrayNPE() {
        Atomics.getAndSetIf((AtomicReferenceArray<String>) null, 0, "", ofInstance(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndSetIfArrayValueNPE() {
        Atomics.getAndSetIf((AtomicReferenceArray<String>) null, 0, "", "");
    }

    @Test
    public void getAndSetArrayIfNull() {
        final String to = "to";
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { null });
        assertEquals(to, Atomics.getAndSetIfNull(ref, 0, ofInstance(to)));
    }

    @Test
    public void getAndSetArrayContended() {
        final String from = "from";
        final String to = "to";
        final String different = "different";
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
        assertEquals(different, Atomics.getAndSetIf(ref, 0, from, new Supplier<String>() {
            public String get() {
                // being called, set the reference so the CAS fails
                ref.set(0, different);
                return to;
            }
        }));
    }

    @Test
    public void getAndSetArrayReturnsOldValueIfChanged() {
        final String old = "old";
        final String from = "from";
        final String to = "to";
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { old });
        assertEquals(old, Atomics.getAndSetIf(ref, 0, from, ofInstance(to)));
    }

    @Test
    public void getAndSetArraySameValue() {
        final String from = "from";
        final String to = from;
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { from });
        assertEquals(to, Atomics.getAndSetIf(ref, 0, from, ofInstance(to)));
    }

    @Test
    public void getAndSetArraySameValueDifferent() {
        final String from = "from";
        final String to = from;
        final String different = "blah";
        final AtomicReferenceArray<String> ref = new AtomicReferenceArray<String>(new String[] { different });
        assertEquals(different, Atomics.getAndSetIf(ref, 0, from, ofInstance(to)));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getAndSetArrayThrowsIndexOutOfBounds() {
        Atomics.getAndSetIf(new AtomicReferenceArray<String>(new String[0]), 0, "test", ofInstance("blah"));
    }

    //
    // AtomicLong tests
    //

    @Test
    public void getAndSetLongIfNormal() {
        final long from = 1;
        final long to = 2;
        final AtomicLong ref = new AtomicLong(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, to));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndSetIfLongNPE() {
        Atomics.getAndSetIf((AtomicLong) null, 0, 0);
    }

    @Test
    public void getAndSetLongReturnsOldValueIfChanged() {
        final long old = 1;
        final long from = 2;
        final long to = 3;
        final AtomicLong ref = new AtomicLong(old);
        assertEquals(old, Atomics.getAndSetIf(ref, from, to));
    }

    @Test
    public void getAndSetLongSameValue() {
        final long from = 1;
        final long to = from;
        final AtomicLong ref = new AtomicLong(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, to));
    }

    @Test
    public void getAndSetLongSameValueDifferent() {
        final long from = 1;
        final long to = from;
        final int different = 3;
        final AtomicLong ref = new AtomicLong(different);
        assertEquals(different, Atomics.getAndSetIf(ref, from, to));
    }

    //
    // AtomicInteger tests
    //

    @Test
    public void getAndSetIntegerIfNormal() {
        final int from = 1;
        final int to = 2;
        final AtomicInteger ref = new AtomicInteger(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, to));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndSetIfIntegerNPE() {
        Atomics.getAndSetIf((AtomicInteger) null, 0, 0);
    }

    @Test
    public void getAndSetIntegerReturnsOldValueIfChanged() {
        final int old = 1;
        final int from = 2;
        final int to = 3;
        final AtomicInteger ref = new AtomicInteger(old);
        assertEquals(old, Atomics.getAndSetIf(ref, from, to));
    }

    @Test
    public void getAndSetIntegerSameValue() {
        final int from = 1;
        final int to = from;
        final AtomicInteger ref = new AtomicInteger(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, to));
    }

    @Test
    public void getAndSetIntegerSameValueDifferent() {
        final int from = 1;
        final int to = from;
        final int different = 3;
        final AtomicInteger ref = new AtomicInteger(different);
        assertEquals(different, Atomics.getAndSetIf(ref, from, to));
    }

    //
    // AtomicBoolean tests
    //

    @Test
    public void getAndSetBooleanIfNormal() {
        final boolean from = false;
        final boolean to = true;
        final AtomicBoolean ref = new AtomicBoolean(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, to));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndSetIfBooleanNPE() {
        Atomics.getAndSetIf((AtomicBoolean) null, false, false);
    }

    @Test
    public void getAndSetBooleanReturnsOldValueIfChanged() {
        final boolean old = true;
        final boolean from = false;
        final boolean to = true;
        final AtomicBoolean ref = new AtomicBoolean(old);
        assertEquals(old, Atomics.getAndSetIf(ref, from, to));
    }

    @Test
    public void getAndSetBooleanSameValue() {
        final boolean from = false;
        final boolean to = from;
        final AtomicBoolean ref = new AtomicBoolean(from);
        assertEquals(to, Atomics.getAndSetIf(ref, from, to));
    }

    @Test
    public void getAndSetBooleanSameValueDifferent() {
        final boolean from = false;
        final boolean to = from;
        final boolean different = true;
        final AtomicBoolean ref = new AtomicBoolean(different);
        assertEquals(different, Atomics.getAndSetIf(ref, from, to));
    }
}
