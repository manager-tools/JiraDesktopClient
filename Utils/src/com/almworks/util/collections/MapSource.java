package com.almworks.util.collections;


/**
 * :todoc:
 *
 * @author sereda
 */
public interface MapSource <K, V> {
  V get(K key);

  MapIterator<K, V> iterator();
}
