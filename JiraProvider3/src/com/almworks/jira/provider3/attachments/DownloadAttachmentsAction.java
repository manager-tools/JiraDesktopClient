package com.almworks.jira.provider3.attachments;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.download.DownloadManager;
import com.almworks.api.download.DownloadedFile;
import com.almworks.engine.gui.attachments.Attachment;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.util.Terms;
import com.almworks.util.ui.actions.*;

import java.util.Collection;
import java.util.List;

import static com.almworks.api.download.DownloadedFile.State.*;

class DownloadAttachmentsAction extends SimpleAction {
  public DownloadAttachmentsAction() {
    super("Down&load Attachments");
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Download all attachments for selected " + Terms.ref_artifacts);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = ItemActionUtils.basicUpdate(context, false);
    LoadedModelKey<List<Attachment>> attachmentsKey = getAttachmentsKey(context);
    DownloadManager downloadManager = context.getSourceObject(DownloadManager.ROLE);
    boolean enabled = false;
    for (ItemWrapper wrapper : wrappers) {
      Collection<? extends Attachment> attachments = wrapper.getModelKeyValue(attachmentsKey);
      if (attachments != null && attachments.size() > 0) {
        for (Attachment attachment : attachments) {
          String url = attachment.getUrl();
          if (url != null) {
            DownloadedFile.State state = downloadManager.getDownloadStatus(url).getState();
            if (needsDownload(state)) {
              enabled = true;
              break;
            }
          }
        }
      }
    }
    context.setEnabled(enabled);
  }

  private static LoadedModelKey<List<Attachment>> getAttachmentsKey(ActionContext context) throws CantPerformException {
    // This is needed to prevent from updating before model keys are created. This is actually harmless, but generates error log messages
    CantPerformException.ensureNotEmpty(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    return context.getSourceObject(GuiFeaturesManager.ROLE).findListModelKey(MetaSchema.KEY_ATTACHMENTS_LIST, Attachment.class);
  }

  private static boolean needsDownload(DownloadedFile.State state) {
    return state != DOWNLOADING && state != QUEUED && state != READY;
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    LoadedModelKey<List<Attachment>> attachmentsKey = getAttachmentsKey(context);
    DownloadManager downloadManager = context.getSourceObject(DownloadManager.ROLE);
    for (ItemWrapper wrapper : wrappers) {
      Collection<? extends Attachment> attachments = wrapper.getModelKeyValue(attachmentsKey);
      if (attachments != null && attachments.size() > 0) {
        for (Attachment aa : attachments) {
          String url = aa.getUrl();
          if (url != null) {
            DownloadedFile.State state = downloadManager.getDownloadStatus(url).getState();
            if (needsDownload(state)) {
              downloadManager.initiateDownload(url, aa.createDownloadRequest(), false, false);
            }
          }
        }
      }
    }
  }
}
