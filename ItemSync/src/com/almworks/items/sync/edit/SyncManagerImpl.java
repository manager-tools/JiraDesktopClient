package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.collections.LongSet;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SyncManagerImpl implements SyncManager {
  // Guarded by myToStart
  private final Deque<EditStart> myToStart = Collections15.arrayDeque();
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final EditLocks myEditLocks = new EditLocks(myModifiable);
  private final MergeManager myMergeManager = new MergeManager(this);
  private final AtomicReference<ReadTransaction<Object>> myStartTransaction = new AtomicReference<ReadTransaction<Object>>();
  private final SyncStartup myDatabase;
  private final ItemAutoMerge.Selector myOperationSelector;
  private final UploadLocks myUploadLocks = new UploadLocks(myModifiable);
  private final FireEventSupport<Listener> myListeners = FireEventSupport.create(Listener.class);

  public SyncManagerImpl(Database database, ItemAutoMerge.Selector operationSelector) {
    myDatabase = new SyncStartup(database, this);
    myOperationSelector = operationSelector;
  }

  public void runStartup() {
    myDatabase.startupNow();
  }

  @Nullable
  public EditCounterpart findLock(long item) {
    return myEditLocks.findLock(item);
  }

  @Override
  public EditorLock findAnyLock(LongList items) {
    return myEditLocks.findAnyLock(items, null);
  }

  @Override
  public EditControl prepareEdit(long item) {
    return prepareEdit(LongArray.create(item));
  }

  @Override
  public EditControl prepareEdit(LongList items) {
    if (items == null || items.isEmpty()) return new EditCounterpart(this, LongList.EMPTY);
    return myEditLocks.findAnyLock(items, null) == null ? new EditCounterpart(this, items) : null;
  }

  @Override
  public void syncUpload(ItemUploader uploader) throws InterruptedException {
    new SyncUploadTask2(this, uploader).perform();
  }

  @Override
  public boolean canUpload(long item) {
    return !getUploadLocks().isAnyLocked(LongArray.create(item));
  }

  @Override
  public boolean canUploadAll(LongList items) {
    return !getUploadLocks().isAnyLocked(items);
  }

  @Override
  public boolean isDuringUpload(DBReader reader, long item) {
    return reader.getValue(item, SyncSchema.UPLOAD_TASK) != null;
  }

  @Override
  public DBResult<Object> writeDownloaded(DownloadProcedure<? super DBDrain> procedure) {
    return new DownloadDrainImpl(this, procedure).start();
  }

  @Override
  public void commitEdit(EditCommit commit) {
    if (commit == null) return;
    new CommitEditDrain(this, CommitCounterpart.CREATE_ONLY, commit, null, true).start();
  }

  @Override
  public void unsafeCommitEdit(EditCommit commit) {
    if (commit == null) return;
    new UnsafeCommitDrain(this, commit).start();
  }

  @Override
  public boolean commitEdit(LongList items, EditCommit commit) {
    if (items == null || items.isEmpty()) {
      commitEdit(commit);
      return true;
    }
    return SimplifiedCommit.start(this, items, commit);
  }

  @Override
  public Modifiable getModifiable() {
    return myModifiable;
  }

  @Override
  public void addListener(Lifespan life, ThreadGate gate, Listener listener) {
    myListeners.addListener(life, gate, listener);
  }

  public boolean autoMergeNow(DBWriter writer, LongList items, @Nullable  EditCounterpart ignoreLock) {
    return myMergeManager.autoMergeNow(writer, items, ignoreLock);
  }

  public void autoMergeNowPartial(DBWriter writer, LongList items, @Nullable  EditCounterpart ignoreLock) {
    myMergeManager.autoMergePartial(writer, items, ignoreLock);
  }

  /**
   * Locks the item for merge if merge lock allowed right now or enqueue to merge later the given item.
   * @param transaction current transaction
   * @param itemsToLock the items to lock for merge
   * @param ignoreLock lock the item even if it is locked for edit by the ignoreLock editor
   * @param itemsToEnqueue if any of itemsToLock cannot be locked right now enqueue itemToLock to merge later
   * @return true iff the itemToLock is locked for merge, false means that the item cannot be locked and itemToEnqueue is to be merged later
   */
  boolean lockOrMergeLater(DBWriter transaction, LongList itemsToLock, @Nullable EditorLock ignoreLock, LongList itemsToEnqueue) {
    if (!UploadLocks.allowsMerge(transaction, itemsToLock, itemsToEnqueue)) return false;
    return tryLockForMergeOrDefer(transaction, itemsToLock, ignoreLock, itemsToEnqueue);
  }

  private boolean tryLockForMergeOrDefer(DBWriter transaction, LongList itemsToLock, @Nullable EditorLock ignoreLock,
    LongList itemsToEnqueue) {
    int countDown = 10;
    while (countDown >= 0) {
      EditCounterpart currentLock = MergeLocker.getInstance(transaction, this).lockAll(itemsToLock, ignoreLock);
      if (currentLock == null) return true;
      if (currentLock.mergeWhenReleased(itemsToEnqueue)) return false;
      countDown--;
    }
    Log.error("Cannot lock or merge later " + itemsToLock + " " + itemsToEnqueue);
    throw new RuntimeException();
  }

  LongList selectMergableNowDeferOther(DBWriter transaction, LongList items, @Nullable EditorLock ignoreLock) {
    items = UploadLocks.selectNotUploadingDeferOther(transaction, items);
    if (items.isEmpty()) return LongList.EMPTY;
    LongSet result = LongSet.copy(items);
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      LongList asList = new LongList.Single(item);
      boolean done = tryLockForMergeOrDefer(transaction, asList, ignoreLock, asList);
      if (!done) result.remove(item);
    }
    return result;
  }

  public boolean start(EditStart start) {
    boolean success = false;
    EditCounterpart counterpart = start.getCounterpart();
    try {
      if (!lockEdit(counterpart, counterpart.getItems())) return false;
      addToStart(Collections.singleton(start));
      success = true;
    } finally {
      if (!success) counterpart.release();
    }
    return true;
  }

  public boolean lockEdit(EditCounterpart counterpart, LongList items) {
    return myEditLocks.lockEdit(items, counterpart);
  }

  private void addToStart(@Nullable Collection<EditStart> starts) {
    if (starts == null || starts.isEmpty()) return;
    boolean empty;
    synchronized (myToStart) {
      for (EditStart start : starts)
        if (start != null && !myToStart.contains(start)) myToStart.addLast(start);
      empty = myToStart.isEmpty();
    }
    if (!empty) {
      if (myStartTransaction.get() != null) return;
      ReadTransaction<Object> readTransaction = new StartTransaction();
      if (myStartTransaction.compareAndSet(null, readTransaction)) enquireRead(DBPriority.FOREGROUND, readTransaction);
    }
  }

  @Nullable
  private EditStart pollNextToStart() {
    synchronized (myToStart) {
      return myToStart.pollFirst();
    }
  }

  public void doRelease(EditCounterpart lock) {
    synchronized (myToStart) {
      for (EditStart start : myToStart) {
        if (start.getCounterpart() == lock) {
          myToStart.remove(start);
          break;
        }
      }
    }
    unlockEdit(lock, lock.getItems());
  }

  public void unlockEdit(EditCounterpart lock, LongList items) {
    myEditLocks.unlock(items, lock);
  }

  @Override
  public void requestAutoMerge(LongList items) {
    myMergeManager.requestAutoMerge(items);
  }

  <T> DBResult<T> enquireWrite(WriteTransaction<T> transaction) {
    return myDatabase.enquireWrite(transaction);
  }

  @Override
  public <T> DBResult<T> enquireRead(DBPriority priority, ReadTransaction<T> transaction) {
    return myDatabase.enquireRead(priority, transaction);
  }

  @Nullable
  public ItemAutoMerge getOperations(DBReader reader, long item) {
    return myOperationSelector != null ? myOperationSelector.getOperations(reader, item) : null;
  }

  private void clearShortLock(Object locker) {
    addToStart(myEditLocks.clearShortLocks(locker));
  }

  UploadLocks getUploadLocks() {
    return myUploadLocks;
  }

  public boolean includeLock(EditCounterpart target, LongList items, EditControl source) {
    if (items == null || items.isEmpty()) return false;
    EditCounterpart sourceImpl = Util.castNullable(EditCounterpart.class, source);
    if (sourceImpl == null) Log.error("Wrong source lock " + source);
    else return myEditLocks.includeLock(target, items, sourceImpl);
    return false;
  }

  public void notifyMerged(long icn, MergedEvent event) {
    myListeners.getDispatcher().onItemsMerged(icn, event);
  }

  private class StartTransaction implements ReadTransaction<Object> {
    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      try {
        while (true) {
          EditStart start = pollNextToStart();
          if (start == null) break;
          if (start.isReleased()) continue;
          if (!myEditLocks.ensureCanEdit(reader, start)) continue;
          boolean success = false;
          try {
            start.performStart(reader);
            success = true;
          } finally {
            if (!success) start.release();
          }
        }
        return null;
      } finally {
        myStartTransaction.compareAndSet(this, null);
        addToStart(Collections15.<EditStart>emptyList());
      }
    }
  }

  private static class MergeLocker implements Procedure<Boolean> {
    private static final TypedKey<MergeLocker> MERGE_FINISHED = TypedKey.create("mergeFinish");
    private final SyncManagerImpl myManager;

    private MergeLocker(SyncManagerImpl manager) {
      myManager = manager;
    }

    public void invoke(Boolean success) {
      myManager.clearShortLock(this);
    }

    @SuppressWarnings({"unchecked"})
    @NotNull
    public static MergeLocker getInstance(DBWriter transaction, SyncManagerImpl manager) {
      Map cache = transaction.getTransactionCache();
      MergeLocker locker = MERGE_FINISHED.getFrom(cache);
      if (locker == null) {
        locker = new MergeLocker(manager);
        transaction.finallyDo(ThreadGate.STRAIGHT, locker);
        MERGE_FINISHED.putTo(cache, locker);
        manager.myEditLocks.registerShortLocker(locker);
      }
      return locker;
    }

    @Nullable
    public EditCounterpart lockAll(LongList items, EditorLock ignoreLock) {
      if (items == null || items.isEmpty()) return null;
      return myManager.myEditLocks.shortLockAll(this, items, ignoreLock);
    }
  }


  private static class SimplifiedCommit implements CommitCounterpart {
    private final SyncManagerImpl myManager;
    private final LongList myItems;

    public SimplifiedCommit(SyncManagerImpl manager, LongList items) {
      myManager = manager;
      myItems = items;
    }

    @Override
    public TLongObjectHashMap<AttributeMap> prepareCommit(DBReader reader) {
      return BaseEditDrain.collectBases(reader, myItems);
    }

    @Override
    public void commitFinished(EditCommit commit, boolean success, boolean release) {
      myManager.clearShortLock(this);
    }

    public static boolean start(SyncManagerImpl manager, LongList items, EditCommit commit) {
      SimplifiedCommit counterpart = new SimplifiedCommit(manager, items);
      manager.myEditLocks.registerShortLocker(counterpart);
      boolean goOn = false;
      try {
        EditorLock lock = manager.myEditLocks.shortLockAll(counterpart, items, null);
        if (lock != null) return false;
        new CommitEditDrain(manager, counterpart, commit, null, true).start();
        goOn = true;
      } finally {
        if (!goOn) manager.clearShortLock(counterpart);
      }
      return true;
    }
  }
}
