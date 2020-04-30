package com.almworks.download;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.io.persist.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class DownloadEntryKey<T> extends TypedKey<T> {
  private static List<DownloadEntryKey> myKeys;
  private static List<DownloadEntryKey> myKeysReadonly;
  public static final DownloadEntryKey<File> FILE = create("FILE", new PersistableFile());
  public static final DownloadEntryKey<String> LAST_DOWNLOAD_ERROR = create("LAST_DOWNLOAD_ERROR", new PersistableString());
  public static final DownloadEntryKey<Long> SIZE = create("SIZE", new PersistableLong());
  public static final DownloadEntryKey<DownloadedFile.State> STATE = create("STATE", new PersistableEnumerable<DownloadedFile.State>(DownloadedFile.State.class));
  public static final DownloadEntryKey<String> MIME_TYPE = create("MIME_TYPE", new PersistableString());
  public static final DownloadEntryKey<Long> LAST_MODIFIED = create("LAST_MODIFIED", new PersistableLong());

  private final LeafPersistable<T> myPersistable;

  private DownloadEntryKey(String name, LeafPersistable<T> persistable) {
    super(name, null, null);
    synchronized(DownloadEntryKey.class) {
      if (myKeys == null) {
        myKeys = Collections15.arrayList();
        myKeysReadonly = Collections.unmodifiableList(myKeys);
      }
      myKeys.add(this);
    }
    myPersistable = persistable;
  }

  public LeafPersistable<T> getPersistable() {
    return myPersistable;
  }

  private static <T> DownloadEntryKey<T> create(String key, LeafPersistable<T> persistable) {
    return new DownloadEntryKey<T>(key, persistable);
  }

  static List<DownloadEntryKey> all() {
    return myKeysReadonly;
  }
}
