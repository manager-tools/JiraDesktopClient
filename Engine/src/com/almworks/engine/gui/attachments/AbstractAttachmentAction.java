package com.almworks.engine.gui.attachments;

import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

abstract class AbstractAttachmentAction extends SimpleAction {
  public AbstractAttachmentAction(@Nullable String name) {
    super(name);
    watchRole(Attachment.ROLE);
  }

  public AbstractAttachmentAction(@Nullable String name, @Nullable Icon icon) {
    super(name, icon);
  }

  @Nullable
  protected static File getGoodFile(ActionContext context) throws CantPerformException {
    Attachment attachment = context.getSourceObject(Attachment.ROLE);
    return attachment.getLocalFile(context.getSourceObject(AttachmentsController.ROLE).getDownloadStatus());
  }
}
