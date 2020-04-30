package com.almworks.util.cache2;

public abstract class LongKeyed {
  protected final long myKey;

  protected LongKeyed(long key) {
    myKey = key;
  }

  public long key() {
    return myKey;
  }

  public final int hashCode() {
    return (int)key();
  }

  public final boolean equals(Object obj) {
    if (!(obj instanceof LongKeyed))
      return false;
    return key() == ((LongKeyed) obj).key();
  }

  public String toString() {
    return getClass().getName() + "(" + key() + ")";
  }
}
