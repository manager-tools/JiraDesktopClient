package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import org.almworks.util.Collections15;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

public class StoreIndex {
  private final List<FileInfo> myFiles;
  private final List<FileInfo> myFilesReadonly;
  private final SortedMap<String, EntryInfo> myEntries;
  private final SortedMap<String, EntryInfo> myEntriesReadonly;

  public StoreIndex(List<FileInfo> files, SortedMap<String, EntryInfo> entries) {
    myFiles = files != null ? files : Collections15.<FileInfo>arrayList();
    myFilesReadonly = Collections.unmodifiableList(myFiles);
    myEntries = entries != null ? entries : Collections15.<String, EntryInfo>treeMap();
    myEntriesReadonly = Collections.unmodifiableSortedMap(myEntries);
  }

  public StoreIndex() {
    this(null, null);
  }

  public List<FileInfo> getFiles() {
    return myFilesReadonly;
  }

  public SortedMap<String, EntryInfo> getEntries() {
    return myEntriesReadonly;
  }

  public void addFileInfo(FileInfo fileInfo) {
    myFiles.add(fileInfo);
  }

  public void addEntryInfo(EntryInfo entryInfo) {
    myEntries.put(entryInfo.getStorePath(), entryInfo);
  }

  public void replaceFileInfo(int index, FileInfo fileInfo) {
    if (fileInfo == null)
      throw new NullPointerException("fileInfo");
    myFiles.set(index, fileInfo);
  }

  public void removeEntry(String id) {
    EntryInfo entryInfo = myEntries.remove(id);
    assert entryInfo != null;
  }


  public static class FileInfo {
    private final String myFileName; // without path
    private final long myLastSize;
    private final long myLastModificationTime;
    private final int myBlockSize;
    private final StoreFeature[] myFeatures;
    private final int myFormatVersion;

    public FileInfo(String fileName, long lastSize, long lastModificationTime, int blockSize, StoreFeature[] features,
      int formatVersion) {
      myFileName = fileName;
      myLastSize = lastSize;
      myLastModificationTime = lastModificationTime;
      myBlockSize = blockSize;
      myFeatures = features;
      myFormatVersion = formatVersion;
    }

    public FileInfo(StoreDataFile dataFile) {
      dataFile.flush();
      File file = dataFile.getPath();
      myFileName = file.getName();
      myLastSize = file.length();
      myLastModificationTime = file.lastModified();
      myBlockSize = dataFile.getBlockSize();
      myFeatures = dataFile.getFileFeatures();
      myFormatVersion = dataFile.getFormatVersion();
    }

    public String getFileName() {
      return myFileName;
    }

    public long getLastSize() {
      return myLastSize;
    }

    public long getLastModificationTime() {
      return myLastModificationTime;
    }

    public int getBlockSize() {
      return myBlockSize;
    }

    public StoreFeature[] getFeatures() {
      return myFeatures;
    }

    public int hashCode() {
      int hashCode = (((myFileName.hashCode() * 23 + new Long(myLastSize).hashCode()) * 23 +
        new Long(myLastModificationTime).hashCode()) * 23 + myBlockSize);
      for (int i = 0; i < myFeatures.length; i++) {
        hashCode = hashCode * 23 + myFeatures[i].hashCode();
      }
      hashCode = hashCode * 23 + myFormatVersion;
      return hashCode;
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof FileInfo))
        return false;
      FileInfo that = (FileInfo) obj;
      return myFileName.equals(that.myFileName)
        && myBlockSize == that.myBlockSize
        && Arrays.equals(myFeatures, that.myFeatures)
        && myLastModificationTime == that.myLastModificationTime
        && myLastSize == that.myLastSize
        && myFormatVersion == that.myFormatVersion;
    }

    public int getFormatVersion() {
      return myFormatVersion;
    }
  }

  public static class EntryInfo {
    private final String myStorePath; // also a key in the map
    private final int myFileIndex; // to look up in myFiles
    private final int myBlockId;  // in the file

    public EntryInfo(String storePath, int fileIndex, int blockId) {
      myStorePath = storePath;
      myFileIndex = fileIndex;
      myBlockId = blockId;
    }

    public String getStorePath() {
      return myStorePath;
    }

    public int getFileIndex() {
      return myFileIndex;
    }

    public int getBlockID() {
      return myBlockId;
    }

    public int hashCode() {
      return ((myStorePath.hashCode() * 23) + myFileIndex) * 23 + myBlockId;
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof EntryInfo))
        return false;
      EntryInfo that = (EntryInfo) obj;
      return myStorePath.equals(that.myStorePath)
        && myFileIndex == that.myFileIndex
        && myBlockId == that.myBlockId;
    }
  }
}
