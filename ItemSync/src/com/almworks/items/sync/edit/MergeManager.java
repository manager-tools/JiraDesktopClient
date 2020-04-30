package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongListIterator;
import com.almworks.items.api.*;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.ItemAutoMerge;
import com.almworks.items.sync.ModifiableDiff;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

class MergeManager {
  private final SyncManagerImpl myManager;
  // Guarded by myToMerge
  private final LongSet myToMerge = new LongSet();
  private final AtomicReference<MergeTransaction> myMergeTransaction = new AtomicReference<MergeTransaction>();
  private static final ItemAutoMerge DUMMY_AUTO_MERGE = new ItemAutoMerge() {
    @Override
    public void preProcess(ModifiableDiff local) {
      Collection<? extends DBAttribute<?>> attributes = local.getChanged();
      if (attributes.isEmpty()) return;
      if (attributes.size() > 1 || !attributes.contains(SyncSchema.INVISIBLE))
        Log.error("Cannot auto merge changes: " + attributes + " " + local.getItem());
    }

    @Override
    public void resolve(AutoMergeData data) {
      if (!data.getUnresolved().isEmpty())
        Log.error("Cannot resolve conflict " + data.getUnresolved());
    }
  };

  MergeManager(SyncManagerImpl manager) {
    myManager = manager;
  }

  public void requestAutoMerge(LongList items) {
    if (items == null || items.isEmpty()) return;
    synchronized (myToMerge) {
      if (!myToMerge.addAllCheckChange(items))
        return;
    }
    requestMerge();
  }

  private void requestMerge() {
    if (myMergeTransaction.get() != null)
      return;
    MergeTransaction transaction = new MergeTransaction();
    if (myMergeTransaction.compareAndSet(null, transaction))
      myManager.enquireWrite(transaction).finallyDoWithResult(ThreadGate.STRAIGHT, transaction);
  }

  private boolean doMerge(DBWriter writer, long item, @Nullable EditCounterpart ignoreLock) {
    ItemAutoMerge operations = myManager.getOperations(writer, item);
    if (operations == null) operations = DUMMY_AUTO_MERGE;
    MergeEventImpl event = AutoMerge.autoMerge(writer, item, operations, myManager, ignoreLock);
    if (event == null || event.isEmpty()) return false;
    MergedNotifier.appendSuccessfulMerge(myManager, writer, event);
    return true;
  }

  public boolean autoMergeNow(DBWriter writer, LongList items, EditCounterpart ignoreLock) {
    return lockOrMergeLater(writer, items, ignoreLock) && mergeAll(writer, items, ignoreLock);
  }

  private boolean mergeAll(DBWriter writer, LongList items, EditCounterpart ignoreLock) {
    boolean result = true;
    for (int i = 0; i < items.size(); i++)
      if (!doMerge(writer, items.get(i), ignoreLock))
        result = false;
    return result;
  }

  public void autoMergePartial(DBWriter writer, LongList items, EditCounterpart ignoreLock) {
    items = myManager.selectMergableNowDeferOther(writer, items, ignoreLock);
    mergeAll(writer, items, ignoreLock);
  }

  /**
   * @see com.almworks.items.sync.edit.SyncManagerImpl#lockOrMergeLater(com.almworks.items.api.DBWriter, com.almworks.integers.LongList, com.almworks.items.sync.EditorLock, com.almworks.integers.LongList) 
   */
  public boolean lockOrMergeLater(DBWriter writer, LongList items, @Nullable EditCounterpart ignoreLock) {
    return myManager.lockOrMergeLater(writer, items, ignoreLock, items);
  }

  private class MergeTransaction implements WriteTransaction<Object>, Procedure<DBResult<?>> {
    private final LongArray myRequested = new LongArray();

    @Override
    public Object transaction(DBWriter writer) throws DBOperationCancelledException {
      myMergeTransaction.compareAndSet(this, null);
      synchronized (myToMerge) {
        myRequested.addAll(myToMerge);
        myToMerge.clear();
      }
      boolean success = false;
      try {
        doMerge(writer);
        success = true;
      } finally {
        if (!success)
          synchronized (myToMerge) {
            myToMerge.addAll(myRequested);
            myRequested.clear();
          }
      }
      return null;
    }

    private void doMerge(DBWriter writer) {
      WritableLongListIterator it = myRequested.iterator();
      while (it.hasNext()) {
        long item = it.nextValue();
        LongArray asList = LongArray.create(item);
        if (lockOrMergeLater(writer, asList, null))
          MergeManager.this.doMerge(writer, item, null);
      }
    }

    @Override
    public void invoke(DBResult<?> result) {
      boolean success = result.isSuccessful();
      synchronized (myToMerge) {
        if (!success)
          myToMerge.addAll(myRequested);
      }
      if (!success)
        requestMerge();
    }
  }
}
