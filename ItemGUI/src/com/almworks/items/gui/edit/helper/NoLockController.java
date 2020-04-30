package com.almworks.items.gui.edit.helper;

import com.almworks.api.gui.WindowController;
import com.almworks.items.api.*;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.detach.Detach;

import java.util.concurrent.atomic.AtomicBoolean;

class NoLockController implements ItemEditController, ReadTransaction<Object>, Procedure<DBResult<Object>> {
  private final EditFeature myEditor;
  private final DefaultEditModel.Root myModel;
  private final AtomicBoolean myDuringCommit = new AtomicBoolean(false);
  private final EditorContent myContent;

  public NoLockController(EditFeature editor, DefaultEditModel.Root model, EditorContent content) {
    myEditor = editor;
    myModel = model;
    myContent = content;
  }

  /**
   * @param contextKey see {@link EditDescriptor#getContextKey()}
   */
  public static void start(final EditItemHelper helper, final EditFeature editor, DefaultEditModel.Root model, EditDescriptor descriptor, final Object contextKey) {
    EditorContent content = new EditorContent(helper, descriptor);
    NoLockController controller = new NoLockController(editor, model, content);
    if (contextKey != null) {
      helper.registerWindow(controller, editor, contextKey);
      content.getLifespan().add(new Detach() {
        protected void doDetach() {
          helper.unregisterWindow(editor, contextKey);
        }
      });
    }
    helper.getSyncManager().enquireRead(DBPriority.FOREGROUND, controller).finallyDoWithResult(ThreadGate.AWT, controller);
    content.showWaitMessage();
  }

  @Override
  public Object transaction(DBReader reader) throws DBOperationCancelledException {
    myEditor.prepareEdit(reader, myModel, null);
    return null;
  }

  @Override
  public void invoke(DBResult<Object> result) {
    if (result.isSuccessful()) myContent.showWindow(myEditor, myModel, this);
    else myContent.dispose();
  }

  @Override
  public void commit(ActionContext context, final EditCommit commit) throws CantPerformException {
    final WindowController windowController = context.getSourceObject(WindowController.ROLE);
    EditCommit editCommit = new EditCommit() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        commit.performCommit(drain);
      }

      @Override
      public void onCommitFinished(boolean success) {
        myDuringCommit.set(false);
        commit.onCommitFinished(success);
        if (success)
          ThreadGate.AWT.execute(new Runnable() {
            @Override
            public void run() {
              windowController.disableCloseConfirmation(true);
              CantPerformExceptionExplained explained = windowController.close();
              LogHelper.assertError(explained == null, explained);
            }
          });
      }
    };
    SyncManager syncManager = context.getSourceObject(SyncManager.ROLE);
    CantPerformException.ensure(myDuringCommit.compareAndSet(false, true));
    syncManager.commitEdit(editCommit);
  }

  public void activate() {
    myContent.activate();
  }
}
