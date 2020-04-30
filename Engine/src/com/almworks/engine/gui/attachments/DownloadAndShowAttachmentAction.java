package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.ui.actions.*;

import java.io.File;
import java.util.List;

import static com.almworks.api.download.DownloadedFile.State.READY;

class DownloadAndShowAttachmentAction extends AbstractAttachmentAction {
  private final AttachmentShowStrategy myShowStrategy;

  public static final AnAction DEFAULT_INSTANCE = new DownloadAndShowAttachmentAction(AttachmentShowStrategy.DEFAULT);
  public static final AnAction VIEW_INSTANCE = new DownloadAndShowAttachmentAction(AttachmentShowStrategy.VIEW);
  public static final AnAction OPEN_INSTANCE = new DownloadAndShowAttachmentAction(AttachmentShowStrategy.OPEN);

  private DownloadAndShowAttachmentAction(AttachmentShowStrategy strat) {
    super(strat.getName(1));
    myShowStrategy = strat;
    watchRole(Attachment.ROLE);
    watchRole(AttachmentsController.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<Attachment> attachments = context.getSourceCollection(Attachment.ROLE);
    boolean download = false;
    int size = attachments.size();
    boolean enabled = false;
    if (size > 0) {
      AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
      for (Attachment attachment : attachments) {
        File file = attachment.getLocalFile(controller.getDownloadStatus());
        if (file != null) enabled = true;
        else if (!attachment.isLocal()) {
          DownloadedFile dfile = attachment.getDownloadedFile(controller.getDownloadStatus());
          if (AttachmentsControllerUtil.isDownloadNeeded(dfile)) {
            download = true;
            enabled = true;
          } else if (dfile != null) {
            if (dfile.getState() != READY || AttachmentsControllerUtil.isGoodFile(dfile.getFile())) enabled = true;
          }
        }
      }
    }
    context.setEnabled(enabled);
    String name = myShowStrategy.getName(size);
    if (download)
      name = "Download and " + name;
    context.putPresentationProperty(PresentationKey.NAME, name);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<Attachment> attachments = context.getSourceCollection(Attachment.ROLE);
    AttachmentsController controller = context.getSourceObject(AttachmentsController.ROLE);
    for (Attachment attachment : attachments) {
      controller.showAttachment(attachment, context.getComponent(), myShowStrategy);
    }
  }
}
