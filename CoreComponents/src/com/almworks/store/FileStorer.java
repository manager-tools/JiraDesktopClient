package com.almworks.store;

import com.almworks.api.misc.WorkArea;
import com.almworks.api.store.StoreFeature;
import com.almworks.util.Pair;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.files.FileUtil;
import com.almworks.util.threads.Threads;
import org.almworks.util.*;
import org.jetbrains.annotations.Nullable;
import util.external.UID;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FileStorer implements Storer {
  public static final String INDEX_FILE_NAME = ".index";
  private static final float BLOCK_RATIO = 1.5F;
  private static final int LOAD_ATTEMPTS = 3;
  private static final int INDEX_WRITE_ATTEMPTS = 5;

  private final WorkArea myWorkArea;
  private StoreIndex myIndex = null;
  private final SDFFormat myDefaultFormat;

  public FileStorer(WorkArea workArea) {
    this(workArea, null);
  }

  public FileStorer(WorkArea workArea, SDFFormat defaultFormat) {
    if (workArea == null)
      throw new NullPointerException("workArea");
    myWorkArea = workArea;
    myDefaultFormat = defaultFormat;
  }

  @Override
  public boolean isSupported(StoreFeature[] features) {
    for (int i = 0; i < features.length; i++) {
      StoreFeature feature = features[i];
      if (!isSupported(feature))
        return false;
    }
    return true;
  }

  public synchronized void store(String id, byte[] buffer, StoreFeature[] features) throws IOException {
    Threads.assertLongOperationsAllowed();
    clear(id);
    checkStatus();
    int dataFileIndex = findFittingFile(id, buffer, features);
    int blockId = -1;
    try {
      while (blockId == -1) {
        try {
          StoreDataFile dataFile = new StoreDataFile(file(myIndex.getFiles().get(dataFileIndex).getFileName()));
          try {
            dataFile.open();
            blockId = dataFile.write(id, buffer, features);
            myIndex.replaceFileInfo(dataFileIndex, new StoreIndex.FileInfo(dataFile));
          } finally {
            dataFile.close();
          }
        } catch (StoreDataFile.Full full) {
          // todo
          throw new Failure(full);
        } catch (StoreDataFile.BlockOverflow blockOverflow) {
          Log.warn("cannot write to a presumably fitting storage, escalating to bigger storage", blockOverflow);
          dataFileIndex++;
          if (dataFileIndex >= myIndex.getFiles().size())
            throw new Failure(blockOverflow); // todo
        }
      }

      myIndex.addEntryInfo(new StoreIndex.EntryInfo(id, dataFileIndex, blockId));
      dumpIndex();
    } catch (FileFormatException e) {
      // todo
      throw new Failure(e);
    }
  }

  @Nullable
  public Collection<StoreIndex.EntryInfo> getAllEntries() throws IOException {
    checkStatus();
    if (myIndex == null) {
      Log.error("Load file storeIndex index failed");
      return null;
    }
    return myIndex.getEntries().values();
  }

  public void store(String id, byte[] buffer) throws IOException {
    store(id, buffer, StoreFeature.EMPTY_FEATURES_ARRAY);
  }

  @Override
  public synchronized byte[] load(String id, StoreFeature[] features) throws IOException, InterruptedException {
    Threads.assertLongOperationsAllowed();
    checkStatus();
    for (int attempt = 0; attempt < LOAD_ATTEMPTS; attempt++) {
      try {
        return doLoad(id);
      } catch (IndexRebuildNeeded e) {
        Log.warn("corrupt index, rebuilding");
        rebuildIndex();
      }
    }
    Log.warn("cannot rebuild index consecutively in " + LOAD_ATTEMPTS + " attempts for [" + id + "]");
    return null;
  }

  public synchronized StoreFeature[] getFeaures(String id) throws IOException, InterruptedException {
    checkStatus();
    try {
      return doGetFeatures(id);
    } catch (IndexRebuildNeeded indexRebuildNeeded) {
      rebuildIndex();
      try {
        return doGetFeatures(id);
      } catch (IndexRebuildNeeded e) {
        Log.error(e);
        return null;
      }
    }
  }

  private StoreFeature[] doGetFeatures(String id) throws IOException, IndexRebuildNeeded {
    StoreIndex.EntryInfo entryInfo = myIndex.getEntries().get(id);
    if (entryInfo == null) return null;
    assert entryInfo.getStorePath().equals(id);
    StoreIndex.FileInfo fileInfo = myIndex.getFiles().get(entryInfo.getFileIndex());
    File file = file(fileInfo.getFileName());
    StoreDataFile dataFile = new StoreDataFile(file);
    try {
      dataFile.open();
      return dataFile.getFeatures(entryInfo.getBlockID());
    } catch (FileFormatException e) {
      // todo
      throw new Failure(e);
    } catch (StoreDataFile.BlockIsFree e) {
      throw new IndexRebuildNeeded(e);
    } finally {
      dataFile.close();
    }
  }

  public byte[] load(String id) throws IOException, InterruptedException {
    return load(id, StoreFeature.EMPTY_FEATURES_ARRAY);
  }

  @Override
  public synchronized void clear(String id) throws IOException {
    Threads.assertLongOperationsAllowed();
    checkStatus();
    StoreIndex.EntryInfo entryInfo = myIndex.getEntries().get(id);
    if (entryInfo == null)
      return;
    if (!entryInfo.getStorePath().equals(id)) {
      assert false : id + " " + entryInfo.getStorePath();
      return;
    }

    int index = entryInfo.getFileIndex();
    List<StoreIndex.FileInfo> fileInfos = myIndex.getFiles();
    if (index < 0 || index >= fileInfos.size()) {
      assert false : id + ": " + index + " " + fileInfos.size();
      return;
    }
    StoreIndex.FileInfo fileInfo = fileInfos.get(index);

    File file = file(fileInfo.getFileName());
    StoreDataFile dataFile = new StoreDataFile(file);
    try {
      dataFile.open();
      dataFile.free(entryInfo.getBlockID());
      myIndex.replaceFileInfo(index, new StoreIndex.FileInfo(dataFile));
    } catch (FileFormatException e) {
      assert false : id + ": " + e;
      // ignore
    } catch (IOException e) {
      assert false : id + ": " + e;
      // ignore
    } catch (StoreDataFile.BlockIsFree e) {
      assert false : e;
    } finally {
      dataFile.close();
    }
    myIndex.removeEntry(id);
    dumpIndex();
  }

  private boolean isSupported(StoreFeature feature) {
    if (feature == StoreFeature.ENCRYPTED)
      return true;
    return false;
  }

  private void rebuildIndex() throws IOException, InterruptedException {
    if (myIndex != null) {
      myIndex = null;
    }
    File file = file(INDEX_FILE_NAME);
    FileUtil.deleteFile(file, false);
    assert !file.exists();
    checkStatus();
  }

  private byte[] doLoad(String id) throws IOException, IndexRebuildNeeded {
    StoreIndex.EntryInfo entryInfo = myIndex.getEntries().get(id);
    if (entryInfo == null)
      return null;
    assert entryInfo.getStorePath().equals(id);
    StoreIndex.FileInfo fileInfo = myIndex.getFiles().get(entryInfo.getFileIndex());
    File file = file(fileInfo.getFileName());
    StoreDataFile dataFile = new StoreDataFile(file);
    try {
      dataFile.open();
      Pair<String, byte[]> pair = dataFile.read(entryInfo.getBlockID());
      if (!pair.getFirst().equals(id))
        throw new IndexRebuildNeeded(null);
      return pair.getSecond();
    } catch (FileFormatException e) {
      // todo
      throw new Failure(e);
    } catch (StoreDataFile.BlockIsFree e) {
      throw new IndexRebuildNeeded(e);
    } finally {
      dataFile.close();
    }
  }

  private int findFittingFile(String id, byte[] buffer, StoreFeature[] features) {
    List<StoreIndex.FileInfo> files = myIndex.getFiles();
    SDFFormat lastFormat = null;
    int size = buffer.length;
    for (int i = 0; i < files.size(); i++) {
      StoreIndex.FileInfo fileInfo = files.get(i);
      SDFFormat format = SDFFormatUtils.getFormatByVersion(fileInfo.getFormatVersion());
      if (format == null) {
        continue;
      }
      if (!format.equals(lastFormat)) {
        lastFormat = format;
        size = lastFormat.getRequiredBlockSize(id, buffer, features);
      }
      if (fileInfo.getBlockSize() >= size)
        return i;
    }
    // todo
    throw new Failure("block is too large [" + size + "]");
  }

  private void checkStatus() throws IOException {
    if (myIndex == null)
      loadIndex();
  }

  private void loadIndex() throws IOException {
    File indexFile = file(INDEX_FILE_NAME);
    if (!indexFile.exists()) {
      buildIndex();
    } else {
      try {
        myIndex = new StoreIndexFile(indexFile).read();
        checkIndex();
      } catch (FileFormatException e) {
        Log.warn("corrupt index file, rebuilding", e);
        buildIndex();
      } catch (IOException e) {
        Log.warn("corrupt index file, rebuilding", e);
        buildIndex();
      } catch (IndexRebuildNeeded e) {
        Log.warn("corrupt index file, rebuilding", e);
        buildIndex();
      }
    }
  }

  private void checkIndex() throws IndexRebuildNeeded {
    // 1. check files
    List<StoreIndex.FileInfo> files = myIndex.getFiles();
    for (StoreIndex.FileInfo fileInfo : files) {
      File file = file(fileInfo.getFileName());
      long lastSize = fileInfo.getLastSize();
      if (!file.isFile()) {
        throw new IndexRebuildNeeded("file " + file + " does not exist", null);
      }
      long currentSize = file.length();
      if (currentSize != lastSize) {
        throw new IndexRebuildNeeded("file " + file + " has incorrect length (" + currentSize + " vs. " + lastSize + ")", null);
      }
    }

    // 2. check entries
    Collection<StoreIndex.EntryInfo> entries = myIndex.getEntries().values();
    for (StoreIndex.EntryInfo entry : entries) {
      int index = entry.getFileIndex();
      if (index < 0 || index >= files.size()) {
        throw new IndexRebuildNeeded("reference to out-of-bounds index: " + index + " " + files.size(), null);
      }
    }

    // 3. check there are enough files
    boolean hasLargestBlock = false;
    for (StoreIndex.FileInfo file : files) {
      int blockSize = file.getBlockSize();
      if (blockSize >= StoreDataFile.TARGET_MAX_BLOCK_SIZE) {
        hasLargestBlock = true;
        break;
      }
    }
    if (!hasLargestBlock) {
      throw new IndexRebuildNeeded("store doesn't have largest block (migration?)", null);
    }
  }

  private void buildIndex() throws IOException {
    File indexFile = file(INDEX_FILE_NAME);
    if (indexFile.exists()) {
      if (!indexFile.delete())
        throw new IOException("cannot delete outdated index file");
    }

    // scan existing files
    SortedMap<Integer, StoreIndex.FileInfo> fileMap = scanExistingFiles();
    createFilesForFullCoverage(fileMap);
    StoreIndex.FileInfo[] files = fileMap.values().toArray(new StoreIndex.FileInfo[fileMap.size()]);
    SortedMap<String, StoreIndex.EntryInfo> entryMap = scanEntries(files);
    myIndex = new StoreIndex(Arrays.asList(files), entryMap);
    dumpIndex();
  }

  private void dumpIndex() throws IOException {
    try {
      StoreIndexFile indexFile = new StoreIndexFile(file(INDEX_FILE_NAME));
      IOException lastException = null;
      for (int i = 0; i < INDEX_WRITE_ATTEMPTS; i++) {
        try {
          if (i > 0) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              throw new RuntimeInterruptedException(e);
            }
          }
          indexFile.write(myIndex);
          return;
        } catch (IOException e) {
          Log.debug(e);
          lastException = e;
        }
      }
      throw lastException;
    } finally {
      assert checkIndexDebug();
    }
  }

  private boolean checkIndexDebug() {
    try {
      checkIndex();
    } catch (IndexRebuildNeeded e) {
      Log.warn(e);
    }
    return true;
  }

  private SortedMap<String, StoreIndex.EntryInfo> scanEntries(StoreIndex.FileInfo[] files)
    throws IOException {
    SortedMap<String, StoreIndex.EntryInfo> entryMap = Collections15.treeMap();
    for (int i = 0; i < files.length; i++) {
      StoreIndex.FileInfo fileInfo = files[i];
      StoreDataFile dataFile = new StoreDataFile(file(fileInfo.getFileName()));
      try {
        dataFile.open();
        List<Pair<Integer, String>> content = dataFile.scanContent();
        for (Iterator<Pair<Integer, String>> iterator = content.iterator(); iterator.hasNext();) {
          Pair<Integer, String> pair = iterator.next();
          entryMap.put(pair.getSecond(), new StoreIndex.EntryInfo(pair.getSecond(), i, pair.getFirst().intValue()));
        }
      } catch (FileFormatException e) {
        Log.warn("invalid data file " + dataFile.getPath(), e);
      } finally {
        dataFile.close();
      }
    }
    return entryMap;
  }

  private void createFilesForFullCoverage(SortedMap<Integer, StoreIndex.FileInfo> existingFiles) throws IOException {
    // create files for uncovered sizes
    int size = StoreDataFile.MIN_BLOCK_SIZE;
    while (true) {
      int neighbor = findNeighbor(existingFiles, size);
      if (neighbor == -1) {
        StoreDataFile dataFile = new StoreDataFile(createDataFilePath(size));
        try {
          dataFile.create(size, myDefaultFormat);
          existingFiles.put(size, new StoreIndex.FileInfo(dataFile));
        } finally {
          dataFile.close();
        }
      }
      if (size >= StoreDataFile.TARGET_MAX_BLOCK_SIZE)
        break;
      size = (int) (size * BLOCK_RATIO);
    }
  }

  private SortedMap<Integer, StoreIndex.FileInfo> scanExistingFiles() {
    File[] allFiles = myWorkArea.getStorerDir().listFiles();
    SortedMap<Integer, StoreIndex.FileInfo> existingFiles = Collections15.treeMap(); // key = block size
    if (allFiles != null) {
      for (File f : allFiles) {
        if (!f.isFile())
          continue;
        int blockSize = -1;
        StoreDataFile file = new StoreDataFile(f);
        try {
          file.open();
          blockSize = file.getBlockSize();
          existingFiles.put(blockSize, new StoreIndex.FileInfo(file));
        } catch (FileFormatException e) {
          Log.warn("system folder contains file of bad format " + f, e);
          continue;
        } catch (IOException e) {
          Log.warn("system folder contains unreadable file " + f, e);
          continue;
        } finally {
          file.close();
        }
      }
    }
    return existingFiles;
  }

  private File createDataFilePath(int size) {
    // weird algo :)
    File file = null;
    do {
      String uid = new UID().toString();
      String hexSize = Util.upper(Integer.toHexString(size));
      while (hexSize.length() < 8)
        hexSize = '0' + hexSize;
      String name = hexSize + uid.substring(8);
      file = file(name);
    } while (file.exists());
    return file;
  }

  private int findNeighbor(SortedMap<Integer, ?> files, int size) {
    int lowerBound = (int) (size / BLOCK_RATIO + 2);
    int higherBound = (int) (size * BLOCK_RATIO - 2);
    for (Integer integer : files.keySet()) {
      int key = integer.intValue();
      if (key > higherBound)
        return -1;
      if (key > lowerBound)
        return key;
    }
    return -1;
  }

  private File file(String pathlessFileName) {
    return new File(myWorkArea.getStorerDir(), new File(pathlessFileName).getName());
  }

  private static class IndexRebuildNeeded extends Exception {
    public IndexRebuildNeeded(Throwable cause) {
      super(cause);
    }


    public IndexRebuildNeeded(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

