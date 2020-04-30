package com.almworks.util.collections;

import java.util.Comparator;
import java.util.List;

public class LexicographicalListsComparator<T> implements Comparator<List<T>> {
  private final boolean myNullsFirst;
  private final boolean myLargerListsFirst;
  private final Comparator<? super T> myComparator;

  public LexicographicalListsComparator(boolean nullsFirst, boolean largerListsFirst, Comparator<? super T> elementComparator) {
    myNullsFirst = nullsFirst;
    myLargerListsFirst = largerListsFirst;
    myComparator = elementComparator;
  }

  public LexicographicalListsComparator<T> copyWithNewNullsFirst(boolean newValue) {
    if (newValue == myNullsFirst) return this;
    else return new LexicographicalListsComparator<T>(newValue, myLargerListsFirst, myComparator);
  }

  public LexicographicalListsComparator<T> copyWithNewLargerListsFirst(boolean newValue) {
    if (newValue == myLargerListsFirst) return this;
    else return new LexicographicalListsComparator<T>(myNullsFirst, newValue, myComparator);
  }

  public int compare(List<T> list1, List<T> list2) {
    if (list1 != null && list1.isEmpty()) list1 = null;
    if (list2 != null && list2.isEmpty()) list2 = null;
    if (list1 == list2) return 0;
    if (list1 == null || list2 == null) {
      int result = list1 == null ? -1 : 1;
      return myNullsFirst ? result : -result;
    }
    int size1 = list1.size();
    int size2 = list2.size();
    if (size1 != size2)
      return Containers.compareInts(size1, size2) * (myLargerListsFirst ? -1 : 1);
    for (int i = 0; i < size1; i++) {
      T t1 = list1.get(i);
      T t2 = list2.get(i);
      int order = myComparator.compare(t1, t2);
      if (order != 0)
        return order;
    }
    return 0;
  }
}
