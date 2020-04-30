package com.almworks.items.cache.util;

import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Factory;
import org.almworks.util.Collections15;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DefaultMap<K, V> extends AbstractMap<K, V> {
  private final Map<K, V> myMap;
  private final Factory<V> myDefault;

  public DefaultMap(Map<K, V> map, Factory<V> aDefault) {
    myMap = map;
    myDefault = aDefault;
  }

  public static <T> DefaultMap<T, LongSet> longSet() {
    return longSet(Collections15.<T, LongSet>hashMap());
  }

  public static <T> DefaultMap<T, LongSet> longSet(Map<T, LongSet> map) {
    return new DefaultMap<T, LongSet>(map, new Factory<LongSet>() {
      @Override
      public LongSet create() {
        return new LongSet();
      }
    });
  }


  @Override
  public Set<Entry<K, V>> entrySet() {
    return myMap.entrySet();
  }

  @Override
  public int size() {
    return myMap.size();
  }

  public V getOrCreate(K key) {
    if (containsKey(key)) return get(key);
    V value = myDefault.create();
    put(key, value);
    return value;
  }

  @Override
  public V get(Object key) {
    return myMap.get(key);
  }

  @Override
  public V put(K key, V value) {
    return myMap.put(key, value);
  }

  @Override
  public boolean containsValue(Object value) {
    return myMap.containsValue(value);
  }

  @Override
  public boolean containsKey(Object key) {
    return myMap.containsKey(key);
  }

  @Override
  public V remove(Object key) {
    return myMap.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    myMap.putAll(m);
  }

  @Override
  public void clear() {
    myMap.clear();
  }

  @Override
  public Set<K> keySet() {
    return myMap.keySet();
  }

  @Override
  public Collection<V> values() {
    return myMap.values();
  }
}
