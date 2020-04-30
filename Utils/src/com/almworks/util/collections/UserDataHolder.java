package com.almworks.util.collections;

import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * @author dyoma
 */
public class UserDataHolder {
  private final Map<TypedKey<?>, Object> myUserData = Collections15.hashMap();

  public <T> T getUserData(TypedKey<T> key) {
    synchronized(getUserDataLock()) {
      return key.getFrom(myUserData);
    }
  }

  public <T> void putUserData(TypedKey<T> key, @Nullable T data) {
    synchronized(getUserDataLock()) {
      if (data == null)
        myUserData.remove(key);
      else
        key.putTo(myUserData, data);
    }
  }

  public Object getUserDataLock() {
    return myUserData;
  }

  /**
   * @return true if the map has been updated
   */
  public <T> boolean putIfAbsent(TypedKey<T> key, T value) {
    synchronized (getUserDataLock()) {
      T existing = key.getFrom(myUserData);
      if (existing == null) key.putTo(myUserData, value);
      return existing == null;
    }
  }

  public <T> boolean replace(TypedKey<T> key, T expected, T newValue) {
    synchronized (getUserDataLock()) {
      T current = key.getFrom(myUserData);
      boolean replace = Util.equals(current, expected);
      if (replace) key.putTo(myUserData, newValue);
      return replace;
    }
  }

  public Collection<TypedKey<?>> keySet() {
    synchronized (getUserDataLock()) {
      return Collections15.arrayList(myUserData.keySet());
    }
  }

  public static <T> void copy(TypedKey<T> key, UserDataHolder source, UserDataHolder target) {
    target.putUserData(key, source.getUserData(key));
  }
}
