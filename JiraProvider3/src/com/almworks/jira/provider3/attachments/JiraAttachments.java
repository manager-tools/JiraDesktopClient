package com.almworks.jira.provider3.attachments;

import com.almworks.api.actions.AttachScreenshotAction;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.config.MiscConfig;
import com.almworks.api.download.DownloadedFile;
import com.almworks.engine.gui.attachments.AttachmentChooserOpen;
import com.almworks.engine.gui.attachments.AttachmentTooltipProvider;
import com.almworks.items.gui.edit.engineactions.NewItemAction;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.itemsync.MergeOperationsManager;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.Attachment;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.List;

import static com.almworks.engine.gui.attachments.AttachmentProperty.*;

public class JiraAttachments {
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(JiraAttachments.class.getClassLoader(), "com/almworks/jira/provider3/attachments/message");

  public static final AnAction ATTACH_FILE = new AttachFileAction();
  public static final AttachScreenshotAction ATTACH_SCREEN_SHOT = new AttachScreenshotAction() {
    @Override
    protected void makeAttach(long issue, File file, String attachmentName, EditDrain drain, String mimeType) {
      AttachEditFeature.createAttachment(drain.changeItem(issue), file, attachmentName, mimeType);
    }
  };
  public static final AnAction ATTACH_TEXT = new AttachTextAction();
  public static final AnAction DELETE_ATTACHMENTS = new DeleteAttachmentAction();
  public static final AnAction DOWNLOAD_ATTACHMENTS = new DownloadAttachmentsAction();

  public static final ScalarSequence MK_DATA_LOADER = AttachmentImplLoader.DESCRIPTOR.getSerializable();

  public static void registerFeature(FeatureRegistry registry) {
    AttachmentImplLoader.DESCRIPTOR.registerFeature(registry);
  }

  public static void setupMerge(MergeOperationsManager manager) {
    manager.buildOperation(Attachment.DB_TYPE).discardEdit(Attachment.LOCAL_FILE).finish();
  }

  private static final AttachmentTooltipProvider<com.almworks.engine.gui.attachments.Attachment> ATTACHMENT_TOOLTIP_PROVIDER
    = new AttachmentTooltipProvider<com.almworks.engine.gui.attachments.Attachment>() {
    @Override
    public void addTooltipText(StringBuilder tooltip, com.almworks.engine.gui.attachments.Attachment item, DownloadedFile dfile) {
      append(tooltip, item, dfile, FILE_NAME, "<b>", "</b>");
      appendSize(tooltip, item, dfile);
      append(tooltip, item, dfile, USER, "Attached By: ", null);
      append(tooltip, item, dfile, DATE, null, null);
    }
  };
  public static AttachmentsFormlet2<?> createFormlet(GuiFeaturesManager features, Configuration config, MiscConfig globalConfig) {
    LoadedModelKey<List<AttachmentImpl>> attachKey =
      features.findListModelKey(MetaSchema.KEY_ATTACHMENTS_LIST, AttachmentImpl.class);
    AttachmentsFormlet2<AttachmentImpl> formlet =
      new AttachmentsFormlet2<AttachmentImpl>(config, attachKey, globalConfig, AttachmentImpl.ROLE);
    formlet.setOrder(AttachmentImpl.ORDER)
      .addProperties(FILE_NAME, MIME_TYPE, DATE, USER, SIZE)
      .setLabelProperty(FILE_NAME)
      .setTooltipProvider(ATTACHMENT_TOOLTIP_PROVIDER);
    return formlet;
  }

  public static void attachFiles(ActionContext context, ItemWrapper issue, @Nullable List<File> fileList) throws CantPerformException {

    String key = Util.NN(MetaSchema.issueKey(issue), "New Issue");
    File[] files;
    if (fileList != null) {
      files = fileList.toArray(new File[0]);
    } else {
      JiraConnection3 connection = CantPerformException.ensureNotNull(issue.services().getConnection(JiraConnection3.class));
      Configuration config = connection.getConnectionConfig("attachments", "attachFile");
      files = AttachmentChooserOpen.show(context.getComponent(), config, true, 0);
    }
    files = CantPerformException.ensureNotEmpty(files);
    AttachEditFeature feature = new AttachEditFeature(key, files, issue.getItem(), issue.getConnection());
    EditDescriptor descriptor = feature.checkContext(context, new UpdateRequest(Updatable.NEVER, context));
    NewItemAction.peform(context, feature, descriptor);
  }
  
  public static void attachImage(ActionContext context, final ItemWrapper issue, Image image) throws CantPerformException {
    ATTACH_SCREEN_SHOT.attachImage(context, issue, image);
  }
}
