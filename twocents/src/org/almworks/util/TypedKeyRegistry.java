package org.almworks.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Stores name-to-key map of registered TypedKeys.
 *
 * @author sereda
 */
public class TypedKeyRegistry<K extends TypedKey> {
  private final Map<String, K> myMap = Collections15.hashMap();

  void add(K key) {
    K expunged = myMap.put(key.getName(), key);
    assert expunged == null : expunged + " " + key;
  }

  public Collection<K> getRegisteredKeys() {
    return Collections.unmodifiableCollection(myMap.values());
  }

  public K getKey(String name) {
    return myMap.get(name);
  }

  public static <T extends TypedKey> TypedKeyRegistry<T> create() {
    return new TypedKeyRegistry<T>();
  }
}
