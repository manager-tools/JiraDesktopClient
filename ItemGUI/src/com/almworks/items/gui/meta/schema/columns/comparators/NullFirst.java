package com.almworks.items.gui.meta.schema.columns.comparators;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

class NullFirst implements SerializableFeature<Comparator> {
  private final boolean myNullFirst;

  NullFirst(boolean nullFirst) {
    myNullFirst = nullFirst;
  }

  @Override
  public Comparator restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
    final Comparator comparator = SerializedObjectAttribute.restore(reader, stream, Comparator.class, invalidate);
    if (comparator == null || !stream.isSuccessfullyAtEnd()) return null;
    return new MyComparator(comparator, myNullFirst);
  }

  @Override
  public Class<Comparator> getValueClass() {
    return Comparator.class;
  }

  private final static class MyComparator implements Comparator {
    private final Comparator myComparator;
    private final boolean myNullFirst;

    public MyComparator(Comparator comparator, boolean nullFirst) {
      myComparator = comparator;
      myNullFirst = nullFirst;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public int compare(Object o1, Object o2) {
      if (o1 == o2) return 0;
      if (o1 != null && o2 != null) return myComparator.compare(o1, o2);
      int result = o1 == null ? 1 : -1;
      if (myNullFirst) result *= -1;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      MyComparator other = Util.castNullable(MyComparator.class, obj);
      return other != null && Util.equals(myComparator, other.myComparator) && myNullFirst == other.myNullFirst;
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myComparator) ^ MyComparator.class.hashCode();
    }
  }
}
