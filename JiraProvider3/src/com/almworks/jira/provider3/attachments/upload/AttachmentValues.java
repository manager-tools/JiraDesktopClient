package com.almworks.jira.provider3.attachments.upload;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.SlaveValues;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.schema.Attachment;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.sync.schema.ServerAttachment;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.util.LogHelper;
import com.almworks.util.files.FileUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

class AttachmentValues extends SlaveValues {
  private final File myFile;
  private final String myAttachmentName;
  private final String myMimeType;
  private final String myAuthorName;

  private AttachmentValues(@Nullable Integer id, File file, String attachmentName, String mimeType, String authorName) {
    super(id);
    myFile = file;
    myMimeType = mimeType;
    myAuthorName = authorName;
    myAttachmentName = Util.NN(attachmentName, "attachment.dat");
  }

  @Override
  public boolean matchesFailure(EntityHolder slave, @NotNull Entity thisUser) {
    return ServerUser.sameUser(thisUser, slave.getReference(ServerAttachment.AUTHOR))
      && myFile.getName().equalsIgnoreCase(slave.getScalarValue(ServerAttachment.FILE_NAME));
  }

  public static AttachmentValues load(ItemVersion attachment) {
    String localFileName = attachment.getValue(Attachment.LOCAL_FILE);
    File source = localFileName == null ? null : new File(localFileName);
    String attachmentName = attachment.getValue(Attachment.ATTACHMENT_NAME);
    String mimeType = attachment.getValue(Attachment.MIME_TYPE);
    ItemVersion author = attachment.readValue(Attachment.AUTHOR);
    String authorName;
    if (author != null) {
      authorName = author.getValue(User.NAME);
      if (attachmentName == null || attachmentName.isEmpty()) attachmentName = User.getDisplayName(author);
    } else authorName = null;
    if (mimeType == null) {
      LogHelper.warning("Missing mime-type");
      mimeType = FileUtil.guessMimeType(attachmentName);
      if (mimeType == null) LogHelper.warning("Failed to guess mime-type", attachmentName);
    }
    return new AttachmentValues(attachment.getValue(Attachment.ID), source, attachmentName, mimeType, authorName);
  }

  public String getAttachmentName() {
    return myAttachmentName;
  }

  public File getFile() {
    return myFile;
  }

  public String getMimeType() {
    return myMimeType;
  }

  @NotNull
  public String getAuthorName() {
    return myAuthorName == null || myAuthorName.isEmpty() ? "<Unknown>" : myAuthorName;
  }

  @Nullable("When not found")
  public EntityHolder find(EntityTransaction transaction, CreateIssueUnit createIssue) {
    EntityHolder issue = createIssue.findIssue(transaction);
    Integer id = getId();
    return issue != null && id != null ? ServerAttachment.find(issue, id) : null;
  }
}
