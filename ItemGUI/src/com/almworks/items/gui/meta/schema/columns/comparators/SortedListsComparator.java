package com.almworks.items.gui.meta.schema.columns.comparators;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

abstract class SortedListsComparator implements Comparator {
  private final Comparator myComparator;

  public SortedListsComparator(Comparator comparator) {
    myComparator = comparator;
  }

  protected final Comparator getComparator() {
    return myComparator;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public int compare(Object o1, Object o2) {
    if (o1 == o2) return 0;
    if (o1 == null || o2 == null) return o1 == null ? -1 : 1;
    List list1 = Util.castNullable(List.class, o1);
    List list2 = Util.castNullable(List.class, o2);

    if (list1 == null || list2 == null) {
      Log.error("Expected 2 Lists, got " + o1.getClass().getName() + " and " + o2.getClass().getName());
      if (list1 == list2)
        return 0;
      return list1 == null ? -1 : 1;
    }
    if (!CollectionUtil.isSorted(list1, myComparator) || !CollectionUtil.isSorted(list2, myComparator)) {
      Log.error("Expected sorted lists");
      return 0;
    }
    return compareSortedLists(list1, list2);
  }

  protected abstract int compareSortedLists(List list1, List list2);

  static abstract class Feature implements SerializableFeature<Comparator> {
    @Override
    public Comparator restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      final Comparator comparator = SerializedObjectAttribute.restore(reader, stream, Comparator.class, invalidate);
      if (comparator == null || !stream.isSuccessfullyAtEnd()) return null;
      return getComparator(comparator);
    }

    protected abstract Comparator getComparator(Comparator comparator);

    @Override
    public Class<Comparator> getValueClass() {
      return Comparator.class;
    }
  }
}
