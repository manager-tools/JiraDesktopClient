package com.almworks.util.cache;

/**
 * :todoc:
 *
 * @author sereda
 */
public class IntegerKey {
  private final int myInt;

  public IntegerKey(int anInt) {
    myInt = anInt;
  }

  public int getInt() {
    return myInt;
  }

  public int hashCode() {
    return myInt;
  }

  public boolean equals(Object obj) {
    return (obj instanceof IntegerKey && ((IntegerKey) obj).myInt == myInt);
  }

  public String toString() {
    return "" + myInt;
  }
}
