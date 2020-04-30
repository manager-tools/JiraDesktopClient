package com.almworks.util;

public class Constant<T> implements Getter<T> {
  private final T myValue;

  public Constant(T value) {
    myValue = value;
  }

  public T get() {
    return myValue;
  }

  public static <T> Constant<T> create(T value) {
    return new Constant<T>(value);
  }
}
