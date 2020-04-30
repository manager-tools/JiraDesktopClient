package com.almworks.items.cache;

import com.almworks.integers.LongList;

// todo doc : what's the source of the returned lists, are they created each get* is called, are they mutable
public interface ImageSliceEvent {
  long getIcn();

  LongList getAdded();

  LongList getRemoved();

  LongList getChanged();

  ImageSlice getSlice();

  boolean isChanged(long item, DataLoader<?> loader);
}
