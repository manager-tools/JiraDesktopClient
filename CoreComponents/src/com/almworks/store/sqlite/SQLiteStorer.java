package com.almworks.store.sqlite;

import com.almworks.api.store.StoreFeature;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.store.*;
import com.almworks.util.Pair;
import com.almworks.util.fileformats.FileFormatException;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteStorer implements Storer {
  private static final byte[] CLEAR = new byte[0];
  private final SQLiteStoreThread myThread;
  private final ConcurrentHashMap<String, Pair<byte[], StoreFeature[]>> myNotWrittenData = new ConcurrentHashMap<String, Pair<byte[], StoreFeature[]>>();
  private final SDFFormat myFormat = new SDFFormatV2();
  private static final Pair<byte[],StoreFeature[]> CLEAR_ID = Pair.create(CLEAR, (StoreFeature[])null);

  public SQLiteStorer(File dbFile, FileStorer legacyStore) {
    File parentFile = dbFile != null ? dbFile.getParentFile() : null;
    if (parentFile != null) parentFile.mkdirs();
    final SQLiteConnection sqliteConn = new SQLiteConnection(dbFile);
    CrashSQLiteAction.ourConnection = sqliteConn;
    myThread = new SQLiteStoreThread(sqliteConn, new InitJob(legacyStore, myFormat));
  }

  public void stop() {
    myThread.stop();
  }

  @Override
  public void store(String id, byte[] data, StoreFeature[] features) throws IOException {
    Pair<byte[], StoreFeature[]> current = myNotWrittenData.get(id);
    data = myFormat.transformBeforeWriting(data, features);
    if (current != null) {
      byte[] currentBytes = current.getFirst();
      if (currentBytes != CLEAR && Arrays.equals(currentBytes, data))
        return;
    }
    myNotWrittenData.put(id, Pair.create(data, features));
    myThread.enqueue(new FlushStore(myNotWrittenData));
  }

  @Override
  public byte[] load(String id, StoreFeature[] f) throws IOException, InterruptedException {
    Pair<byte[], StoreFeature[]> current = myNotWrittenData.get(id);
    byte[] data;
    StoreFeature[] features;
    if (current == null) {
      StoreJob.Read job = performRead(id);
      data = job.getData();
      features = job.getFeatures();
    } else if (current != null && current.getFirst() == CLEAR) return null;
    else {
      data = ArrayUtil.arrayCopy(current.getFirst());
      features = current.getSecond();
    }
    try {
      data = myFormat.transformAfterReading(data, features);
    } catch (FileFormatException e) {
      Log.error(e);
      return null;
    }
    return data;
  }

  StoreJob.Read performRead(String id) throws InterruptedException, IOException {
    StoreJob.Read job = new StoreJob.Read(id, 2);
    myThread.enqueue(job);
    job.waitDone();
    Exception failure = job.getFailedReason();
    if (failure != null) throw new IOException(failure);
    return job;
  }

  @Override
  public void clear(String id) throws IOException {
    Pair<byte[], StoreFeature[]> current = myNotWrittenData.get(id);
    if (current != null && current.getFirst() == CLEAR) return;
    myNotWrittenData.put(id, CLEAR_ID);
    myThread.enqueue(new FlushStore(myNotWrittenData));
  }

  @Override
  public boolean isSupported(StoreFeature[] features) {
    for (StoreFeature feature : features) {
      if (feature != StoreFeature.ENCRYPTED) return false;
    }
    return true;
  }

  int getPendingCount() {
    return myNotWrittenData.size();
  }

  private static class FlushStore extends StoreJob {
    private final ConcurrentHashMap<String, Pair<byte[], StoreFeature[]>> myData;

    FlushStore(ConcurrentHashMap<String, Pair<byte[], StoreFeature[]>> data) {
      super(1);
      myData = data;
    }

    @Override
    protected void doPerform(SQLiteConnection connection) throws SQLiteException {
      Enumeration<String> enumeration = myData.keys();
      while (enumeration.hasMoreElements()) {
        String id = enumeration.nextElement();
        updateId(connection, id);
      }
    }

    private void updateId(SQLiteConnection connection, String id) throws SQLiteException {
      Pair<byte[], StoreFeature[]> toWrite = myData.get(id);
      if (toWrite == null) return;
      if (toWrite.getFirst() == CLEAR) clearRow(connection, id);
      else {
        StoreFeature[] features = toWrite.getSecond();
        byte[] data = toWrite.getFirst();
        writeRow(connection, id, data, features);
      }
      myData.remove(id, toWrite);
    }


    private boolean isEqualData(byte[] bytes1, byte[] bytes2) {
      if (bytes1 == bytes2) return true;
      if (bytes1 == null || bytes2 == null) return false;
      if (bytes1 == CLEAR || bytes2 == CLEAR) return false;
      return Arrays.equals(bytes1, bytes2);
    }

    @Override
    public String toString() {
      return "Flush store job";
    }
  }


  private static class InitJob extends StoreJob {
    private static final String MIGRATION_DONE = "$sqliteStore$.migrated";
    @Nullable
    private final FileStorer myLegacyStore;
    private final SDFFormat myFormat;

    public InitJob(FileStorer legacyStore, SDFFormat format) {
      super(1);
      myLegacyStore = legacyStore;
      myFormat = format;
    }

    @Override
    protected void doPerform(SQLiteConnection connection) throws SQLiteException {
      if (!SQLiteStoreThread.performJob(connection, StoreJob.INIT_SCHEMA, "Init schema")) return;
      if (myLegacyStore == null) return;
      StoreJob.Read done = new StoreJob.Read(MIGRATION_DONE, 2);
      if (!SQLiteStoreThread.performJob(connection, done, "Failed to load migration state. Migration skipped.")) return;
      byte[] loaded = done.getData();
      if (loaded != null && loaded.length == 1 && loaded[0] == 1) return;
      Collection<StoreIndex.EntryInfo> prevEntries;
      try {
        prevEntries = myLegacyStore.getAllEntries();
      } catch (IOException e) {
        Log.error("Cannot load old store. Migration skipped", e);
        return;
      }
      if (prevEntries == null) {
        Log.error("Failed to load old store. Migration skipped");
        return;
      }
      for (StoreIndex.EntryInfo entry : prevEntries) {
        byte[] bytes;
        StoreFeature[] features;
        String id = entry.getStorePath();
        try {
          features = myLegacyStore.getFeaures(id);
          bytes = myLegacyStore.load(id);
        } catch (IOException e) {
          Log.error("Error loading old store entry: " + id, e);
          continue;
        } catch (InterruptedException e) {
          Log.error("Migration interrupted. Migration cancelled", e);
          return;
        }
        bytes = myFormat.transformBeforeWriting(bytes, features);
        StoreJob job = new StoreJob.Write(id, bytes, features);
        if (!SQLiteStoreThread.performJob(connection, job, "Failed to store entry to SQLite store " + id)) continue;
      }
      StoreJob migrated = new StoreJob.Write(MIGRATION_DONE, new byte[]{1}, StoreFeature.EMPTY_FEATURES_ARRAY);
      if (SQLiteStoreThread.performJob(connection, migrated, "Migration completed but failed to store state"))
        Log.debug("Store migration to SQLite complete");
    }
  }
}