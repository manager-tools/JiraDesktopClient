package com.almworks.edit;

import com.almworks.api.edit.EditLifecycle;
import com.almworks.api.gui.BasicWindowBuilder;
import com.almworks.api.gui.WindowController;
import com.almworks.integers.LongList;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditControl;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @deprecated replaced in JC3
 */
@Deprecated
public class EditLifecycleImpl implements EditLifecycle {
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  @Nullable
  private final EditControl myEditControl;
  private final AtomicBoolean myDuringCommit = new AtomicBoolean(false);
  private boolean myWindowClosed = false;

  // AWT-confined
  protected AnActionListener myCloseConfirmation;
  protected static final int CLOSE_CONFIRMATION_ENABLED = 0;
  private static final int CLOSE_CONFIRMATION_DISABLED_CAN_CLOSE = 1;
  private static final int CLOSE_CONFIRMATION_DISABLED_NO_CLOSE = 2;
  protected int myCloseConfirmationState = CLOSE_CONFIRMATION_ENABLED;

  protected EditLifecycleImpl(EditControl editControl) {
    myEditControl = editControl;
  }

  public static EditLifecycleImpl install(BasicWindowBuilder builder, @Nullable EditControl control) {
    final EditLifecycleImpl life = new EditLifecycleImpl(control);
    builder.detachOnDispose(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        EditControl editControl = life.myEditControl;
        if (editControl != null) editControl.release();
      }
    });
    ConstProvider provider = new ConstProvider();
    provider.addData(ROLE, life);
    provider.addData(MODIFIABLE, life.getModifiable());
    builder.addProvider(provider);
    builder.setCloseConfirmation(new AnActionListener() {
      @Override
      public void perform(ActionContext context) throws CantPerformException {
        if (life.myCloseConfirmationState == CLOSE_CONFIRMATION_ENABLED && life.myCloseConfirmation != null) {
          CantPerformExceptionExplained dontClose = ActionUtil.performSafe(life.myCloseConfirmation, context);
          if (dontClose != null) throw dontClose;
        }
      }
    });
    return life;
  }

  public static EditLifecycleImpl testCreate(EditControl control) {
    return new EditLifecycleImpl(control);
  }

  @Override
  public void commit(ActionContext context, final EditCommit commit, boolean unsafe) throws CantPerformException {
    final WindowController window = context.getSourceObject(WindowController.ROLE);
    AggregatingEditCommit wrapper = AggregatingEditCommit.toAggregating(commit);
    wrapper.addProcedure(ThreadGate.AWT, new EditCommit.Adapter() {
      @Override
      public void onCommitFinished(boolean success) {
        if (success) commitSuccessfullyFinished(window);
        else announceEditing(window);
      }
    });
    setCloseConfirmationState(CLOSE_CONFIRMATION_DISABLED_NO_CLOSE, window);
    if (!myDuringCommit.compareAndSet(false, true)) {
      setCloseConfirmationState(CLOSE_CONFIRMATION_ENABLED, window);
      Log.error("Duplicated commit");
      return;
    }
    myModifiable.fireChanged();
    boolean success = false;
    try {
      if (myEditControl == null || unsafe) {
        SyncManager manager = context.getSourceObject(SyncManager.ROLE);
        if (unsafe) {
          assert myEditControl == null;
          manager.unsafeCommitEdit(wrapper);
        } else manager.commitEdit(wrapper);
      } else if (!myEditControl.commit(wrapper)) return;
      success = true;
    } finally {
      if (!success) {
        myDuringCommit.set(false);
        setCloseConfirmationState(CLOSE_CONFIRMATION_ENABLED, window);
        myModifiable.fireChanged();
      }
    }
    myModifiable.fireChanged();
  }

  private void setCloseConfirmationState(int state, WindowController window) {
    myCloseConfirmationState = state;
    if (state == CLOSE_CONFIRMATION_ENABLED) {
      window.enableCloseConfirmation();
    } else {
      window.disableCloseConfirmation(state == CLOSE_CONFIRMATION_DISABLED_CAN_CLOSE);
    }
  }

  @Override
  public final void commit(ActionContext context, EditCommit commit) throws CantPerformException {
    commit(context, commit, false);
  }

  @Override
  public void discardEdit(ActionContext context) throws CantPerformException {
    Threads.assertAWTThread();
    if (myDuringCommit.get()) {
      Log.warn("Attempt to discard edit during commit");
      return;
    }
    if (myCloseConfirmationState == CLOSE_CONFIRMATION_DISABLED_NO_CLOSE) {
      Log.debug("ELI: no close");
      return;
    }
    WindowController.CLOSE_ACTION.perform(context);
    if (myEditControl != null) myEditControl.release();
  }

  public void setDiscardConfirmation(@NotNull AnActionListener confirmation) {
    Threads.assertAWTThread();
    myCloseConfirmation = confirmation;
  }

  @Override
  public LongList getEditingItems() {
    return myEditControl == null ? LongList.EMPTY : myEditControl.getItems();
  }

  private void announceEditing(WindowController window) {
    if (!myDuringCommit.compareAndSet(true, false)) Log.error("Not during commit");
    setCloseConfirmationState(CLOSE_CONFIRMATION_ENABLED, window);
    myModifiable.fireChanged();
  }

  private void commitSuccessfullyFinished(WindowController window) {
    if (!myDuringCommit.compareAndSet(true, false)) Log.error("Not during commit");
    setCloseConfirmationState(CLOSE_CONFIRMATION_DISABLED_CAN_CLOSE, window);
    myModifiable.fireChanged();
    closeWindow(window);
  }

  protected void closeWindow(WindowController window) {
    if (!myWindowClosed) {
      CantPerformExceptionExplained cannotClose = window.close();
      assert cannotClose == null : window + " " + cannotClose;
      myWindowClosed = cannotClose == null;
    }
  }

  @Override
  public Modifiable getModifiable() {
    return myModifiable;
  }

  @Override
  public void checkCommitAction() throws CantPerformException {
    CantPerformException.ensure(!myDuringCommit.get());
  }

  @Override
  public boolean isDuringCommit() {
    return myDuringCommit.get();
  }

  @Override
  public EditControl getControl() {
    return myEditControl;
  }
}
