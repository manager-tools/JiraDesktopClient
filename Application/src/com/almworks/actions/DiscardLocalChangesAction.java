package com.almworks.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.Synchronizer;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.integers.WritableLongListIterator;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.AppBook;
import com.almworks.util.Terms;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.LText1;
import com.almworks.util.i18n.LText2;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.actions.*;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Log;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author dyoma
 */
public class DiscardLocalChangesAction extends SimpleAction {
  private static final String PREFIX = "Application.Actions.";
  private static final Condition<ItemWrapper> DISCARDABLE_ITEM = new Condition<ItemWrapper>() {
    public boolean isAccepted(ItemWrapper wrapper) {
      return wrapper.getDBStatus().isDiscardable();
    }
  };

  public DiscardLocalChangesAction() {
    super("Discard &Changes", Icons.ACTION_DISCARD);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.watchModifiableRole(SyncManager.MODIFIABLE);
    ItemActionUtils.basicUpdate(context, true);

    SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);
    List<ItemWrapper> items = getItemWrappers(context);
    int count = items.size();
    for (ItemWrapper item : items) {
      if (syncMan.findLock(item.getItem()) != null || item.services().isLockedForUpload()) {
        count--;
      } else {
        LongList slaves = item.getMetaInfo().getSlavesToDiscard(item);
        if (syncMan.findAnyLock(slaves) != null) count--;
      }
    }
    context.setEnabled(count > 0);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);

    final LongArray items = new LongArray();
    int discarding = 0, removing = 0;
    for(final ItemWrapper iw : getItemWrappers(context)) {
      final long item = iw.getItem();
      if(syncMan.findLock(item) == null) {
        items.add(item);
        if(iw.getDBStatus() == ItemWrapper.DBStatus.DB_NEW) {
          removing++;
        } else {
          discarding++;
        }
      } 
    }
    CantPerformException.ensure(!items.isEmpty());

    final Component component = context.getComponent();
    final String confirmationMsg = createConfirmationMessage(discarding, removing);
    final Synchronizer sync = context.getSourceObject(Engine.ROLE).getSynchronizer();

    CantPerformException.ensure(
      CantPerformException.ensureNotNull(syncMan.prepareEdit(items)).start(new MyDiscardFactory(component, confirmationMsg, sync)));
  }

  private static EditCommit createDiscardCommit(final LongArray items, final Synchronizer sync, final SynchronizedBoolean alive) {
    return new EditCommit() {
      @Override
      public void performCommit(EditDrain drain) {
        for (WritableLongListIterator i = items.iterator(); i.hasNext();) {
          long primary = i.nextValue();
          boolean discardSucceeded = drain.discardChanges(primary);
          if (!discardSucceeded) {
            Log.warn("discard did not succeed for item " + primary);
          }
        }
      }

      @Override
      public void onCommitFinished(boolean success) {
        if(success) {
          ThreadGate.AWT_QUEUED.execute(new Runnable() { public void run() {
            for (WritableLongListIterator i = items.iterator(); i.hasNext();) {
              removeProblem(i.nextValue(), sync);
            }
          }});
        } else {
          alive.set(false);
        }
      }
    };
  }

  public String toString() {
    return "DiscardLocalChangesAction";
  }

  private static boolean confirmDiscard(Component component, String message) {
    Component c = component == null ? null : SwingUtilities.getWindowAncestor(component);
    return (DialogsUtil.YES_OPTION == DialogsUtil.askUser(c, message, "Discard Changes", DialogsUtil.YES_NO_OPTION));
  }

  private static String createConfirmationMessage(int discardCount, int removeCount) {
    final String message;
    if(discardCount > 0 && removeCount == 0) {
      message = DISCARD_CONFIRM().format(discardCount);
    } else if (discardCount == 0 && removeCount > 0) {
      message = REMOVE_CONFIRM().format(removeCount);
    } else {
      message = MIXED_CONFIRM().format(discardCount, removeCount);
    }
    return message;
  }

  private static LText1<Integer> REMOVE_CONFIRM() {
    return AppBook.text(PREFIX + "REMOVE_CONFIRM", Local.parse(
      "Are you sure you want to remove {0,choice,1#this $(" + Terms.key_artifact + ")|1<these {0,number} " +
        Terms.ref_artifacts + "}?"), 0);
  }

  private static LText1<Integer> DISCARD_CONFIRM() {
    return AppBook.text(PREFIX + "DISCARD_CONFIRM", Local.parse(
      "Are you sure you want to discard {0,choice,1#changes|1<the changes to {0,number} " + Terms.ref_artifacts + "}?"),
      0);
  }

  private static LText2<Integer, Integer> MIXED_CONFIRM() {
    return AppBook.text(PREFIX + "MIXED_CONFIRM", Local.parse(
      "Are you sure you want to discard the changes to {0,number} " + Terms.ref_artifacts + " and to remove " +
        "{1, number} new " + Terms.ref_artifacts + "?"), 0, 0);
  }

  private static List<ItemWrapper> getItemWrappers(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    return DISCARDABLE_ITEM.filterList(wrappers);
  }

  private static void removeProblem(long item, Synchronizer synchronizer) {
    if (synchronizer != null) {
      for (ItemSyncProblem problem : synchronizer.getItemProblems(item)) {
        problem.disappear();
      }
    }
  }

  private static class DiscardEditor implements ItemEditor {
    private final SynchronizedBoolean myAlive;
    private final Component myComponent;
    private final String myConfirmationMsg;
    private final LongArray myItemsToDiscard;
    private final Synchronizer mySync;
    private final EditControl myEditControl;

    public DiscardEditor(Component component, String confirmationMsg, LongArray itemsToDiscard, Synchronizer sync, EditControl editControl, SynchronizedBoolean aliveFlag) {
      myComponent = component;
      myConfirmationMsg = confirmationMsg;
      myItemsToDiscard = itemsToDiscard;
      mySync = sync;
      myEditControl = editControl;
      myAlive = aliveFlag;
    }

    @Override
    public void showEditor() throws CantPerformException {
      if (!confirmDiscard(myComponent, myConfirmationMsg)) throw new CantPerformExceptionSilently("Rejected");
      ThreadGate.AWT_QUEUED.execute(new Runnable() { public void run() {
        myEditControl.commit(createDiscardCommit(myItemsToDiscard, mySync, myAlive));
      }});
    }

    @Override
    public boolean isAlive() {
      return myAlive.get();
    }

    @Override
    public void activate() throws CantPerformException {}

    @Override
    public void onEditReleased() {}

    @Override
    public void onItemsChanged(TLongObjectHashMap<ItemValues> newValues) {}
  }


  private static class MyDiscardFactory implements EditorFactory {
    private final Component myComponent;
    private final String myConfirmationMsg;
    private final Synchronizer mySync;
    private final SynchronizedBoolean myAliveFlag = new SynchronizedBoolean(true);

    public MyDiscardFactory(Component component, String confirmationMsg, Synchronizer sync) {
      myComponent = component;
      myConfirmationMsg = confirmationMsg;
      mySync = sync;
    }

    @Override
    public void editCancelled() {
      myAliveFlag.set(false);
    }

    @Override
    public ItemEditor prepareEdit(DBReader reader, final EditPrepare prepare) throws DBOperationCancelledException {
      LongArray primaryItems = LongArray.copy(prepare.getItems());
      final LongArray itemsToDiscard = new LongArray(primaryItems.size());
      for (LongListIterator i = primaryItems.iterator(); i.hasNext();) {
        long primary = i.nextValue();
        LongList subtree = SyncUtils.getSlavesSubtree(reader, primary);
        if (prepare.addItems(subtree)) {
          itemsToDiscard.add(primary);
        }
      }
      if (itemsToDiscard.isEmpty()) return null;
      return new DiscardEditor(myComponent, myConfirmationMsg, itemsToDiscard, mySync, prepare.getControl(), myAliveFlag);
    }
  }
}
