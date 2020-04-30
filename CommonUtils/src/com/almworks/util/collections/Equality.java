package com.almworks.util.collections;

import org.almworks.util.Util;

public interface Equality<T> {
  Equality GENERAL = new Equality() {
    public boolean areEqual(Object o, Object o1) {
      return Util.equals(o, o1);
    }
  };

  Equality IDENTITY = new Equality() {
    public boolean areEqual(Object o1, Object o2) {
      return o1 == o2;
    }
  };

  static <T> Equality<T> general() {
    //noinspection unchecked
    return GENERAL;
  }

  Equality<String> EMPTY_TEXT = new Equality<String>() {
    @Override
    public boolean areEqual(String o1, String o2) {
      o1 = normalize(o1);
      o2 = normalize(o2);
      return Util.equals(o1, o2);
    }

    private String normalize(String text) {
      return text != null && text.length() > 0 ? text : null;
    }
  };

  Equality<String> TRIM_EMPTY_TEXT = new Equality<String>() {
    @Override
    public boolean areEqual(String o1, String o2) {
      o1 = normalize(o1);
      o2 = normalize(o2);
      return Util.equals(o1, o2);
    }

    private String normalize(String text) {
      if (text == null) return null;
      text = text.trim();
      if (text.length() == 0) text = null;
      return text;
    }
  };

  boolean areEqual(T o1, T o2);
}
