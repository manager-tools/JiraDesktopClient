package com.almworks.items.util;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class AttributeMap {
  private final Map<DBAttribute<?>, Object> myMap = Collections15.linkedHashMap();

  public static <T> AttributeMap singleton(DBAttribute<T> attribute, T value) {
    AttributeMap map = new AttributeMap();
    map.put(attribute, value);
    return map;
  }

  public <T> T put(DBAttribute<T> attribute, T value) {
    if (value != null)
      return (T) myMap.put(attribute, value);
    else
      return (T) myMap.remove(attribute);
  }

  public <T> void putFrom(AttributeMap other, DBAttribute<T> attribute) {
    put(attribute, other.get(attribute));
  }

  @NotNull
  public AttributeMap copy() {
    AttributeMap copy = new AttributeMap();
    copy.putAll(this);
    return copy;
  }

  public void putAll(AttributeMap map) {
    if (map == null) return;
    myMap.putAll(map.myMap);
  }

  public <T> T get(DBAttribute<T> attribute) {
    return (T) myMap.get(attribute);
  }

  public boolean containsKey(DBAttribute<?> attribute) {
    return myMap.containsKey(attribute);
  }

  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  public Set<DBAttribute<?>> keySet() {
    return Collections.unmodifiableSet(myMap.keySet());
  }

  public <V> V fold(V parameter, AttributeValueFunction<V> function) {
    V pass = parameter;
    for (Map.Entry<DBAttribute<?>, Object> e : myMap.entrySet()) {
      Object value = e.getValue();
      pass = function.f((DBAttribute<Object>) e.getKey(), value, pass);
    }
    return pass;
  }

  public int size() {
    return myMap.size();
  }

  public void clear() {
    myMap.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Map<DBAttribute<?>, Object> theirMap = ((AttributeMap) o).myMap;

    if (myMap.size() != theirMap.size())
      return false;

    for (Map.Entry<DBAttribute<?>, Object> entry : myMap.entrySet()) {
      if (!DatabaseUtil.valueEquals(entry.getValue(), theirMap.get(entry.getKey())))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (Map.Entry<DBAttribute<?>, Object> entry : myMap.entrySet()) {
      Object value = entry.getValue();
      h += entry.getKey().hashCode() ^ DatabaseUtil.valueHash(value);
    }
    return h;
  }

  @Override
  public String toString() {
    return myMap.toString();
  }
}
