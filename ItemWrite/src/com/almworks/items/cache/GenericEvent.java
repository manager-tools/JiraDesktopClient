package com.almworks.items.cache;

import com.almworks.integers.LongList;

class GenericEvent implements ImageSliceEvent {
  private final DataChange myChange;
  private final BaseImageSlice mySlice;
  private LongList myChanged = null;

  public GenericEvent(DataChange change, BaseImageSlice slice) {
    myChange = change;
    mySlice = slice;
  }

  @Override
  public long getIcn() {
    return myChange.getIcn();
  }

  @Override
  public LongList getAdded() {
    return myChange.getAdded(mySlice);
  }

  @Override
  public LongList getRemoved() {
    return myChange.getRemoved(mySlice);
  }

  @Override
  public LongList getChanged() {
    if (myChanged == null) myChanged = myChange.getAllChanges(mySlice);
    return myChanged;
  }

  @Override
  public ImageSlice getSlice() {
    return mySlice;
  }

  @Override
  public boolean isChanged(long item, DataLoader<?> loader) {
    return myChange.isChanged(item, loader);
  }
}
