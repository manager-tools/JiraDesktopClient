package com.almworks.items.gui.meta.schema.columns.comparators;

import org.almworks.util.Util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class ListLexical extends SortedListsComparator.Feature {
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
      Iterator i1 = list1.iterator();
      Iterator i2 = list2.iterator();
      while (i1.hasNext() && i2.hasNext()) {
        int cmp = getComparator().compare(i1.next(), i2.next());
        if (cmp != 0) return cmp;
      }
      if (i1.hasNext() || i2.hasNext())
        return i1.hasNext() ? -1 : 1;
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
