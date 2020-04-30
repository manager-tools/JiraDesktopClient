package com.almworks.engine.gui.attachments;

import com.almworks.util.files.FileActions;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;
import com.almworks.util.ui.actions.presentation.MenuBuilder;

import java.io.File;

import static com.almworks.api.gui.MainMenu.Attachments.*;
import static com.almworks.util.files.FileActions.Action.*;

class AttachmentFileAction extends AbstractAttachmentAction {
  private static final AttachmentFileAction[] ACTIONS = {
    new AttachmentFileAction(OPEN_EXTERNAL, FileActions.OPEN_TITLE, OPEN),
    new AttachmentFileAction(OPEN_WITH, FileActions.OPEN_WITH_TITLE, OPEN_AS),
    new AttachmentFileAction(OPEN_FOLDER, FileActions.OPEN_FOLDER_TITLE, OPEN_CONTAINING_FOLDER),
  };

  private final String myId;
  private final FileActions.Action myAction;

  private AttachmentFileAction(String id, String name, FileActions.Action action) {
    super(name);
    myId = id;
    myAction = action;
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(getGoodFile(context) != null);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    File file = getGoodFile(context);
    if (file != null) {
      FileActions.performAction(myAction, file, context.getComponent());
    }
  }

  private boolean isSupported() {
    return FileActions.isSupported(myAction);
  }

  public static void register(ActionRegistry registry) {
    for(final AttachmentFileAction afa : ACTIONS) {
      if(afa.isSupported()) {
        registry.registerAction(afa.myId, afa);
      }
    }
  }

  public static boolean installTo(MenuBuilder builder) {
    boolean result = false;
    for(final AttachmentFileAction afa : ACTIONS) {
      if(afa.isSupported()) {
        builder.addAction(afa.myId);
        result = true;
      }
    }
    return result;
  }
}
