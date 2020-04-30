package com.almworks.util.collections;

import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author : Dyoma
 */
public class InheritedMapping <K, V> implements Map<K, V> {
  private final Map<K, V> myMap = Collections15.hashMap();
  private final Map<K, V> myParent;

  public InheritedMapping(Map<K, V> parent) {
    if (parent == null)
      throw new NullPointerException("parent");
    myParent = parent;
  }

  public int size() {
    return keySet().size();
  }

  public boolean isEmpty() {
    return myMap.isEmpty() && myParent.isEmpty();
  }

  public boolean containsKey(Object key) {
    return myMap.containsKey(key) || myParent.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return myMap.containsValue(value) || myParent.containsValue(value);
  }

  public V get(Object key) {
    return myMap.containsKey(key) ? myMap.get(key) : myParent.get(key);
  }

  public V put(K k, V v) {
    myMap.put(k, v);
    return v;
  }

  public V remove(Object key) {
    return myMap.remove(key);
  }

  public void putAll(Map<? extends K, ? extends V> map) {
    myMap.putAll(map);
  }

  public void clear() {
    myMap.clear();
  }

  public Set<K> keySet() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public Collection<V> values() {
    return FlattenCollection.create(myMap.values(), myParent.values());
  }

  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public static <K, V> Map<K, V> create(Map<K, V> parent) {
    return new InheritedMapping<K, V>(parent);
  }
}
