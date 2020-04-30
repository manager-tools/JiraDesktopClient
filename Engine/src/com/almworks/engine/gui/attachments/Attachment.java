package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadOwner;
import com.almworks.api.download.DownloadRequest;
import com.almworks.api.download.DownloadedFile;
import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.files.FileUtil;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Comparator;
import java.util.Date;

public abstract class Attachment {
  public static final Condition<Attachment> LOCAL = new Condition<Attachment>() {
    @Override
    public boolean isAccepted(Attachment value) {
      return value != null && value.isLocal();
    }
  };

  public static final DataRole<Attachment> ROLE = DataRole.createRole(Attachment.class);

  public static final Comparator<Attachment> ORDER = new Comparator<Attachment>() {
    public int compare(Attachment o1, Attachment o2) {
      return Containers.compareLongs(o1.getOrderForSorting(), o2.getOrderForSorting());
    }
  };

  public static final String FORBIDDEN_CHARS_MSG = "Attachment name cannot contain any of the following symbols: " +
    FileUtil.FORBIDDEN_CHARS_DISPLAYABLE;
  private static final String UNNAMED = "<Unnamed>";

  @NotNull
  public abstract DownloadOwner getDownloadOwner();

  @Nullable
  public abstract String getDownloadArgument();

  @Nullable
  public abstract String getUrl();

  @Nullable
  public abstract String getName();

  public String getDisplayName() {
    String name = getName();
    return name == null || name.length() == 0 ? UNNAMED : name;
  }

  public abstract void setName(String name);

  public abstract long getExpectedSize();


  public abstract boolean isLocal();

  @Nullable
  public abstract String getMimeType(@Nullable AttachmentDownloadStatus<? extends Attachment> downloadStatus);

  @Nullable
  public abstract String getExpectedSizeText();

  public abstract String getUser();

  public abstract String getDateString();

  @Nullable
  public abstract Date getDate();

  public DownloadRequest createDownloadRequest() {
    String filename = Util.NN(getName(), "attachment.dat");
    return new DownloadRequest(getDownloadOwner(), getDownloadArgument(), filename, getExpectedSize());
  }

  public DownloadedFile getDownloadedFile(AttachmentDownloadStatus<? extends Attachment> status) {
    if (status == null) return null;
    String url = getUrl();
    if (url == null) return null;
    return status.getDownloadedFile(url);
  }

  public abstract File getLocalFile(@Nullable AttachmentDownloadStatus<? extends Attachment> downloadStatus);

  public abstract long getOrderForSorting();
}
