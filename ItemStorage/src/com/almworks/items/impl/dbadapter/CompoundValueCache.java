package com.almworks.items.impl.dbadapter;

import com.almworks.integers.LongIterable;

public interface CompoundValueCache {
  ValueCache getCache(Object key);

  CacheUpdate createUpdate();

  void dispose();

  interface CacheUpdate {
    CacheUpdate setItems(Object key, LongIterable items);

    CacheUpdate setItems(Object key, long[] items);

    void apply();
  }
}
