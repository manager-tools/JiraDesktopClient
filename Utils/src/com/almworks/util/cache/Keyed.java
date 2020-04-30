package com.almworks.util.cache;

public abstract class Keyed <K> {
  protected final K myKey;

  protected Keyed(K key) {
    if (key == null)
      throw new NullPointerException("key");
    myKey = key;
  }

  public K key() {
    return myKey;
  }

  public final int hashCode() {
    return key().hashCode();
  }

  public final boolean equals(Object obj) {
    if (!(obj instanceof Keyed))
      return false;
    return key().equals(((Keyed) obj).key());
  }

  public String toString() {
    return getClass().getName() + "(" + key() + ")";
  }
}