package com.almworks.util.collections;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface WriteAccessor <V, T> {
  void setValue(V object, T value);
}
