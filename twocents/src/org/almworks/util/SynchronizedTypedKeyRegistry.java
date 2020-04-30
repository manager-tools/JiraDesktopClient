package org.almworks.util;

import java.util.Collection;

public class SynchronizedTypedKeyRegistry<K extends TypedKey> extends TypedKeyRegistry<K> {
  synchronized void add(K key) {
    super.add(key);
  }

  public synchronized Collection<K> getRegisteredKeys() {
    return Collections15.arrayList(super.getRegisteredKeys());
  }

  public synchronized K getKey(String name) {
    return super.getKey(name);
  }
}
