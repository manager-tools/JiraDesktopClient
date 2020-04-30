package com.almworks.util.commons;

import com.almworks.util.collections.Convertor;
import org.almworks.util.Util;

public interface Factory<T> extends FactoryE<T, Exception> {
  T create();

  class Const<T> implements Factory<T> {
    public static Const NULL = new Const(null);
    private final T myValue;

    public Const(T value) {
      myValue = value;
    }

    public T create() {
      return myValue;
    }

    public static <T> Factory<T> newConst(T value) {
      return new Const<T>(value);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null) return false;
      if (obj.getClass() != Const.class) return false;
      Const<?> other = (Const<?>) obj;
      return Util.equals(myValue, other.myValue);
    }

    @Override
    public int hashCode() {
      return myValue != null ? myValue.hashCode() : 0;
    }
  }

  class Invoke<T> extends Convertor<Factory<T>, T> {
    private static final Invoke<Object> INSTANCE = new Invoke<Object>();

    @Override
    public T convert(Factory<T> value) {
      return value != null ? value.create() : null;
    }

    public static <T> Convertor<Factory<T>, T> create() {
      //noinspection unchecked
      return (Convertor<Factory<T>, T>) INSTANCE;
    }
  }
}
