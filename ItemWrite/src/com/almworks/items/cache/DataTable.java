package com.almworks.items.cache;

import org.almworks.util.Collections15;

import java.util.Map;

class DataTable {
  private final Map<DataLoader<?>, LoaderCounterpart<?>> myData = Collections15.hashMap();

  public boolean hasValue(long item, DataLoader<?> data) {
    LoaderCounterpart<?> storage = findStorage(data);
    return storage != null && storage.hasValue(item);
  }

  public <T> T getValue(long item, DataLoader<T> data) {
    LoaderCounterpart<T> storage = findStorage(data);
    if (storage == null) return null;
    return storage.getValue(item);
  }

  @SuppressWarnings( {"unchecked"})
  public DataChange applyUpdate(CacheUpdate update) {
    DataChange event = new DataChange(update.getICN());
    for (Map.Entry<DataLoader<?>, LoaderCounterpart> entry : update.getLoaded().entrySet()) {
      DataLoader<?> loader = entry.getKey();
      LoaderCounterpart storage = findStorage(loader);
      LoaderCounterpart newData = entry.getValue();
      if (storage != null) storage.updateData(loader, newData, event);
      else {
        myData.put(loader, newData);
        event.addChange(loader, newData.getItems());
      }
    }
    return event;
  }

  private <T> LoaderCounterpart<T> findStorage(DataLoader<T> loader) {
    //noinspection unchecked
    return (LoaderCounterpart<T>) myData.get(loader);
  }
}
