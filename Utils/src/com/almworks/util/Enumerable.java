package com.almworks.util;

import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Map;

public class Enumerable <E extends Enumerable> {
  private static final Map<Class, Map<String, Enumerable>> ourRegistry = Collections15.hashMap();

  private final String myName;

  protected Enumerable(String name) {
    assert name != null;
    myName = name;
    register(this);
  }

  public final String getName() {
    // must be final - called from constructor
    return myName;
  }

  public String toString() {
    return myName;
  }

  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  public final int hashCode() {
    return super.hashCode();
  }

  public static synchronized <E extends Enumerable> E forName(Class<E> clazz, String name) {
    Map<String, Enumerable> map = ourRegistry.get(getKeyClass(clazz));
    return map == null ? null : (E) map.get(name);
  }

  protected static synchronized int count(Class<? extends Enumerable> clazz) {
    Map<String, Enumerable> map = ourRegistry.get(getKeyClass(clazz));
    return map == null ? 0 : map.size();
  }

  protected static synchronized <E extends Enumerable<E>> Collection<E> getAll(Class<E> clazz) {
    Map<String, Enumerable> map = ourRegistry.get(getKeyClass(clazz));
    return map != null ? (Collection) map.values() : (Collection) Collections15.emptyCollection();
  }

  private static synchronized void register(Enumerable e) {
    Class classKey = getKeyClass(e.getClass());
    Map<String, Enumerable> map = ourRegistry.get(classKey);
    if (map == null) {
      map = Collections15.hashMap();
      ourRegistry.put(classKey, map);
    }
    String name = e.getName();
    Enumerable existing = map.get(name);
    if (existing != null)
      throw new IllegalStateException("enum for class " + classKey + " already contains " + existing);
    map.put(name, e);
  }

  private static Class getKeyClass(Class clazz) {
    while (true) {
      Class superclazz = clazz.getSuperclass();
      if (superclazz == null || Enumerable.class.equals(superclazz))
        return clazz;
      clazz = superclazz;
    }
  }
}
