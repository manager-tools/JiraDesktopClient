package com.almworks.util;

import org.jetbrains.annotations.NotNull;

public class NonLocalReturn extends CheapCheckedException {
  private final Object myValue;

  public static void nlreturn(Object value) throws NonLocalReturn {
    throw new NonLocalReturn(value);
  }

  public static void nlreturn() throws NonLocalReturn {
    throw new NonLocalReturn();
  }

  public NonLocalReturn(Object value) {
    myValue = value;
  }

  public NonLocalReturn() {
    this(null);
  }

  public Object getValue() {
    return myValue;
  }

  public <T> T getValue(@NotNull Class<T> valueClass) {
    if(valueClass.isInstance(myValue)) {
      return (T)myValue;
    }
    return null;
  }
}
