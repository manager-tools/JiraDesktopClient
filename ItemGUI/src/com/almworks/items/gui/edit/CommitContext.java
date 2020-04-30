package com.almworks.items.gui.edit;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CommitContext {
  private final EditDrain myDrain;
  private final EditItemModel myModel;
  private final ItemVersionCreator myThisItem;
  private final CommitRoot myRoot;

  private CommitContext(EditDrain drain, EditItemModel model, ItemVersionCreator thisItem, CommitRoot root) {
    myDrain = drain;
    myModel = model;
    myThisItem = thisItem;
    myRoot = root;
  }
  
  public static CommitRoot createRoot(SyncManager manager, EditDrain drain, EditItemModel model, ItemVersionCreator thisItem) {
    return new CommitRoot(drain, model, thisItem, manager);
  }

  public EditItemModel getModel() {
    return myModel;
  }

  public long getItem() {
    return myThisItem.getItem();
  }

  public ItemVersionCreator getCreator() {
    markNotCancellable();
    return myThisItem;
  }

  public EditDrain getDrain() {
    markNotCancellable();
    return myDrain;
  }

  @NotNull
  public ItemVersion readTrunk(long item) {
    return myDrain.forItem(item);
  }

  public ItemVersion readTrunk() {
    return myThisItem;
  }

  public DBReader getReader() {
    return myDrain.getReader();
  }

  public CommitContext subContext(EditItemModel childModel) {
    return new CommitContext(myDrain, childModel, myThisItem, myRoot);
  }

  public CommitContext newSubContext(EditItemModel newItemModel) {
    return new CommitContext(myDrain, newItemModel, myDrain.createItem(), myRoot);
  }

  public CommitContext createNew() {
    return new CommitContext(myDrain, myModel, myDrain.createItem(), myRoot);
  }

  public CommitContext itemContext(long item) {
    return new CommitContext(myDrain, myModel, myDrain.changeItem(item), myRoot);
  }

  /**
   * Commits all commit editors of the model. Desired commit order can be specified
   * @param order if not null specifies desirable commit order. If the order contains not commit editor it is not committed.
   *              If some commit editors are missing in the order - they are committed after all ordered editors.
   * @throws CancelCommitException
   */
  public void commitEditors(@Nullable List<? extends FieldEditor> order) throws CancelCommitException {
    Collection<FieldEditor> editors = Collections15.hashSet(myModel.getCommitEditors());
    if (order != null)
      for (FieldEditor editor : order) {
        if (editors.remove(editor)) editor.commit(this);
      }
    for (FieldEditor editor : editors) editor.commit(this);
  }

  private void markNotCancellable() {
    myRoot.markNotCancellable();
  }

  public void afterCommit(Procedure<CommitContext> finish) {
    myRoot.afterCommit(this, finish);
  }

  public SyncManager getManager() {
    return myRoot.getManager();
  }

  public static class CommitRoot {
    private final List<Pair<CommitContext, Procedure<CommitContext>>> myAfterCommit = Collections15.arrayList();
    private boolean myCancellable = true;
    private final CommitContext myRootContext;
    private final SyncManager myManager;

    public CommitRoot(EditDrain drain, EditItemModel model, ItemVersionCreator item, SyncManager manager) {
      myManager = manager;
      myRootContext = new CommitContext(drain, model, item, this);
    }

    public CommitContext getRootContext() {
      return myRootContext;
    }

    public boolean canCancel() {
      return myCancellable;
    }

    void markNotCancellable() {
      myCancellable = false;
    }

    void afterCommit(CommitContext context, Procedure<CommitContext> finish) {
      if (context == null || finish == null) return;
      myAfterCommit.add(Pair.create(context, finish));
    }

    public void finishCommit() {
      for (Pair<CommitContext, Procedure<CommitContext>> pair : myAfterCommit) pair.getSecond().invoke(pair.getFirst());
    }

    public SyncManager getManager() {
      return myManager;
    }
  }
}
