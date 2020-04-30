package com.almworks.util.properties;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface StorableMap {
  boolean isFixed();

  StorableMap fix();

  void clear();

  void store(DataOutput out) throws IOException;

  void restore(DataInput in, StorableKey sampleKey) throws IOException, StorableException;

  int hashCode();

  boolean equals(Object another);

  StorableMap newMap();

  public static final class Factory {
    public static StorableMap create() {
      return new StorableMapImpl();
    }
  }
}
