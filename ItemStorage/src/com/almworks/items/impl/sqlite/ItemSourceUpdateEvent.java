package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongList;
import org.jetbrains.annotations.NotNull;

public abstract class ItemSourceUpdateEvent {
  @NotNull
  public abstract LongList getAddedItemsSorted();

  @NotNull
  public abstract LongList getRemovedItemsSorted();

  @NotNull
  public abstract LongList getUpdatedItemsSorted();

  public final boolean isEmpty() {
    return getAddedItemsSorted().isEmpty() && getRemovedItemsSorted().isEmpty() && getUpdatedItemsSorted().isEmpty();
  }
}
