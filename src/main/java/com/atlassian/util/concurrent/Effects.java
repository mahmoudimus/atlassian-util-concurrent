package com.atlassian.util.concurrent;

public class Effects {
  private Effects (){}

  private static Effect<Object> NOOP = new Effect<Object>() {
    public void apply(Object a) {};
  };

  public static <E> Effect<E> noop() {
    @SuppressWarnings("unchecked")
    Effect<E> result = (Effect<E>) NOOP;
    return result;
  }
}
