package com.almworks.dup.util;

import java.util.Collections;
import java.util.Map;

public class TypedKey <T> {
  private final String myName;

  public TypedKey(String name) {
    assert name != null;
    myName = name;
  }

  public final String getName() {
    return myName;
  }

  public String toString() {
    return myName;
  }

  public T getFromMap(Map<? extends TypedKey, ?> map) {
    Object value = map.get(this);
    return (T) value;
  }

  public T putToMap(Map<? extends TypedKey, ?> map, T value) {
    //noinspection RawUseOfParameterizedType
    return (T) ((Map) map).put(this, value);
  }

  public final T cast(Object object) {
    return (T) object;
  }

  public Map<TypedKey, T> singletonMap(T value) {
    //noinspection RawUseOfParameterizedType
    return Collections.singletonMap((TypedKey) this, value);
  }
}
