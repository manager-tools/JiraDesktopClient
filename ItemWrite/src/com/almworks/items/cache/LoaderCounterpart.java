package com.almworks.items.cache;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import org.almworks.util.Util;

import java.util.List;

class LoaderCounterpart<T> {
  private static final Object NO_DATA = new Object();
  private final LongArray myItems;
  private final List<Object> myData;

  private LoaderCounterpart(LongArray items, List<Object> values) {
    myItems = items;
    myData = values;
  }

  public static LoaderCounterpart load(CacheUpdate update, DataLoader<?> loader, LongList itemsToLoad) {
    LongArray items = LongArray.copy(itemsToLoad);
    items.sortUnique();
    List<?> values = loader.loadValues(update.getReader(), items, update.getLife(), update.getReload());
    //noinspection unchecked
    return new LoaderCounterpart(items, (List<Object>) values);
  }

  public boolean hasValue(long item) {
    int index = myItems.binarySearch(item);
    return index >= 0 && myData.get(index) != NO_DATA;
  }

  public T getValue(long item) {
    int index = myItems.binarySearch(item);
    if (index < 0) return null;
    return getValueAt(index);
  }

  public LongList getItems() {
    return myItems;
  }

  public void updateData(DataLoader<?> loader, LoaderCounterpart<T> data, DataChange change) {
    LongList items = data.getItems();
    LongArray changed = new LongArray();
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      T newValue = data.getValueAt(i);
      int index = myItems.binarySearch(item);
      if (index < 0) {
        index = -index - 1;
        myItems.insert(index, item);
        myData.add(index, newValue);
        changed.add(item);
      } else if (!Util.equals(getValueAt(index), newValue)) {
        myData.set(index, newValue);
        changed.add(item);
      }
    }
    change.addChange(loader, changed);
  }

  private T getValueAt(int index) {
    @SuppressWarnings( {"unchecked"})
    T val = (T) myData.get(index);
    return val != NO_DATA ? val : null;
  }
}
