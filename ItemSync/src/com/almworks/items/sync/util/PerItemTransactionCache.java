package com.almworks.items.sync.util;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.Nullable;

public class PerItemTransactionCache<T> {
  private final TransactionCacheKey<TLongObjectHashMap<T>> myKey;

  public PerItemTransactionCache(String debugName) {
    myKey = TransactionCacheKey.create(debugName);
  }

  public static <T> PerItemTransactionCache<T> create(String debugName) {
    return new PerItemTransactionCache<T>(debugName);
  }

  public void put(ItemVersion item, T value) {
    put(item.getReader(), item.getItem(), value);
  }

  public void put(DBReader reader, long item, T value) {
    TLongObjectHashMap<T> map = myKey.get(reader);
    if (map == null) {
      map = new TLongObjectHashMap<>();
      myKey.put(reader, map);
    }
    if (value != null) map.put(item, value);
    else map.remove(item);
  }

  @Nullable
  public T get(ItemVersion item) {
    return get(item.getReader(), item.getItem());
  }

  @Nullable
  public T get(DBReader reader, long item) {
    TLongObjectHashMap<T> map = myKey.get(reader);
    return map != null ? map.get(item) : null;
  }
}
