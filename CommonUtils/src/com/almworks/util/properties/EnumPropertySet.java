package com.almworks.util.properties;

import org.almworks.util.Collections15;

import java.util.*;

/**
 * @author dyoma
 */
public final class EnumPropertySet<T> extends AbstractCollection<T> {
  private final Map<T, Integer> myEnum;

  public EnumPropertySet(T ... properties) {
    HashMap<T, Integer> enumeration = Collections15.hashMap(properties.length);
    for (int i = 0; i < properties.length; i++) {
      T property = properties[i];
      enumeration.put(property, i);
    }
    myEnum = Collections.unmodifiableMap(enumeration);
  }

  public static <T> EnumPropertySet<T> create(T ... properties) {
    return new EnumPropertySet<T>(properties);
  }

  public static <T> EnumPropertySet<T> create(Collection<? extends T> collection) {
    //noinspection unchecked
    return new EnumPropertySet<T>(collection.toArray((T[])new Object[collection.size()]));
  }

  public Iterator<T> iterator() {
    return myEnum.keySet().iterator();
  }

  public int size() {
    return myEnum.size();
  }

  public EnumPropertySet<T> with(T ... additional) {
    //noinspection unchecked
    T[] keys = (T[]) new Object[myEnum.size() + additional.length];
    myEnum.keySet().toArray(keys);
    System.arraycopy(additional, 0, keys, myEnum.size(), additional.length);
    return new EnumPropertySet<T>(keys);
  }

  public int getIndex(T key) {
    Integer index = myEnum.get(key);
    assert index != null : key + " " + myEnum.keySet();
    return index;
  }

  public int getSafeIndex(T key, int max) {
    int index = getIndex(key);
    assert index < max : index + " " + max + key;
    assert index >= 0 : index + " " + key;
    return index;
  }

  public int getKnownIndex(T key, int max) {
    Integer index = myEnum.get(key);
    if (index == null) return -1;
    int result = index;
    return 0 <= result && result < max ? result : -1;
  }
}
