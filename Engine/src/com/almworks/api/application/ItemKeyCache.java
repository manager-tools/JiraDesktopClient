package com.almworks.api.application;

import com.almworks.integers.LongObjMap;
import com.almworks.items.api.DBReader;
import com.almworks.util.Pair;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import java.util.concurrent.ConcurrentHashMap;

public class ItemKeyCache {
  /** item => (item key, ICN) */
  private final LongObjMap<Pair<? extends ResolvedItem, Long>> myMap = LongObjMap.create();
  private final ConcurrentHashMap<Long, SynchronizedBoolean> myBeingCreated = new ConcurrentHashMap<Long, SynchronizedBoolean>();
  private final LongObjMap<String> myBadItems = LongObjMap.create();

  private final Object myLock = new Object();

  public ItemKeyCache() {
  }

  /**
   * @param reader is used to read from local version of item, and it's ok since items (enum elements) are be not user-editable. To check this requirement, see implementations of {@link ResolvedFactory#createResolvedItem}
   * */
  @Nullable
  public <T extends ResolvedItem> T getItemKeyOrNull(@Nullable Long item, DBReader reader, ResolvedFactory<T> factory) {
    if (item == null || item <= 0L) {
      return null;
    }
    try {
      return getItemKey(item, reader, factory);
    } catch (BadItemException ignored) {
      return null;
    }
  }

  /**
   * @param reader is used to read from local version of item, and it's ok since items (enum elements) are not user-editable. To check this requirement, see implementations of {@link ResolvedFactory#createResolvedItem}
   * */
  @NotNull
  public <T extends ResolvedItem> T getItemKey(long item, DBReader reader, ResolvedFactory<T> factory) throws BadItemException {
    SynchronizedBoolean createFlag;
    long itemIcn = reader.getItemIcn(item);
    while (true) {
      synchronized (myLock) {
        ResolvedItem key = priFindExisting(item, itemIcn);
        if (key != null) return (T) key;
        // if it is not being created, prepare to create it myself
        createFlag = new SynchronizedBoolean(false);
      }
      SynchronizedBoolean create = myBeingCreated.putIfAbsent(item, createFlag);
      if (create == null) break;
      else {
        try {
          create.waitForValue(true);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
        myBeingCreated.remove(item, create);
      }
    }
    T key = null;
    String badMessage;
    try {
// create the key - not under a lock!
      badMessage = null;
      try {
        key = factory.createResolvedItem(item, reader, this);
        if (key == null) {
          assert false : item + " " + factory;
          badMessage = "key is null";
        }
      } catch (BadItemException e) {
        Log.warn("bad item " + item, e);
        badMessage = e.getMessage();
      }
      synchronized (myLock) {
        if (badMessage != null) {
          myBadItems.put(item, badMessage);
        } else {
          // put it into the map, see that noone has done the same
          Pair<? extends ResolvedItem, Long> removedKey = myMap.put(item, Pair.create(key, itemIcn));
          assert removedKey == null || removedKey.getSecond() < itemIcn : item + "@" + itemIcn + ' ' + key + ' ' + removedKey;
        }
        myBeingCreated.remove(item, createFlag);
      }
      // let others who wait know that the key is created
    } finally {
      createFlag.set(true);
    }
    if (badMessage != null) {
      throw new BadItemException(badMessage, item);
    }
    assert key != null;
    return key;
  }

  private ResolvedItem priFindExisting(long item, Long icn) throws BadItemException {
    assert Thread.holdsLock(myLock);
    // see if the key is immediately available
    Pair<? extends ResolvedItem, Long> key_icn = myMap.get(item);
    if (key_icn != null && (icn == null || key_icn.getSecond() >= icn)) {
      return key_icn.getFirst();
    }

    String badMessage = myBadItems.get(item);
    if (badMessage != null) {
      throw new BadItemException(badMessage, item);
    }
    return null;
  }

  public void remove(long item) {
    // we should not interfere with key creation
    SynchronizedBoolean createFlag;
    synchronized (myLock) {
      createFlag = myBeingCreated.get(item);
      if (createFlag == null) {
        // no interference: just delete
        myMap.remove(item);
        return;
      }
    }
    // we have to wait: someone is creating the key
    try {
      createFlag.waitForValue(true);
      myBeingCreated.remove(item, createFlag);
      synchronized (myLock) {
        myMap.remove(item);
      }
    } catch(InterruptedException ex) {
      throw new RuntimeInterruptedException(ex);
    }
  }
}
