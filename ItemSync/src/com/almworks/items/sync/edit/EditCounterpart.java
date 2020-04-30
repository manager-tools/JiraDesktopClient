package com.almworks.items.sync.edit;

import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.collections.LongSet;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class EditCounterpart implements EditControl, CommitCounterpart {
  private static final int STATE_INIT = 0;
  private static final int STATE_PREPARING = 1;
  private static final int STATE_EDITING = 2;
  private static final int STATE_COMMITTING = 3;
  private static final int STATE_COMMIT_DONE = 4;
  /**
   * Edit lock is released. Switching to this state should be guarded by myMergeWhenRelease 
   */
  private static final int STATE_RELEASED = 5;

  // Guarded by myMergeWhenRelease
  private final LongSetBuilder myMergeWhenRelease = new LongSetBuilder();
  private final SyncManagerImpl myManager;
  private final AtomicInteger myState = new AtomicInteger(STATE_INIT);
  private final AtomicReference<EditCommit> myCurrentCommit = new AtomicReference<EditCommit>(null);
  private final AtomicReference<ItemEditor> myEditor = new AtomicReference<ItemEditor>();
  private final LongSet myItems = new LongSet();
  private volatile LongList myItemsCopy = null;
  private final TLongObjectHashMap<AttributeMap> myBases = new TLongObjectHashMap<>();
  private final AtomicReference<EditStart> myEditStart = new AtomicReference<EditStart>(null);

  public EditCounterpart(SyncManagerImpl manager, LongList items) {
    myManager = manager;
    myItems.addAll(items);
  }

  @Override
  public boolean start(@Nullable EditorFactory factory) {
    if (factory == null) return false;
    if (!myState.compareAndSet(STATE_INIT, STATE_PREPARING)) return false;
    boolean success = false;
    try {
      EditStart start = new EditStart(this, factory);
      if (myEditStart.compareAndSet(null, start)) success = myManager.start(start);
    } finally {
      if (!success) release();
    }
    return success;
  }

  @Override
  public boolean commit(EditCommit commit) {
    if (!myState.compareAndSet(STATE_EDITING, STATE_COMMITTING)) return false;
    boolean success = false;
    try {
      if (myCurrentCommit.compareAndSet(null, commit)) {
        new CommitEditDrain(myManager, EditCounterpart.this, commit, EditCounterpart.this, true).start();
      } else {
        success = true;
        return false;
      }
      success = true;
    } finally {
      if (!success) release();
    }
    return success;
  }

  @Override
  public void activateEditor() throws CantPerformException {
    ItemEditor editor = myEditor.get();
    if (editor != null) editor.activate();
  }

  @Override
  public void release() {
    LongList toMerge;
    while (true) {
      int state = myState.get();
      if (state == STATE_COMMITTING || state == STATE_RELEASED) return;
      synchronized (myMergeWhenRelease) {
        if (myState.compareAndSet(state, STATE_RELEASED)) {
          toMerge = myMergeWhenRelease.toArray();
          myMergeWhenRelease.clear(false);
          break;
        }
      }
    }
    myManager.requestAutoMerge(toMerge);
    myManager.doRelease(this);
    final ItemEditor editor = myEditor.get();
    if (myEditor.compareAndSet(editor, null)) releaseEditor(editor);
    EditStart start = myEditStart.get();
    if (myEditStart.compareAndSet(start, null) && start != null) start.onEditCancelled();
  }

  private void releaseEditor(final ItemEditor editor) {
    if (editor != null) ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        editor.onEditReleased();
      }
    });
  }

  @Override
  public boolean include(LongList items, EditorFactory loader) {
    if (items == null) items = LongList.EMPTY;
    if (myState.get() != STATE_EDITING) return false;
    EditControl control = myManager.prepareEdit(items);
    return control != null && control.start(new InclusionFactory(loader, this));
  }

  @Override
  public boolean isAlive() {
    int state = myState.get();
    switch (state) {
    case STATE_INIT:
    case STATE_COMMIT_DONE:
    case STATE_RELEASED: return false;
    case STATE_EDITING:
      ItemEditor editor = myEditor.get();
      if (editor == null) {
        release();
        return false;
      }
      return editor.isAlive();
    case STATE_PREPARING:
    case STATE_COMMITTING: return true;
    default:
      assert false : state;
      return false;
    }
  }

  @Override
  public boolean isPreparing() {
    int state = myState.get();
    return state > STATE_INIT && state < STATE_EDITING;
  }

  @Override
  public boolean isCommitting() {
    return myState.get() == STATE_COMMITTING;
  }

  @Override
  public LongList getItems() {
    LongList copy = myItemsCopy;
    if (copy == null) {
      synchronized (myItems) {
        copy = LongSet.copy(myItems);
      }
      myItemsCopy = copy;
    }
    return copy;
  }

  public TLongObjectHashMap<AttributeMap> prepareCommit(DBReader reader) {
    if (myState.get() != STATE_COMMITTING) {
      release();
      return null;
    }
    synchronized (myBases) {
      return myBases.clone();
    }
  }

  public void commitFinished(EditCommit commit, boolean success, boolean release) {
    if (!myCurrentCommit.compareAndSet(commit, null)) Log.error("Wrong commit finished " + commit + " " + myCurrentCommit.get());
    else if (!myState.compareAndSet(STATE_COMMITTING, success ? STATE_COMMIT_DONE : STATE_EDITING)) release();
    if (release) release();
  }

  @Override
  public String toString() {
    return "EditCounterpart (state=" + myState.get() + " editor=" + myEditor.get() + " commit=" + myCurrentCommit.get() + " start=" + myEditStart.get() + ")";
  }

  /**
   * Enqueue item to merge when the lock is released
   * @return true iff this lock is still held and the item is enqueued,
   * false means the lock is not held right now - no item enqueued
   */
  public boolean mergeWhenReleased(LongList items) {
    synchronized (myMergeWhenRelease) {
      if (myState.get() == STATE_RELEASED) return false;
      myMergeWhenRelease.addAll(items);
      return true;
    }
  }

  public boolean beforeStart(DBReader reader) {
    int state = myState.get();
    boolean success = false;
    try {
      if (state != STATE_PREPARING) {
        if (state != STATE_RELEASED) Log.error("Wrong state " + state);
      } else {
        LongList items = getItems();
        collectBases(reader, items);
        success = true;
      }
      return success;
    } finally {
      if (!success) release();
    }
  }

  private void collectBases(DBReader reader, LongList items) {
    synchronized (myBases) {
      BaseEditDrain.collectBases(reader, items, myBases);
    }
  }

  public boolean addItems(DBReader reader, LongList items) {
    if (items == null || items.isEmpty()) return true;
    if (!myManager.lockEdit(this, items)) return false;
    collectBases(reader, items);
    return true;
  }

  public void afterStart(final ItemEditor editor) {
    if (editor != null) {
      ThreadGate.AWT.execute(new Runnable() {
        @Override
        public void run() {
          boolean success = false;
          try {
            if (myState.get() == STATE_PREPARING) editor.showEditor();
            else return;
            if (!myEditor.compareAndSet(null, editor)) {
              editor.onEditReleased();
              return;
            }
            myEditStart.set(null);
            success = myState.compareAndSet(STATE_PREPARING, STATE_EDITING);
            if (!success && myEditor.compareAndSet(editor, null)) editor.onEditReleased();
          } catch (CantPerformExceptionSilently e){
            // ignore
          } catch (CantPerformException e) {
            Log.warn("Edit cancelled", e);
          } finally {
            if (!success) {
              boolean releaseEditor = myEditor.get() != editor;
              release();
              if (releaseEditor) releaseEditor(editor);
            }
          }
        }
      });
    }
  }
  
  public boolean isReleased() {
    return myState.get() == STATE_RELEASED;
  }

  public boolean removeLocks(LongList item) {
    if (!isPreparing()) return false;
    priRemoveLocks(item);
    return true;
  }

  private void priRemoveLocks(LongList item) {
    synchronized (myItems) {
      myItems.removeAll(item);
      myItemsCopy = null;
    }
  }

  public boolean itemsLocked(LongList items) {
    if (isCommitting()) return false;
    synchronized (myItems) {
      myItems.addAll(items);
      myItemsCopy = null;
    }
    return true;
  }

  public AttributeMap getItemBase(long item) {
    synchronized (myBases) {
      return myBases.get(item);
    }
  }

  @ThreadAWT
  public void notifyConcurrentEdit(TLongObjectHashMap<ItemValues> values) {
    if (myState.get() != STATE_EDITING) return;
    ItemEditor editor = myEditor.get();
    if (editor == null) return;
    editor.onItemsChanged(values);
  }

  private static class InclusionFactory implements EditorFactory {
    private final EditCounterpart myCounterpart;
    private final EditorFactory myLoader;
    private final AtomicReference<Boolean> myCancelLoader = new AtomicReference<Boolean>(null);

    public InclusionFactory(EditorFactory loader, EditCounterpart counterpart) {
      myLoader = loader;
      myCounterpart = counterpart;
    }

    @Override
    public ItemEditor prepareEdit(DBReader reader, EditPrepare prepare) throws DBOperationCancelledException {
      EditControl source = prepare.getControl();
      try {
        if (myCounterpart.myState.get() != STATE_EDITING) return null;
        LongSet items = LongSet.copy(prepare.getItems());
        items.removeAll(myCounterpart.getItems());
        InclusionPrepare inclusion = new InclusionPrepare(reader, items, myCounterpart);
        if (!myCounterpart.myManager.includeLock(myCounterpart, items, source)) return null;
        myCounterpart.collectBases(reader, items);
        if (!myCancelLoader.compareAndSet(null, false)) return null; // Already cancelled
        ItemEditor edit = myLoader.prepareEdit(reader, inclusion);
        if (edit == null) {
          LongSet justAdded = inclusion.myItems;
          myCounterpart.priRemoveLocks(justAdded);
          myCounterpart.myManager.unlockEdit(myCounterpart, justAdded);
        } else edit.onEditReleased();
      } finally {
        source.release();
      }
      return null;
    }

    @Override
    public void editCancelled() {
      if (myCancelLoader.compareAndSet(null, true)) myLoader.editCancelled();
    }
  }

  private static class InclusionPrepare implements EditPrepare {
    private final EditCounterpart myCounterpart;
    private final DBReader myReader;
    private final LongSet myItems;

    private InclusionPrepare(DBReader reader, LongList items, EditCounterpart counterpart) {
      myReader = reader;
      myCounterpart = counterpart;
      myItems = LongSet.copy(items);
    }

    @Override
    public LongList getItems() {
      return myItems;
    }

    @Override
    public EditControl getControl() {
      return myCounterpart;
    }

    @Override
    public boolean addItems(LongList items) {
      if (items == null || items.isEmpty()) return true;
      LongSet copy = LongSet.copy(items);
      copy.removeAll(myCounterpart.getItems());
      if (copy.isEmpty()) return true;
      boolean success = myCounterpart.addItems(myReader, copy);
      if (success) myItems.addAll(copy);
      return success;
    }
  }
}
