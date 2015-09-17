package io.atlassian.util.concurrent;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AbstractCOWMapKeysViewTest {
  @Test public void contains() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertTrue(map.keySet().contains("one"));
  }

  @Test public void doesntContains() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertFalse(map.keySet().contains("two"));
  }

  @Test public void containsAll() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertTrue(map.keySet().containsAll(singleton("one")));
  }

  @Test public void doesntContainsAll() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertFalse(map.keySet().containsAll(asList("one", "two")));
  }

  @Test public void isEmpty() {
    final Map<String, String> map = map();
    assertTrue(map.keySet().isEmpty());
  }

  @Test public void notEmpty() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertFalse(map.keySet().isEmpty());
  }

  @Test public void size() {
    final Map<String, String> map = map();
    assertEquals(0, map.keySet().size());
    map.put("one", "two");
    assertEquals(1, map.keySet().size());
    map.put("two", "three");
    assertEquals(2, map.keySet().size());
    map.put("three", "four");
    assertEquals(3, map.keySet().size());
    map.remove("two");
    assertEquals(2, map.keySet().size());
    map.clear();
    assertEquals(0, map.keySet().size());
  }

  @Test public void toEmptyArray() {
    final Map<String, String> map = map();
    assertEquals(0, map.keySet().toArray().length);
  }

  @Test public void toNonEmptyArray() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertEquals(1, map.keySet().toArray().length);
    assertArrayEquals(new String[] { "one" }, map.keySet().toArray());
  }

  @Test public void toEmptyArrayCopy() {
    final Map<String, String> map = map();
    assertEquals(0, map.keySet().toArray(new String[0]).length);
  }

  @Test public void toNonEmptyArrayCopy() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertEquals(1, map.keySet().toArray(new String[1]).length);
    assertArrayEquals(new String[] { "one" }, map.keySet().toArray(new String[1]));
  }

  @Test public void toNonEmptyToString() {
    final Map<String, String> map = map();
    map.put("one", "two");
    assertEquals("[one]", map.keySet().toString());
  }

  @Test public void equality() {
    final Map<String, String> map = map();
    final Map<String, String> map2 = new HashMap<String, String>();
    map.put("one", "two");
    map2.put("one", "two");
    assertEquals(map2.keySet(), map.keySet());
    assertEquals(map.keySet(), map2.keySet());
  }

  @Test public void hashCodeEquality() {
    final Map<String, String> map = map();
    final Map<String, String> map2 = new HashMap<String, String>();
    map.put("one", "two");
    map2.put("one", "two");
    assertEquals(map2.keySet().hashCode(), map.keySet().hashCode());
  }

  static Map<String, String> map() {
    return new MutableView();
  }

  static final Map<String, String> emptyMap = Collections.<String, String> emptyMap();

  static class MutableView extends AbstractCopyOnWriteMap<String, String, Map<String, String>> {
    private static final long serialVersionUID = 8917313652796867115L;

    MutableView() {
      super(emptyMap, View.Type.LIVE);
    }

    @Override <N extends java.util.Map<? extends String, ? extends String>> java.util.Map<String, String> copy(final N map) {
      return new HashMap<>(map);
    }
  }
}
