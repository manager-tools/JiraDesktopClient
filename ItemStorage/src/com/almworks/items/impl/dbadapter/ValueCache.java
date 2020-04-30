package com.almworks.items.impl.dbadapter;

import com.almworks.integers.LongIterable;
import com.almworks.integers.LongList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ValueCache {
  int NO_VALUE = 0;
  int OUT_OF_DATE = 1;
  int UP_TO_DATE = 2;

  void addItems(LongIterable items);

  void removeItems(LongIterable items);

  void setItems(LongIterable items);

  void addAttributes(List<? extends Attribute> attributes);

  void removeAttribute(SyncValueLoader attribute);

  @Nullable
  Object getObjectValue(int item, SyncValueLoader attr, @Nullable int[] uptodate);

  @Nullable
  ItemAccessor getItemAccessor(long item);

  ItemSetAccessor getItemSetAccessor(LongList items);


  class NoItemData implements ItemAccessor {
    private final int myItem;

    public NoItemData(int item) {
      myItem = item;
    }

    @Nullable
    public Object getValue(SyncValueLoader attribute) {
      return null;
    }

    public int getInt(SyncValueLoader accessor, int missingValue) {
      return missingValue;
    }

    public long getItem() {
      return myItem;
    }

    public long getLong(SyncValueLoader attribute, long missingValue) {
      return missingValue;
    }

    public boolean hasValues() {
      return false;
    }

    public boolean hasUptodateValue(SyncValueLoader attribute) {
      return false;
    }
  }
}
