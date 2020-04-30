package com.almworks.util.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class SingleValueMap<K, V> extends AbstractMap<K, V> {
  private final Set<K> myKeys;
  private final V myCommonValue;
  private final Convertor<K,Entry<K,V>> myConvertor = new Convertor<K, Entry<K, V>>() {
    @Override
    public Entry<K, V> convert(K key) {
      return new SimpleEntry<K, V>(key, myCommonValue);
    }
  };
  private final AbstractSet<Entry<K,V>> myEntrySet = new AbstractSet<Entry<K, V>>() {
    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new ConvertingIterator<K, Entry<K, V>>(myKeys.iterator(), myConvertor);
    }

    @Override
    public int size() {
      return myKeys.size();
    }
  };

  public SingleValueMap(Set<K> keys, V commonValue) {
    myKeys = keys;
    myCommonValue = commonValue;
  }

  public static <K, V> SingleValueMap<K, V> create(Set<K> keys, V commonValue) {
    return new SingleValueMap<K, V>(keys, commonValue);
  }

  @Override
  public V get(Object key) {
    return myKeys.contains(key) ? myCommonValue : null;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return myEntrySet;
  }
}
