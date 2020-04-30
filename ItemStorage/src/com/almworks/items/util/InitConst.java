package com.almworks.items.util;

import com.almworks.items.api.DBWriter;
import com.almworks.util.commons.Function;

public class InitConst<T> implements Function<DBWriter, T> {
  private final T myValue;

  public InitConst(T value) {
    myValue = value;
  }

  public T invoke(DBWriter argument) {
    return myValue;
  }

  public static <T> InitConst<T> create(T value) {
    return new InitConst<T>(value);
  }
}
