package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;

public class ChangedItemsCollector {
  private final LongSetBuilder myChangedItems = new LongSetBuilder();
  private boolean myHasChangedItems;

  public void itemChanged(long itemId) {
    if (itemId <= 0)
      return;
    myChangedItems.add(itemId);
    myHasChangedItems = true;
  }

  public LongList drainChangedItemsSorted() {
    LongList items = myChangedItems.toArray();
    myChangedItems.clear(true);
    return items;
  }

  public void cleanUp() {
    myChangedItems.clear(true);
    myHasChangedItems = false;
  }

  public boolean hasChanges() {
    return myHasChangedItems;
  }
}
