package com.almworks.jira.provider3.services.upload.queue;

import com.almworks.api.application.DBStatusKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBPriority;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.SyncState;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.SetHolder;
import com.almworks.util.model.SetHolderModel;
import com.almworks.util.threads.InterruptableRunnable;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class ProblemsMaintenance implements SetHolder.Listener<SyncProblem>,SyncManager.Listener {
  private final SetHolderModel<SyncProblem> myProblems;
  private final LongSet myProblematicItems = new LongSet();
  private final SyncManager myManager;
  private final CheckSyncState myItemCheck;
  private final AtomicReference<DetachComposite> myStarted = new AtomicReference<DetachComposite>(null);

  ProblemsMaintenance(SetHolderModel<SyncProblem> problems, Connection connection, SyncManager manager) {
    myProblems = problems;
    myManager = manager;
    myItemCheck = new CheckSyncState(connection, this);
  }

  public void ensureStarted() {
    if (myStarted.get() != null) return;
    DetachComposite life = new DetachComposite();
    if (!myStarted.compareAndSet(null, life)) return;
    myProblems.addInitListener(life, ThreadGate.STRAIGHT, this);
    myManager.addListener(life, ThreadGate.STRAIGHT, this);
  }

  public void stop() {
    DetachComposite detach = myStarted.get();
    if (detach != null) {
      myStarted.compareAndSet(detach, null);
      detach.detach();
    }
  }

  @Override
  public void onSetChanged(@NotNull SetHolder.Event<SyncProblem> syncProblemEvent) {
    List<SyncProblem> problems = myProblems.copyCurrent();
    LongSet checkRequired;
    synchronized (myProblematicItems) {
      LongSet prevItems = LongSet.copy(myProblematicItems);
      myProblematicItems.clear();
      for (SyncProblem p : problems) {
        ItemSyncProblem problem = Util.castNullable(ItemSyncProblem.class, p);
        if (problem == null) continue;
        myProblematicItems.add(problem.getItem());
      }
      if (prevItems.equals(myProblematicItems)) return;
      checkRequired = LongSet.copy(myProblematicItems);
      checkRequired.removeAll(prevItems);
    }
    myItemCheck.request(myManager, checkRequired);
  }

  @Override
  public void onItemsMerged(long icn, SyncManager.MergedEvent event) {
    onItemsBecameSync(event.selectItems(SyncState.SYNC));
  }

  private void onItemsBecameSync(LongList items) {
    if (items == null || items.isEmpty()) return;
    LongArray clear = new LongArray();
    synchronized (myProblematicItems) {
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (myProblematicItems.contains(item)) clear.add(item);
      }
    }
    List<SyncProblem> problems = myProblems.copyCurrent();
    ArrayList<ItemSyncProblem> remove = Collections15.arrayList();
    for (int i = 0; i < clear.size(); i++) {
      remove.clear();
      selectForItem(problems, clear.get(i), remove);
      myProblems.remove(problems);
    }
  }

  private void selectForItem(Collection<SyncProblem> problems, long item, Collection<ItemSyncProblem> target) {
    for (SyncProblem p : problems) {
      ItemSyncProblem problem = Util.castNullable(ItemSyncProblem.class, p);
      if (problem != null && item == problem.getItem()) target.add(problem);
    }
  }

  private static class CheckSyncState implements ReadTransaction<Object> {
    private final LongSet myToCheck = new LongSet();
    private final Connection myConnection;
    private final ProblemsMaintenance myCallback;

    private CheckSyncState(Connection connection, ProblemsMaintenance callback) {
      myConnection = connection;
      myCallback = callback;
    }

    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      LongArray items;
      synchronized (myToCheck) {
        if (myToCheck.isEmpty()) return null;
        items = LongArray.copy(myToCheck);
        myToCheck.clear();
      }
      final LongArray sync = new LongArray();
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        ItemWrapper.DBStatus status = DBStatusKey.calcStatus(item, reader, myConnection);
        switch (status) {
        case DB_CONNECTION_NOT_READY:
        case DB_NOT_CHANGED:
          sync.add(item);
          break;
        case DB_MODIFIED:
        case DB_CONFLICT:
        case DB_NEW:
          break;
        default:
          LogHelper.error("Unknown status", status);
        }
      }
      ThreadGate.executeLong(new InterruptableRunnable() {
        @Override
        public void run() throws InterruptedException {
          myCallback.onItemsBecameSync(sync);
        }
      });
      return null;
    }

    public void request(SyncManager manager, LongSet items) {
      if (items == null || items.isEmpty()) return;
      boolean needsTransaction;
      synchronized (myToCheck) {
        needsTransaction = myToCheck.isEmpty();
        myToCheck.addAll(items);
      }
      if (needsTransaction) manager.enquireRead(DBPriority.BACKGROUND, this);
    }
  }
}
