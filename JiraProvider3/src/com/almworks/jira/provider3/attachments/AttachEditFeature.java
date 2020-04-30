package com.almworks.jira.provider3.attachments;

import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.ListMessage;
import com.almworks.items.gui.edit.editors.MockEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.comments.gui.BaseEditComment;
import com.almworks.jira.provider3.schema.Attachment;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

class AttachEditFeature implements EditFeature {
  private final File[] myFiles;
  private final long myItem;
  private final String myIssueKey;
  private final Connection myConnection;
  private final ListMessage myFilesMessage;
  private final AttachFilesEditor myAttachFiles;

  AttachEditFeature(String issueKey, File[] files, long item, Connection connection) {
    myIssueKey = issueKey;
    myFiles = files;
    myItem = item;
    myConnection = connection;
    myFilesMessage = ListMessage.create(NameMnemonic.rawText("Files"), Arrays.asList(files));
    myAttachFiles = new AttachFilesEditor(files);
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) {
    String title = "Attach File" + (myFiles.length > 1 ? "s" : "") + " to " + myIssueKey;
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("attachFiles", title, null);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setDescriptionStrings(
      title,
      "New attachments were saved in the local database.",
      "Save new attachments in the local database without uploading to server",
      "Save new attachments and upload them to server");
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(LongArray.create(myItem));
    EngineConsts.setupConnection(model, myConnection);
    return model;
  }

  @Override
  public void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
    BranchSource source = BranchSource.trunk(reader);
    BaseEditComment.COMMENT_SLAVE.prepareModel(source, model, editPrepare);
    myFilesMessage.prepareModel(source, model, editPrepare);
    myAttachFiles.prepareModel(source, model, editPrepare);
  }

  @Override
  public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
    return VerticalLinePlacement.buildTopComponent(life, model, myFilesMessage, BaseEditComment.COMMENT_SLAVE);
  }

  public static void createAttachment(ItemVersionCreator issue, File file, String attachmentName, @Nullable String mimeType) {
    if (file == null || !file.isFile() || !file.canRead()) return;
    ItemVersionCreator attachment = issue.createItem();
    attachment.setValue(DBAttribute.TYPE, Attachment.DB_TYPE);
    attachment.setValue(SyncAttributes.CONNECTION, issue.getValue(SyncAttributes.CONNECTION));
    attachment.setValue(Attachment.ISSUE, issue);
    attachment.setValue(Attachment.ATTACHMENT_NAME, attachmentName);
    attachment.setValue(Attachment.LOCAL_FILE, file.getAbsolutePath());
    attachment.setValue(Attachment.SIZE_STRING, String.valueOf(file.length()));
    if (mimeType != null) attachment.setValue(Attachment.MIME_TYPE, mimeType);
  }

  public static void createAttachment(SyncManager syncManager, final long issue, final File file, final String attachmentName, final String mimeType) {
    syncManager.commitEdit(new EditCommit() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        AttachEditFeature.createAttachment(drain.changeItem(issue), file, attachmentName, mimeType);
      }

      @Override
      public void onCommitFinished(boolean success) {
      }
    });
  }

  private static class AttachFilesEditor extends MockEditor {
    private final File[] myFiles;

    private AttachFilesEditor(File[] files) {
      myFiles = files;
    }

    @Override
    public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
      model.registerEditor(this);
    }

    @Override
    public boolean hasDataToCommit(EditItemModel model) {
      return true;
    }

    @Override
    public boolean hasValue(EditModelState model) {
      return true;
    }

    @Override
    public void commit(CommitContext context) {
      for (File file : myFiles) createAttachment(context.getCreator(), file, file.getName(), null);
    }
  }
}
