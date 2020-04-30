package com.almworks.dup.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class, used instead of <code>enum</code> where external String id is needed.
 */
public class ExternalizableEnum {
  private static final Map<Class, Map<String, ExternalizableEnum>> ourRegistry = new HashMap<Class, Map<String, ExternalizableEnum>>();

  private final String myName;

  protected ExternalizableEnum(String name) {
    assert name != null;
    myName = name;
    register(this);
  }

  public final String getExternalName() {
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

  public static synchronized ExternalizableEnum forExternalName(Class clazz, String name) {
    Map<String, ExternalizableEnum> map = ourRegistry.get(clazz);
    return map == null ? null : map.get(name);
  }

  private static synchronized void register(ExternalizableEnum e) {
    Class classKey = e.getClass();
    Map<String, ExternalizableEnum> map = ourRegistry.get(classKey);
    if (map == null) {
      map = new HashMap<String, ExternalizableEnum>();
      ourRegistry.put(classKey, map);
    }
    String name = e.getExternalName();
    ExternalizableEnum existing = map.get(name);
    if (existing != null)
      throw new IllegalStateException("enum for class " + classKey + " already contains " + existing);
    map.put(name, e);
  }
}
