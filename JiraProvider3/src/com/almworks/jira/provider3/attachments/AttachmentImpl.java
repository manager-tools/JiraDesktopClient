package com.almworks.jira.provider3.attachments;

import com.almworks.api.application.UiItem;
import com.almworks.api.download.DownloadOwner;
import com.almworks.api.download.DownloadedFile;
import com.almworks.download.DownloadOwnerResolverImpl;
import com.almworks.engine.gui.attachments.Attachment;
import com.almworks.engine.gui.attachments.AttachmentDownloadStatus;
import com.almworks.engine.gui.attachments.AttachmentsControllerUtil;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.files.FileUtil;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

import static com.almworks.api.download.DownloadedFile.State.READY;

public class AttachmentImpl extends Attachment implements UiItem{

  public static DataRole<AttachmentImpl> ROLE = DataRole.createRole(AttachmentImpl.class);

  private final GuiFeaturesManager myManager;
  private long myItem;
  private final long myAuthor;
  private final String myUrl;
  private String myName;
  private final String myMimeType;
  private final String mySizeText;
  private final File myLocalFile;
  private final String myDateText;
  private final Date myDate;

  private long myCacheSize = -1;
  private final DownloadOwner myDownloadOwner;

  private AttachmentImpl(GuiFeaturesManager manager, long item, String url, String name, String mimeType, long author,
    String dateText, Date date, String sizeText, File localFile, DownloadOwner downloadOwner)
  {
    myManager = manager;
    myItem = item;
    myUrl = url;
    myName = name;
    myMimeType = mimeType;
    myAuthor = author;
    myDateText = dateText;
    myDate = date;
    mySizeText = sizeText;
    myLocalFile = localFile;
    myDownloadOwner = downloadOwner;
  }

  static AttachmentImpl load(ItemVersion slaveVersion, GuiFeaturesManager features, JiraConnection3 connection) {
    String url = slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.FILE_URL);
    String attachmentName = slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.ATTACHMENT_NAME);
    String mimeType = slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.MIME_TYPE);
    String dateStr = slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.DATE_STRING);
    Date date = slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.DATE);
    String size = slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.SIZE_STRING);
    String localFile = slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.LOCAL_FILE);
    File file;
    if (localFile != null) {
      file = new File(localFile);
      file = AttachmentsControllerUtil.isGoodFile(file) ? file : null;
    } else file = null;
    long author = Util.NN(slaveVersion.getValue(com.almworks.jira.provider3.schema.Attachment.AUTHOR), 0l);
    DownloadOwner downloadOwner;
    if (connection != null) downloadOwner = connection.getDownloadOwner();
    else {
      LogHelper.error("Missing attachment connection", slaveVersion, url, attachmentName);
      downloadOwner = DownloadOwnerResolverImpl.DisabledOwner.INSTANCE;
    }
    return new AttachmentImpl(features, slaveVersion.getItem(), url, attachmentName, mimeType, author, dateStr, date, size, file, downloadOwner);
  }

  public long getAuthor() {
    return myAuthor;
  }

  @NotNull
  @Override
  public DownloadOwner getDownloadOwner() {
    return myDownloadOwner;
  }

  @Override
  public String getDownloadArgument() {
    return getUrl();
  }

  @Override
  public String getUrl() {
    return myUrl;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void setName(String name) {
    LogHelper.assertError(myUrl == null, "Setting attachment name to uploaded file");
    myName = name;
  }

  @Override
  public long getExpectedSize() {
    if (myCacheSize < 0) myCacheSize = FileUtil.getSizeFromString(getExpectedSizeText());
    return myCacheSize;
  }

  @Override
  public boolean isLocal() {
    return myLocalFile != null;
  }

  @Override
  public String getMimeType(AttachmentDownloadStatus<? extends Attachment> downloadStatus) {
    if (myMimeType != null) return myMimeType;
    DownloadedFile file = getDownloadedFile(downloadStatus);
    String mime = file != null ? file.getMimeType() : null;
    if (mime != null) return mime;
    return FileUtil.guessMimeType(getName());
  }

  @Override
  public String getExpectedSizeText() {
    return mySizeText;
  }

  @Override
  public String getUser() {
    return LoadedIssueUtil.getUserDisplayName(myManager, myAuthor);
  }

  @Override
  public String getDateString() {
    return myDateText;
  }

  @Override
  public Date getDate() {
    return myDate;
  }

  @Override
  public long getItem() {
    return myItem;
  }

  @Override
  public long getOrderForSorting() {
    return myDate != null ? myDate.getTime() : (Long.MAX_VALUE / 2 + myItem);
  }

  public File getLocalFile(@Nullable AttachmentDownloadStatus<? extends Attachment> downloadStatus) {
    File file;
    if (myLocalFile != null) file = myLocalFile;
    else if (downloadStatus != null) {
      DownloadedFile dfile = getDownloadedFile(downloadStatus);
      file = dfile != null && dfile.getState() == READY ? dfile.getFile() : null;
    } else return null;
    return AttachmentsControllerUtil.isGoodFile(file) ? file : null;
  }
}
