package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.English;
import com.almworks.util.ui.actions.*;

import java.util.List;

class DownloadAttachmentsAction extends AbstractAttachmentAction {
  public static final AnAction INSTANCE = new DownloadAttachmentsAction();
  
  private DownloadAttachmentsAction() {
    super("Download Attachments");
    watchRole(Attachment.ROLE);
    watchRole(AttachmentsController.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<Attachment> attachments = context.getSourceCollection(Attachment.ROLE);
    AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
    boolean enabled = false;
    boolean needed = false;
    int size = attachments.size();
    if (size > 0) {
      for (Attachment attachment : attachments) {
        if (!attachment.isLocal()) {
          enabled = true;
          DownloadedFile dfile = attachment.getDownloadedFile(controller.getDownloadStatus());
          if (AttachmentsControllerUtil.isDownloadNeeded(dfile)) needed = true;
        }
      }
    }
    context.setEnabled(enabled);
    String name = (needed || size == 0 ? "Download " : "Re-download ") + English.getSingularOrPlural("Attachment", size);
    context.putPresentationProperty(PresentationKey.NAME, name);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<Attachment> attachments = context.getSourceCollection(Attachment.ROLE);
    AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
    for (Attachment attachment : attachments) {
      if (!attachment.isLocal()) {
        AttachmentsControllerUtil.initiateDownload(controller.getDownloadStatus().getDownloadManager(), attachment);
      }
    }
  }
}
