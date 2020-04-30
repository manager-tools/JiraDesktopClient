package com.almworks.actions.order;

import com.almworks.api.application.order.ReorderItem;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.FlatCollectionComponent;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author dyoma
 */
public class UndoHistory extends SimpleModifiable {
  public static final DataRole<UndoHistory> ROLE = DataRole.createRole(UndoHistory.class);
  public static final AnAction UNDO_ACTION = new UndoAction("Undo", Icons.ACTION_UNDO, true);
  public static final AnAction REDO_ACTION = new UndoAction("Redo", Icons.ACTION_REDO, false);
  private final List<UndoEntry> myHistory = Collections15.arrayList();
  private static final int LIMIT = 20;
  private int myRedo = 0;
  private final FlatCollectionComponent<ReorderItem> myComponent;
  private final OrderListModel<ReorderItem> myModel;

  public UndoHistory(FlatCollectionComponent<ReorderItem> component, OrderListModel<ReorderItem> model) {
    myComponent = component;
    myModel = model;
  }

  public void addEntry(ReorderItem[] prevOrder, Object[] prevValues, int[] selection) {
    addEntry(UndoEdit.createEdit(prevOrder, prevValues, selection));
  }

  private void addEntry(UndoEntry entry) {
    if (myHistory.size() == LIMIT)
      myHistory.remove(0);
    if (myHistory.size() > myRedo)
      myHistory.subList(myRedo, myHistory.size()).clear();
    myHistory.add(entry);
    myRedo = myHistory.size();
    fireChanged();
  }

  public boolean can(boolean undo) {
    return undo ? canUndo() : canRedo();
  }

  public void perform(boolean undo) {
    if (!can(undo)) {
      assert false;
      return;
    }
    if (undo)
      undo();
    else
      redo();
  }

  private void redo() {
    myHistory.get(myRedo).applyTo(myComponent, myModel);
    myRedo++;
    fireChanged();
  }

  private void undo() {
    myHistory.get(myRedo - 1).applyTo(myComponent, myModel);
    myRedo--;
    fireChanged();
  }

  public boolean canUndo() {
    return myRedo > 0;
  }

  public boolean canRedo() {
    return myRedo < myHistory.size();
  }

  public void addRemove(ReorderItem[] items, int[] selection) {
    addEntry(new UndoRemove(items, selection));
  }

  public void clear() {
    myHistory.clear();
    myRedo = 0;
    fireChanged();
  }

  private static class UndoAction extends SimpleAction {
    private final boolean myUndo;

    public UndoAction(@Nullable String name, @Nullable Icon icon, boolean undo) {
      super(name, icon);
      myUndo = undo;
      setDefaultPresentation(PresentationKey.SHORTCUT, undo ? Shortcuts.UNDO : Shortcuts.REDO);
      watchModifiableRole(ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(context.getSourceObject(ROLE).can(myUndo));
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      context.getSourceObject(ROLE).perform(myUndo);
    }
  }
}
