package com.almworks.items.impl.sqlite.cache;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.impl.dbadapter.CompoundValueCache;
import com.almworks.items.impl.dbadapter.ValueCache;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

class CompoundCacheImpl implements CompoundValueCache {
  private final ValueCacheManager myManager;
  private final Procedure<LongList> myCallback;
  private final Map<Object, ValueCacheImpl> myCaches = Collections15.hashMap();
  private boolean myDisposed = false;

  public CompoundCacheImpl(ValueCacheManager manager, Procedure<LongList> callback) {
    myManager = manager;
    myCallback = callback;
  }

  public ValueCache getCache(Object key) {
    synchronized (myManager.getLock()) {
      if (myDisposed) {
        assert false;
        return null;
      }
      ValueCacheImpl cache = myCaches.get(key);
      if (cache == null) {
        cache = myManager.createNewCache(myCallback);
        myCaches.put(key, cache);
      }
      return cache;
    }
  }

  public CacheUpdate createUpdate() {
    return new CacheUpdateImpl();
  }

  public void dispose() {
    synchronized (myManager.getLock()) {
      if (myDisposed)
        return;
      for (ValueCacheImpl cache : myCaches.values())
        myManager.removeCache(cache);
      myCaches.clear();
      myDisposed = true;
    }
  }

  private void applySetItems(@NotNull Map<Object, LongIterable> setItems) {
    synchronized (myManager.getLock()) {
      Map<ValueCacheImpl, Pair<LongArray, LongList>> addRemove = Collections15.hashMap();
      for (Map.Entry<Object, LongIterable> entry : setItems.entrySet()) {
        ValueCacheImpl cache = myCaches.get(entry.getKey());
        if (cache == null)
          continue;
        addRemove.put(cache, cache.selectAddRemove(entry.getValue()));
      }
      ValueCacheImpl.ValueSearch search = new ValueCacheImpl.ValueSearch(myManager);
      long[] tmp = new long[2];
      for (Map.Entry<ValueCacheImpl, Pair<LongArray, LongList>> entry : addRemove.entrySet()) {
        ValueCacheImpl cache = entry.getKey();
        LongList toRemove = entry.getValue().getSecond();
        for (LongIterator it = toRemove.iterator(); it.hasNext();) {
          long item = it.nextValue();
          for (Map.Entry<ValueCacheImpl, Pair<LongArray, LongList>> otherEntry : addRemove.entrySet()) {
            ValueCacheImpl otherCache = otherEntry.getKey();
            if (cache == otherCache)
              continue;
            LongArray toAdd = otherEntry.getValue().getFirst();
            assert toAdd.isSortedUnique();
            int index = toAdd.binarySearch(item);
            if (index < 0)
              continue;
            otherCache.addItem(item, search, tmp);
            toAdd.removeAt(index);
          }
        }
        cache.removeItems(toRemove);
      }
      for (Map.Entry<ValueCacheImpl, Pair<LongArray, LongList>> entry : addRemove.entrySet()) {
        ValueCacheImpl cache = entry.getKey();
        LongArray add = entry.getValue().getFirst();
        if (!add.isEmpty())
          cache.addItems(add);
      }
    }
  }

  private class CacheUpdateImpl implements CacheUpdate {
    private Map<Object, LongIterable> mySetItems;

    public CacheUpdate setItems(Object key, LongIterable items) {
      ensureReady().put(key, items);
      return this;
    }

    public CacheUpdate setItems(Object key, long[] items) {
      LongList itemsList;
      if (items.length == 0)
        itemsList = LongList.EMPTY;
      else
        itemsList = LongArray.create(items);
      ensureReady().put(key, itemsList);
      return this;
    }

    public void apply() {
      if (mySetItems == null)
        return;
      Map<Object, LongIterable> setItems = mySetItems;
      mySetItems = null;
      applySetItems(setItems);
    }

    private Map<Object, LongIterable> ensureReady() {
      if (mySetItems == null)
        mySetItems = Collections15.hashMap();
      return mySetItems;
    }
  }
}
