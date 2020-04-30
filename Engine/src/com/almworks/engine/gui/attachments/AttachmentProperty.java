package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.files.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Comparator;
import java.util.Date;

import static com.almworks.api.download.DownloadedFile.State.READY;

public abstract class AttachmentProperty<T extends Attachment, V> implements Comparator<V> {
  public static final AttachmentProperty<Attachment, String> FILE_NAME = new FileNameProperty();
  public static final AttachmentProperty<Attachment, Date> DATE = new DateProperty();
  public static final AttachmentProperty<Attachment, String> USER = new UserProperty();
  public static final AttachmentProperty<Attachment, Pair<String, Long>> SIZE = new SizeAttachmentProperty();
  public static final AttachmentProperty<Attachment, String> MIME_TYPE = new MimeTypeProperty();

  private final String myName;

  protected AttachmentProperty(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public int compare(V value1, V value2) {
    if (value1 == null) {
      if (value2 == null) {
        return 0;
      } else {
        return 1;
      }
    } else {
      if (value2 == null) {
        return -1;
      } else {
        return compareNotNull(value1, value2);
      }
    }
  }

  protected int compareNotNull(V value1, V value2) {
    if (!value1.getClass().equals(value2.getClass())) {
      assert false : value1 + " " + value2;
      return Containers.compareInts(value1.getClass().hashCode(), value2.getClass().hashCode());
    } else {
      if (value1 instanceof Comparable) {
        return ((Comparable) value1).compareTo(value2);
      } else {
        assert false : value1 + " " + value2;
        return 0;
      }
    }
  }

  @Nullable
  public abstract V getColumnValue(@NotNull T attachment, @Nullable DownloadedFile downloadedFile);

  public abstract String getStringValue(@NotNull T attachment, DownloadedFile downloadedFile);


  private static class SizeAttachmentProperty extends AttachmentProperty<Attachment, Pair<String, Long>> {
    public SizeAttachmentProperty() {
      super("Size");
    }

    public Pair<String, Long> getColumnValue(@NotNull Attachment attachment, @Nullable DownloadedFile downloadedFile) {
      long sizeLong = attachment.getExpectedSize();
      if(downloadedFile != null && (sizeLong <= 0 || downloadedFile.getState() == READY)) {
        sizeLong = downloadedFile.getSize();
      }
      final String sizeText = FileUtil.getSizeString(sizeLong);
      return Pair.create(sizeText, sizeLong);
    }

    public String getStringValue(@NotNull Attachment attachment, DownloadedFile downloadedFile) {
      Pair<String, Long> pair = getColumnValue(attachment, downloadedFile);
      return pair != null ? pair.getFirst() : "";
    }

    public int compareNotNull(Pair<String, Long> value1, Pair<String, Long> value2) {
      Long size1 = value1.getSecond();
      Long size2 = value2.getSecond();
      if (size1 == null || size1 <= 0) {
        if (size2 == null || size2 < 0) {
          return 0;
        } else {
          return 1;
        }
      } else {
        if (size2 == null || size2 < 0) {
          return -1;
        } else {
          return Containers.compareLongs(size1, size2);
        }
      }
    }
  }


  private static class FileNameProperty extends AttachmentProperty<Attachment, String> {
    public FileNameProperty() {
      super("File Name");
    }

    public String getColumnValue(Attachment attachment, DownloadedFile downloadedFile) {
      String fileName = attachment.getDisplayName();
      if (fileName != null) return fileName;
      if (downloadedFile != null) {
        File file = downloadedFile.getFile();
        if (file != null) return file.getName();
      }
      return null;
    }

    public String getStringValue(@NotNull Attachment attachment, DownloadedFile downloadedFile) {
      return getColumnValue(attachment, downloadedFile);
    }
  }


  private static class DateProperty extends AttachmentProperty<Attachment, Date> {
    public DateProperty() {
      super("Date");
    }

    public Date getColumnValue(@NotNull Attachment attachment, @Nullable DownloadedFile downloadedFile) {
      return attachment.getDate();
    }

    public String getStringValue(@NotNull Attachment attachment, DownloadedFile downloadedFile) {
      final Date date = attachment.getDate();
      return date != null ? DateUtil.toLocalDateTime(date) : attachment.getDateString();
    }
  }


  private static class UserProperty extends AttachmentProperty {
    public UserProperty() {
      super("User");
    }

    public Object getColumnValue(@NotNull Attachment attachment, @Nullable DownloadedFile downloadedFile) {
      return attachment.getUser();
    }

    public String getStringValue(@NotNull Attachment attachment, DownloadedFile downloadedFile) {
      return attachment.getUser();
    }
  }


  private static class MimeTypeProperty extends AttachmentProperty<Attachment, String> {
    public MimeTypeProperty() {
      super("Type");
    }

    public String getColumnValue(@NotNull Attachment attachment, @Nullable DownloadedFile downloadedFile) {
      String type = attachment.getMimeType(null);
      if (downloadedFile != null && downloadedFile.getState() == READY) {
        type = downloadedFile.getMimeType();
      }
      return type;
    }

    public String getStringValue(@NotNull Attachment attachment, DownloadedFile downloadedFile) {
      return getColumnValue(attachment, downloadedFile);
    }
  }
}
