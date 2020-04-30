package com.almworks.engine.gui.attachments;

import org.almworks.util.TypedKey;

import java.io.File;

public class FileData {
  public static final TypedKey<FileData> FILE_DATA = TypedKey.create(FileData.class);

  private final byte[] myData;
  private final String myMimeType;
  private final File myFile;

  public FileData(File file, String mimeType, byte[] bytes) {
    myFile = file;
    myMimeType = mimeType;
    myData = bytes;
  }

  public byte[] getBytesInternal() {
    return myData;
  }

  public String getMimeType() {
    return myMimeType;
  }

  public File getFile() {
    return myFile;
  }

  public String getFileName() {
    return myFile.getName();
  }
}
