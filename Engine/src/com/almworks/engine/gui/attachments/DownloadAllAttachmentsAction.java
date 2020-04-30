package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

class DownloadAllAttachmentsAction extends AbstractAttachmentAction {
  public static final AnAction INSTANCE = new DownloadAllAttachmentsAction();

  private DownloadAllAttachmentsAction() {
    super("&Download All", Icons.ACTION_DOWNLOAD_ATTACHMENT);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Download all attachments");
    setDefaultPresentation(PresentationKey.SHORTCUT,
      KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
    watchRole(AttachmentsController.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
    context.updateOnChange(controller.getDownloadedStatusModifiable());
    context.updateOnChange(controller.getAllAttachmentsModifiable());
    Collection<? extends Attachment> attachments = controller.getAttachments();
    int size = attachments.size();
    boolean download = false;
    for (Attachment attachment : attachments) {
      if (!attachment.isLocal()) {
        DownloadedFile dfile = attachment.getDownloadedFile(controller.getDownloadStatus());
        if (AttachmentsControllerUtil.isDownloadNeeded(dfile)) {
          download = true;
          break;
        }
      }
    }
    context.setEnabled(download);
    context.putPresentationProperty(PresentationKey.NAME, size == 1 ? "&Download" : "&Download All");
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
    Collection<? extends Attachment> attachments = controller.getAttachments();
    for (Attachment attachment : attachments) {
      if (!attachment.isLocal()) {
        DownloadedFile dfile = attachment.getDownloadedFile(controller.getDownloadStatus());
        if (AttachmentsControllerUtil.isDownloadNeeded(dfile)) {
          AttachmentsControllerUtil.initiateDownload(controller.getDownloadStatus().getDownloadManager(), attachment);
        }
      }
    }
  }
}
