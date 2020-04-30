package com.almworks.util.collections;

import org.almworks.util.Collections15;

import java.util.AbstractList;
import java.util.List;

public class ROArrayListWrapper<T> extends AbstractList<T> {
  private final T[] myArray;
  private final int mySize;

  public ROArrayListWrapper(T[] array, int size) {
    myArray = array;
    mySize = size;
  }

  public T get(int index) {
    return myArray[index];
  }

  public int size() {
    return mySize;
  }

  public static <T> List<T> wrapArray(T[] array) {
    return array != null && array.length > 0 ? new ROArrayListWrapper(array, array.length) : Collections15.<T>emptyList();
  }
}
