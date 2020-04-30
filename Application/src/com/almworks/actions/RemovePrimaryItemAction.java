package com.almworks.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.Synchronizer;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBWriter;
import com.almworks.items.api.Database;
import com.almworks.items.api.WriteTransaction;
import com.almworks.items.sync.EditorLock;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.English;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.collections.PrimitiveUtils;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

/**
 * Removes item from the local database only. It is not an edit that can be synchronized with server. <br>
 * This is a maintenance action and should be carried out in special cases, like to remove an item stuck due to internal application problems and download it again from the server.
 */
public class RemovePrimaryItemAction extends SimpleAction {
  private final Synchronizer mySynchronizer;

  public RemovePrimaryItemAction(Engine engine) {
    super(L.actionName("Remove &Invalid $(app.term.Artifact)"));
    watchRole(ItemWrapper.ITEM_WRAPPER);
    mySynchronizer = engine.getSynchronizer();
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<ItemWrapper> collection = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    context.setEnabled(!collection.isEmpty());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = CantPerformException.ensureNotEmpty(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    final String text = confirmationText(wrappers);
    final Component contextComponent = context.getComponent();

    final String title = L.dialog("Confirm " + Local.parse(Terms.ref_Artifact) + " Removal");
    int r = JOptionPane.showConfirmDialog(contextComponent, text, title, JOptionPane.YES_NO_OPTION);
    if (r == JOptionPane.YES_OPTION) {
      doRemoveItems(context, wrappers, mySynchronizer);
    }
  }

  private static String confirmationText(List<ItemWrapper> wrappers) {
    String text = English.getSingularOrPlural(Local.parse(Terms.ref_artifact), wrappers.size());
    return "<html><body>You're about to remove selected " + text + " from the local database.<br>" +
      "This is a maintenance action and should be done only to get rid of invalid " + Local.parse(Terms.ref_artifacts) +
      ".<br>" + "Proceed with removal?";
  }

  public static void doRemoveItems(ActionContext context, Collection<ItemWrapper> wrappers, final Synchronizer synchronizer) throws CantPerformException {
    final LongList primaryItems = PrimitiveUtils.collect(UiItem.GET_ITEM, wrappers);
    final SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);
    context.getSourceObject(Database.ROLE).writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        clearItems(writer, primaryItems, syncMan);
        return null;
      }
    }).onSuccess(ThreadGate.LONG_QUEUED, new Procedure<Object>() { @Override public void invoke(Object arg) {
      clearItemsProblems(synchronizer, primaryItems);
    }});
  }

  private static void clearItems(DBWriter writer, LongList primaryItems, SyncManager syncMan) {
    for (LongListIterator i = primaryItems.iterator(); i.hasNext();) {
      LongList slaves = SyncUtils.getSlavesSubtree(writer, i.nextValue());
      for (LongListIterator itemIt = slaves.iterator(); itemIt.hasNext();) {
        long item = itemIt.nextValue();
        EditorLock editor = syncMan.findLock(item);
        if (editor != null) editor.release();
        writer.clearItem(item);
      }
    }
  }

  private static void clearItemsProblems(Synchronizer synchronizer, LongList primaryItems) {
    for (ItemSyncProblem p : ItemSyncProblem.SELECT.invoke(synchronizer.getProblems().copyCurrent())) {
      if (primaryItems.contains(p.getItem())) {
        p.disappear();
      }
    }
  }
}
