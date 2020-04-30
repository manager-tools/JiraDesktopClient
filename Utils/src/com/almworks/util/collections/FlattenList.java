package com.almworks.util.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
public class FlattenList <T> extends AbstractList<T> {
  private final List<? extends List<T>> mySources;

  protected FlattenList(List<? extends List<T>> sources) {
    if (sources == null) throw new NullPointerException();
    mySources = sources;
  }

  public static <T> List<T> create(List<? extends List<T>> sources) {
    return new FlattenList<T>(sources);
  }

  public static <T> List<T> create(List<? extends T> list1, List<? extends T> list2) {
    return new FlattenList<T>(Arrays.<List<T>>asList(new List[]{list1, list2}));
  }

  public T get(final int index) {
    int i = index;
    int totalSize = 0;
    Iterator<? extends List<T>> iterator = notNullLists();
    while (iterator.hasNext()) {
      List<T> list = iterator.next();
      int size = list.size();
      if (size > i) return list.get(i);
      totalSize += size;
      i -= size;
    }
    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + totalSize);
  }

  public int size() {
    Iterator<? extends List<T>> iterator = notNullLists();
    int totalSize = 0;
    while (iterator.hasNext()) {
      List<T> list = iterator.next();
      totalSize += list.size();
    }
    return totalSize;
  }

  private Iterator<? extends List<T>> notNullLists() {
    return Containers.notNullIterator(mySources.iterator());
  }
}
