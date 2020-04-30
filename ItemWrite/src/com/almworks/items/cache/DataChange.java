package com.almworks.items.cache;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.cache.util.DefaultMap;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

class DataChange {
  private final DefaultMap<DataLoader<?>, LongSet> myChanges = DefaultMap.longSet();
  private final Map<BaseImageSlice, Pair<LongList, LongList>> myItemChange = Collections15.hashMap();
  private final long myICN;

  DataChange(long ICN) {
    myICN = ICN;
  }

  public void setItemChange(BaseImageSlice slice, LongList added, LongList removed) {
    if (added.isEmpty() && removed.isEmpty()) return;
    myItemChange.put(slice, Pair.create(added, removed));
  }

  public void addChange(DataLoader<?> loader, LongList items) {
    myChanges.getOrCreate(loader).addAll(items);
  }

  @NotNull
  public LongList getAdded(BaseImageSlice slice) {
    Pair<LongList, LongList> change = myItemChange.get(slice);
    return change != null ? change.getFirst() : LongList.EMPTY;
  }

  @NotNull
  public LongList getRemoved(BaseImageSlice slice) {
    Pair<LongList, LongList> changed = myItemChange.get(slice);
    return changed != null ? changed.getSecond() : LongList.EMPTY;
  }

  public long getIcn() {
    return myICN;
  }

  public LongList getAllChanges(BaseImageSlice slice) {
    DataLoader<?>[] loaders = slice.getActualData();
    LongArray items = new LongArray();
    for (DataLoader<?> loader : loaders) {
      LongSet change = myChanges.get(loader);
      if (change != null) items.addAll(change);
    }
    LongSet set = LongSet.copy(items);
    set.retainAll(slice.getActualItems());
    return set;
  }

  public boolean isChanged(long item, DataLoader<?> loader) {
    LongSet items = myChanges.get(loader);
    return items != null && items.contains(item);
  }
}
