package com.almworks.util.collections;


import org.almworks.util.Collections15;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class MapAdapters {
  public static <K, V> MapSource<K, V> getMapSource(final Map<K, V> map) {
    return new MapSource<K, V>() {
      public V get(K key) {
        return map.get(key);
      }

      public MapIterator<K, V> iterator() {
        return getMapIterator(map);
      }
    };
  }

  public static <K, V> MapSource<K, V> getMapSource(final K oneKey, final V oneValue) {
    return new MapSource<K, V>() {
      public V get(K key) {
        if (key == oneKey || (key != null && key.equals(oneKey)))
          return oneValue;
        else
          return null;
      }

      public MapIterator<K, V> iterator() {
        return getMapIterator(oneKey, oneValue);
      }
    };
  }

  private static <K,V> MapIterator<K, V> getMapIterator(final K oneKey, final V oneValue) {
    return new MapIterator<K, V>() {
      boolean myStepped = false;

      public boolean next() {
        if (myStepped)
          return false;
        myStepped = true;
        return true;
      }

      public K lastKey() {
        if (!myStepped)
          throw new NoSuchElementException("not iterated");
        return oneKey;
      }

      public V lastValue() {
        if (!myStepped)
          throw new NoSuchElementException("not iterated");
        return oneValue;
      }
    };
  }

  public static <K, V> MapIterator<K, V> getMapIterator(final Map<K, V> map) {
    return new MapIterator<K, V>() {
      private K myLastKey = null;
      private V myLastValue = null;
      private boolean myLastKeySet = false;
      private boolean myLastValueSet = false;
      private Iterator<K> myIterator = map.keySet().iterator();

      public boolean next() {
        if (!myIterator.hasNext())
          return false;
        myLastKeySet = false;
        myLastValueSet = false;
        myLastKey = myIterator.next();
        myLastKeySet = true;
        return true;
      }

      public K lastKey() {
        if (!myLastKeySet)
          throw new NoSuchElementException("next() was not called or threw error");
        return myLastKey;
      }

      public V lastValue() {
        if (!myLastValueSet) {
          myLastValue = map.get(lastKey());
          myLastValueSet = true;
        }
        return myLastValue;
      }
    };
  }

  public static <K, V> Map<K, V> getHashMap(MapSource<K, V> mapSource) {
    Map<K, V> result = Collections15.hashMap();
    MapIterator<K, V> it = mapSource.iterator();
    while (it.next())
      result.put(it.lastKey(), it.lastValue());
    return result;
  }

  public static <K, V> Map<K, V> getHashMap(K[] keys, V[] values) {
    assert keys.length == values.length;
    Map<K, V> result = Collections15.hashMap();
    for (int i = 0; i < keys.length; i++)
      result.put(keys[i], values[i]);
    return result;
  }

  public static <K, V> MapSource<K, V> getMapSource(K[] keys, V[] values) {
    return getMapSource(getHashMap(keys, values));
  }
}
