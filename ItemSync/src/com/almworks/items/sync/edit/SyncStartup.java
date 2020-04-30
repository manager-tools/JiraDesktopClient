package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.sync.impl.HolderCache;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dyoma
 */
class SyncStartup {
  private final Database myDatabase;
  private final SyncManagerImpl mySyncManager;
  private final AtomicBoolean myStartupDone = new AtomicBoolean(false);

  public SyncStartup(Database database, SyncManagerImpl syncManager) {
    myDatabase = database;
    mySyncManager = syncManager;
  }

  public <T> DBResult<T> enquireWrite(WriteTransaction<T> transaction) {
    if (myStartupDone.get()) return myDatabase.writeBackground(transaction);
    StartupTransaction<T> startup = new StartupTransaction<T>(transaction);
    DBResult<T> dbResult = myDatabase.writeBackground(startup);
    dbResult.finallyDoWithResult(ThreadGate.STRAIGHT, startup);
    return dbResult;
  }

  public <T> DBResult<T> enquireRead(DBPriority priority, ReadTransaction<T> transaction) {
    return myDatabase.read(priority, transaction);
  }

  void startupNow() {
    if (myStartupDone.get()) return;
    StartupTransaction<Object> startup = new StartupTransaction<Object>(null);
    DBResult<Object> dbResult = myDatabase.writeBackground(startup);
    dbResult.finallyDoWithResult(ThreadGate.STRAIGHT, startup);
  }

  private void runStartup(DBWriter writer) {
    finishUploads(writer);
    clearUploads(writer);
    collectMerge(writer);
    clearIllegalState(writer);
  }

  private void clearIllegalState(DBWriter writer) {
    LongSet illegals = new LongSet();
    illegals.addAll(writer.query(SyncSchema.HAS_DONE_UPLOAD).copyItemsSorted());
    illegals.addAll(writer.query(DPNotNull.create(SyncSchema.UPLOAD_TASK)).copyItemsSorted());
    illegals.addAll(writer.query(DPNotNull.create(SyncSchema.DOWNLOAD)).copyItemsSorted());
    if (!illegals.isEmpty()) {
      Log.error("Illegal states detected: " + illegals);
      HolderCache holders = HolderCache.instance(writer);
      for (int i = 0; i < illegals.size(); i++) {
        long item = illegals.get(i);
        boolean doneUpload = SyncSchema.hasDoneUpload(writer, item);
        boolean upload = writer.getValue(item, SyncSchema.UPLOAD_TASK) != null;
        boolean download = writer.getValue(item, SyncSchema.DOWNLOAD) != null;
        if (doneUpload || upload || download) {
          Log.warn(item + ": " +
            (doneUpload ? "DONE_UPLOAD " : " ") +
            (upload ? "UPLOAD " : " ") +
            (download ? "DOWNLOAD " : " ")
          );
          holders.setDoneUpload(item, null, null);
          holders.setDownload(item, null);
          holders.setUploadTask(item, null);
        } else
          Log.warn(item + " has no problems");
      }
    }
  }

  private void finishUploads(DBWriter writer) {
    LongArray items = writer.query(SyncSchema.HAS_DONE_UPLOAD).copyItemsSorted();
    mySyncManager.autoMergeNow(writer, items, null);
  }

  private void collectMerge(DBWriter writer) {
    LongArray downloaded = writer.query(DPNotNull.create(SyncSchema.DOWNLOAD)).copyItemsSorted();
    mySyncManager.autoMergeNow(writer, downloaded, null);
  }

  private void clearUploads(DBWriter writer) {
    LongArray failedUploads = writer.query(DPNotNull.create(SyncSchema.UPLOAD_TASK)).copyItemsSorted();
    for (int i = 0; i < failedUploads.size(); i++) {
      long item = failedUploads.get(i);
      writer.setValue(item, SyncSchema.UPLOAD_TASK, null);
      if (writer.getValue(item, SyncSchema.BASE) != null) {
        Long type = writer.getValue(item, DBAttribute.TYPE);
        String typeName = type != null ? writer.getValue(type, DBAttribute.NAME) : null;
        Log.warn("Detected failed upload " + item + " " + typeName);
      }
    }
  }

  private class StartupTransaction<T> implements WriteTransaction<T>, Procedure<DBResult<T>> {
    private final WriteTransaction<T> myDelegate;

    private StartupTransaction(WriteTransaction<T> delegate) {
      myDelegate = delegate;
    }

    @Override
    public T transaction(DBWriter writer) throws DBOperationCancelledException {
      if (!myStartupDone.get()) runStartup(writer);
      return myDelegate != null ? myDelegate.transaction(writer) : null;
    }

    @Override
    public void invoke(DBResult<T> result) {
      if (result.isSuccessful()) myStartupDone.set(true);
    }
  }
}
