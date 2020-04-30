package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongList;
import org.jetbrains.annotations.NotNull;

public class ItemSourceUpdateEventImpl extends ItemSourceUpdateEvent {
  private final LongList myAdded;
  private final LongList myRemoved;
  private final LongList myChanged;

  public ItemSourceUpdateEventImpl(LongList added, LongList removed, LongList changed) {
    myAdded = added == null ? LongList.EMPTY : added;
    myRemoved = removed == null ? LongList.EMPTY : removed;
    myChanged = changed == null ? LongList.EMPTY : changed;
  }

  @NotNull
  public LongList getAddedItemsSorted() {
    return myAdded;
  }

  @NotNull
  public LongList getRemovedItemsSorted() {
    return myRemoved;
  }

  @NotNull
  public LongList getUpdatedItemsSorted() {
    return myChanged;
  }

  public String toString() {
    return "added: " + myAdded + "; removed: " + myRemoved + "; changed: " + myChanged;
  }
}
