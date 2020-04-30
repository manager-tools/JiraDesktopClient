package com.almworks.util.collections;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ReadAccessor<V, T> {
  T getValue(V object);
}
