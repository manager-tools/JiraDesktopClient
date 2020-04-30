package com.almworks.util.xml;

import com.almworks.util.collections.MultiMap;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

public class XmlTextAdjusterHelper {
  private final MultiMap myMap = new MultiMap();

  public <T> void push(TypedKey<T> key, T value) {
    myMap.add(key, value);
  }

  @Nullable
  public <T> T pop(TypedKey<T> key) {
    return (T)myMap.removeLast(key);
  }

  @Nullable
  public <T> T get(TypedKey<T> key) {
    return (T)myMap.getLast(key);
  }
}
