package com.almworks.util.collections;

import java.util.*;

public class MapBuilder<K, V> {
  private Map<K, V> myMap;

  private MapBuilder(Map<K, V> map) {
    myMap = map;
  }

  public static <K, V> MapBuilder<K, V> hashMap() {
    return new MapBuilder<K, V>(new HashMap<K, V>());
  }

  public static <K, V> MapBuilder<K, V> linkedHashMap() {
    return new MapBuilder<K, V>(new LinkedHashMap<K, V>());
  }

  public static <K, V> MapBuilder<K, V> treeMap() {
    return new MapBuilder<K, V>(new TreeMap<K, V>());
  }

  public MapBuilder<K, V> map(K key, V value) {
    myMap.put(key, value);
    return this;
  }

  public Map<K, V> create() {
    Map<K, V> r = myMap;
    myMap = null; // protect from further modification here
    return r;
  }

  public Map<K, V> createUnmodifiable() {
    return Collections.unmodifiableMap(create());
  }

  public Map<K, V> createSynchronized() {
    return Collections.synchronizedMap(create());
  }
}

