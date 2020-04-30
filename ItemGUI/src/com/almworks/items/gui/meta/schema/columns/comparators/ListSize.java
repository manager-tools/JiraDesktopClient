package com.almworks.items.gui.meta.schema.columns.comparators;

import org.almworks.util.Util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class ListSize extends SortedListsComparator.Feature {
  @Override
  protected Comparator getComparator(final Comparator comparator) {
    return new MyComparator(comparator);
  }

  private final static class MyComparator extends SortedListsComparator {

    public MyComparator(Comparator comparator) {
      super(comparator);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected int compareSortedLists(List list1, List list2) {
      int res = Util.compareInts(list1.size(), list2.size());
      if (res != 0)
        return -res;
      Iterator it1 = list1.iterator();
      Iterator it2 = list2.iterator();
      while (it1.hasNext()) {
        res = getComparator().compare(it1.next(), it2.next());
        if (res != 0)
          return res;
      }
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      MyComparator other = Util.castNullable(MyComparator.class, obj);
      return other != null && Util.equals(getComparator(), other.getComparator());
    }

    @Override
    public int hashCode() {
      return getComparator().hashCode();
    }
  }
}
