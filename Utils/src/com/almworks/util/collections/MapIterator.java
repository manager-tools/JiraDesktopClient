package com.almworks.util.collections;

import java.util.NoSuchElementException;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface MapIterator <K, V> {
  boolean next();

  K lastKey() throws NoSuchElementException;

  V lastValue() throws NoSuchElementException;
}
