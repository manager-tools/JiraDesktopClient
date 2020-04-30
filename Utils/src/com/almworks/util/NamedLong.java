package com.almworks.util;

/**
 * :todoc:
 *
 * @author sereda
 */
public class NamedLong extends NamedConstant<Long> {
  private final long myValue;

  public NamedLong(long value, String name) {
    super(name);
    myValue = value;
  }

  public NamedLong(long value, String name, NamedConstantRegistry registry) {
    super(name, registry);
    myValue = value;
  }

  public long getLong() {
    return myValue;
  }

  public Long value() {
    return new Long(myValue);
  }

  public int hashCode() {
    return (int) myValue ^ (int) (myValue >>> 32);
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof NamedLong))
      return false;
    return myValue == ((NamedLong) obj).getLong();
  }

  public String toString() {
    return myName + "::" + myValue;
  }

  public boolean equals(long value) {
    return myValue == value;
  }
}
