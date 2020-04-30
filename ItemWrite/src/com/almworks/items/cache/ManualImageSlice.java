package com.almworks.items.cache;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.threads.Threads;
import org.almworks.util.TypedKey;

/**
 * Allows manual control over items loaded from DB.<br>
 * When data for newly added items is loaded send an event.<br>
 * Supports item sets: actual (all value loaded) and requested (items added but data is not loaded yet).
 */
public class ManualImageSlice extends BaseImageSlice {
  private static final TypedKey<long[]> ADDED = TypedKey.create("added");
  private static final TypedKey<long[]> REMOVED = TypedKey.create("removed");
  private final LongSet myActual = new LongSet();
  private final LongSet myAdd = new LongSet();
  private final LongSet myRemove = new LongSet();

  ManualImageSlice(DBImage image) {
    super(image);
  }

  @Override
  protected LongList earth() {
    Threads.assertAWTThread();
    LogHelper.assertError(!isRunning(), "burying alive", this);
    LongArray current;
    synchronized (myActual) {
      myAdd.clear();
      myRemove.clear();
      current = LongArray.copy(myActual);
      myActual.clear();
    }
    return current;
  }

  public void addItems(long ... items) {
    addItems(LongArray.create(items));
  }

  public void addItems(LongList items) {
    if (items == null || items.isEmpty()) return;
    boolean changed = false;
    synchronized (myActual) {
      if (!isRunning()) return;
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (myRemove.remove(item)) continue;
        if (myAdd.contains(item) || myActual.contains(item)) continue;
        myAdd.add(item);
        changed = true;
      }
    }
    if (changed) requestUpdate();
  }

  public void removeItems(long ... items) {
    if (items == null || items.length == 0) return;
    removeItems(LongArray.create(items));
  }

  public void removeItems(LongList items) {
    if (items == null || items.isEmpty()) return;
    boolean changed = false;
    synchronized (myActual) {
      if (!isRunning()) return;
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (myAdd.remove(item)) continue;
        if (myRemove.contains(item) || !myActual.contains(item)) continue;
        myRemove.add(item);
        changed = true;
      }
    }
    if (changed) requestUpdate();
  }


  public void getRequested(LongArray target) {
    target.clear();
    if (!isRunning()) return;
    synchronized (myActual) {
      target.addAll(myActual);
      if (myAdd.isEmpty() && myRemove.isEmpty()) return;
      target.addAll(myAdd);
      target.removeAll(myRemove);
    }
    target.sortUnique();
  }

  @Override
  public LongList getActualItems() {
    if (isBuried()) return LongList.EMPTY;
    synchronized (myActual) {
      return new LongArray(myActual);
    }
  }

  @Override
  public boolean hasAllValues(long item) {
    synchronized (myActual) {
      return myActual.contains(item);
    }
  }

  @Override
  public int getActualCount() {
    if (isBuried()) return 0;
    synchronized (myActual) {
      return myActual.size();
    }
  }

  @Override
  public long getItem(int index) {
    synchronized (myActual) {
      return myActual.get(index);
    }
  }

  @Override
  void updateItemSet(CacheUpdate update) {
    long[] add;
    long[] remove;
    LongSet newSet;
    synchronized (myActual) {
      add = myAdd.toNativeArray();
      remove = myRemove.toNativeArray();
      newSet = LongSet.copy(myActual);
      newSet.addAll(myAdd);
      newSet.removeAll(myRemove);
    }
    update.putData(ADDED, add);
    update.putData(REMOVED, remove);
    setItemSet(update, newSet);
  }

  @Override
  void applyItemSetUpdate(CacheUpdate update, DataChange change) {
    LongArray added = LongArray.create(update.getData(ADDED));
    LongArray removed = LongArray.create(update.getData(REMOVED));
    boolean updateRequired;
    synchronized (myActual) {
      added.retain(myAdd);
      myAdd.removeAll(added);
      removed.retain(myRemove);
      myRemove.removeAll(removed);
      myActual.addAll(added);
      myActual.removeAll(removed);
      updateRequired = !myAdd.isEmpty() || !myRemove.isEmpty();
    }
    if (updateRequired) requestUpdate();
    change.setItemChange(this, added, removed);
  }

  @Override
  public String toString() {
    return "MIS " + myActual + " +" + myAdd + " -" + myRemove;
  }
}
