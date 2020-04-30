package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.util.commons.Condition;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

class TargetValues {
  private final LongList myIncluded;
  private final boolean myAllowEmpty;
  private final LongList myExcluded;
  private final boolean myDenyEmpty;

  private TargetValues(LongList included, boolean allowEmpty, LongList excluded, boolean denyEmpty) {
    myIncluded = included;
    myAllowEmpty = allowEmpty;
    myExcluded = excluded;
    myDenyEmpty = denyEmpty;
  }

  public static TargetValues create(LongList inc, LongList exc) {
    LongArray included = LongArray.copy(inc);
    LongArray excluded = LongArray.copy(exc);
    included.removeAll(excluded);
    boolean allowEmpty = removeNoItem(included);
    boolean denyEmpty = removeNoItem(excluded);
    included.sortUnique();
    excluded.sortUnique();
    return new TargetValues(included, allowEmpty, excluded, denyEmpty);
  }

  private static boolean removeNoItem(LongArray list) {
    boolean noItem = false;
    int i = 0;
    while (i < list.size()) {
      long item = list.get(i);
      if (item > 0) i++;
      else {
        list.removeAt(i);
        noItem = true;
      }
    }
    return noItem;
  }

  public boolean matches(ItemKey value) {
    long item = Math.max(0, value == null ? 0 : value.getItem());
    if (isNegative()) return item == 0 ? !myDenyEmpty : !myExcluded.contains(item);
    return item == 0 ? myAllowEmpty : myIncluded.contains(item);
  }

  public boolean matches(List<ItemKey> values) {
    values = Util.NN(values, Collections.<ItemKey>emptyList());
    if (values.isEmpty()) return isNegative() ? myDenyEmpty : myAllowEmpty;
    for (ItemKey value : values) {
      long item = value.getItem();
      if (item < 0) continue;
      if (myIncluded.contains(item)) continue;
      if (isNegative() && !myExcluded.contains(item)) continue;
      return false;
    }
    return true;
  }
  
  public boolean mayAddToValues(List<ItemKey> values) {
    if (myIncluded.isEmpty()) return false;
    values = Util.NN(values, Collections.<ItemKey>emptyList());
    if (values.isEmpty()) return true;
    LongArray items = new LongArray();
    for (ItemKey value : values) {
      long item = value.getItem();
      if (item > 0) items.add(item);
    }
    items.sortUnique();
    for (LongIterator cursor : myIncluded) if (!items.contains(cursor.value())) return true;
    return false;
  }

  public boolean isNegative() {
    return !myAllowEmpty && myIncluded.isEmpty();
  }

  public boolean isAllowsEmpty() {
    return myAllowEmpty || (!myDenyEmpty && myIncluded.isEmpty());
  }

  public boolean isPositive() {
    return !myAllowEmpty && myExcluded.isEmpty();
  }

  public Long getSinglePositive() {
    if (myAllowEmpty) return myIncluded.isEmpty() ? 0l : null;
    return myIncluded.size() == 1 ? myIncluded.get(0) : null;
  }

  public Condition<ItemKey> getVariantsFilter(final boolean excludeOnly) {
    return new Condition<ItemKey>() {
      @Override
      public boolean isAccepted(ItemKey value) {
        if (value == null) return false;
        long item = value.getItem();
        if (item <= 0) return false;
        if (excludeOnly || isNegative()) return !myExcluded.contains(item);
        return myIncluded.contains(item);
      }
    };
  }

  @NotNull
  public LongList getExcluded() {
    return myExcluded;
  }

  @NotNull
  public LongList getIncluded() {
    return myIncluded;
  }
}
