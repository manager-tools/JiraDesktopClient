package com.almworks.util.collections;

import org.almworks.util.ArrayUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public abstract class CollectionSortPolicy {
  public abstract <T> void sort(List<T> sublist, Comparator<? super T> comparator);

  /**
   * Default policy, doing a merge sort.
   */
  public static final CollectionSortPolicy DEFAULT = new CollectionSortPolicy() {
    public <T> void sort(List<T> sublist, Comparator<? super T> comparator) {
      Collections.sort(sublist, comparator);
    }
  };

  /**
   * Policy doing a quick sort. Does not retain the order!
   */
  public static final CollectionSortPolicy QUICK = new CollectionSortPolicy() {
    public <T> void sort(List<T> list, Comparator<? super T> comparator) {
      // copied from Collections.sort, replacing the sort method
      // todo: do not allocated additional array
      Object[] array = list.toArray();
      ArrayUtil.quicksort(array, (Comparator) comparator);
      ListIterator ii = list.listIterator();
      for (Object update : array) {
        ii.next();
        ii.set(update);
      }
    }
  };
}
