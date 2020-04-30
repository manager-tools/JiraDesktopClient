package com.almworks.items.gui.edit.helper;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.Nullable;

class LockedController implements EditorFactory, ItemEditController, ItemEditor {
  private final EditFeature myEditor;
  private final DefaultEditModel.Root myModel;
  private final EditControl myEditControl;
  private final EditorContent myEditorContent;

  public LockedController(EditFeature editor, DefaultEditModel.Root model, EditControl control, EditorContent editorContent) {
    myEditor = editor;
    myModel = model;
    myEditControl = control;
    myEditorContent = editorContent;
  }

  public static void start(EditItemHelper helper, EditFeature editor, DefaultEditModel.Root model, EditDescriptor descriptor, EditControl control) {
    EditorContent content = new EditorContent(helper, descriptor);
    content.getLifespan().add(new EditorLock.ToDetach(control));
    control.start(new LockedController(editor, model, control, content));
    content.showWaitMessage();
  }

  @Override
  public ItemEditor prepareEdit(DBReader reader, @Nullable EditPrepare prepare) throws DBOperationCancelledException {
    myEditor.prepareEdit(reader, myModel, prepare);
    return this;
  }

  @Override
  public void commit(ActionContext context, EditCommit commit) throws CantPerformException {
    CantPerformException.ensure(!myEditorContent.isDisposed());
    if (!myEditControl.commit(commit)) throw new CantPerformException("Failed to start commit");
  }

  @Override
  public void showEditor() throws CantPerformException {
    CantPerformException.ensure(!myEditorContent.isDisposed());
    myEditorContent.showWindow(myEditor, myModel, this);
  }

  @Override
  public boolean isAlive() {
    return myEditorContent.isAlive();
  }

  @Override
  public void activate() throws CantPerformException {
    CantPerformException.ensure(!myEditorContent.isDisposed());
    myEditorContent.activate();
  }

  @Override
  public void onEditReleased() {
    myEditorContent.dispose();
  }

  @Override
  public void editCancelled() {
    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        myEditorContent.dispose();
      }
    });
  }

  @Override
  public void onItemsChanged(TLongObjectHashMap<ItemValues> newValues) {
    EditItemModel.notifyItemsChanged(myModel, newValues);
  }
}
