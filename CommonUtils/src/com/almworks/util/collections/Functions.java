package com.almworks.util.collections;

import com.almworks.util.Pair;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Function2;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/** Commonly used functions from JDK as objects that can be given to e.g. with {@link Functional} functions. */
public class Functions {
  private Functions() {}

  public static final Function2<String, String, String> CONCAT = new Function2<String, String, String>() {
    @Override
    public String invoke(String s, String s1) {
      return s + s1;
    }
  };

  public static Function<Map.Entry, Pair> MAP_ENTRY_TO_PAIR = new Function<Map.Entry, Pair>() {
    @Override
    public Pair invoke(Map.Entry entry) {
      return Pair.create(entry.getKey(), entry.getValue());
    }
  };

  public static <T1, T2> Function<Map.Entry<T1, T2>, Pair<T1, T2>> mapEntryToPair() {
    return (Function)MAP_ENTRY_TO_PAIR;
  }

  public static <T> Condition<T> contains(@Nullable final Collection<? extends T> c) {
    return c == null ? Condition.<T>always() : new Condition<T>() {
      @Override
      public boolean isAccepted(T value) {
        return c.contains(value);
      }
    };
  }

  public static Function<Map.Entry, Object> GET_KEY = new Function<Map.Entry, Object>() {
    @Override
    public Object invoke(Map.Entry entry) {
      return entry.getKey();
    }
  };

  public static <T> Function<Map.Entry<T, ?>, T> getKey() {
    return (Function) GET_KEY;
  }

  public static Function<Map.Entry, Object> GET_VALUE = new Function<Map.Entry, Object>() {
    @Override
    public Object invoke(Map.Entry entry) {
      return entry.getValue();
    }
  };
}

