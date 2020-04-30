package com.almworks.itemsync;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.*;
import com.almworks.items.sync.edit.SyncManagerImpl;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

public class ApplicationSyncManager implements SyncManager, ItemAutoMerge.Selector, Startable {
  private final SyncManagerImpl myManager;
  private final Database myDb;
  private final MergeOperationsManager.MergeProvider[] myProviders;
  private final MergeOperationsManagerImpl myOperations = new MergeOperationsManagerImpl();

  public ApplicationSyncManager(Database db, MergeOperationsManager.MergeProvider[] providers) {
    myDb = db;
    myProviders = providers;
    myManager = new SyncManagerImpl(db, this);
  }

  @Override
  public void start() {
    if (!myDb.getUserData().putIfAbsent(ROLE, this))
      LogHelper.error("Another SyncManager already registered", myDb.getUserData().getUserData(ROLE));
    for (MergeOperationsManager.MergeProvider provider : myProviders) provider.registerMergeOperations(myOperations);
    myManager.runStartup();
  }

  @Override
  public void stop() {
    myDb.getUserData().replace(ROLE, this, null);
    myOperations.clear();
  }

  @Override
  public ItemAutoMerge getOperations(DBReader reader, long item) {
    return myOperations.getOperations(reader, item);
  }

  @Override
  public boolean canUpload(long item) {
    return myManager.canUpload(item);
  }

  @Override
  public boolean canUploadAll(LongList items) {
    return myManager.canUploadAll(items);
  }

  @Override
  public boolean isDuringUpload(DBReader reader, long item) {
    return myManager.isDuringUpload(reader, item);
  }

  @Override
  public void commitEdit(EditCommit commit) {
    myManager.commitEdit(commit);
  }

  @Override
  public boolean commitEdit(LongList items, EditCommit commit) {
    return myManager.commitEdit(items, commit);
  }

  @Override
  @Nullable
  public EditorLock findLock(long item) {
    return myManager.findLock(item);
  }

  @Override
  public EditorLock findAnyLock(LongList items) {
    return myManager.findAnyLock(items);
  }

  @Override
  @Nullable
  public EditControl prepareEdit(long item) {
    return myManager.prepareEdit(item);
  }

  @Override
  @Nullable
  public EditControl prepareEdit(LongList items) {
    return myManager.prepareEdit(items);
  }

  @Override
  public void unsafeCommitEdit(EditCommit commit) {
    myManager.unsafeCommitEdit(commit);
  }

  @Override
  public void syncUpload(ItemUploader uploader) throws InterruptedException {
    myManager.syncUpload(uploader);
  }

  @Override
  public DBResult<Object> writeDownloaded(DownloadProcedure<? super DBDrain> procedure) {
    return myManager.writeDownloaded(procedure);
  }

  @Override
  public Modifiable getModifiable() {
    return myManager.getModifiable();
  }

  @Override
  public void requestAutoMerge(LongList items) {
    myManager.requestAutoMerge(items);
  }

  @Override
  public <T> DBResult<T> enquireRead(DBPriority priority, ReadTransaction<T> transaction) {
    return myManager.enquireRead(priority, transaction);
  }

  @Override
  public void addListener(Lifespan life, ThreadGate gate, Listener listener) {
    myManager.addListener(life, gate, listener);
  }

  @Nullable
  public static SyncManager getInstance(DBReader reader) {
    UserDataHolder userData = reader.getDatabaseUserData();
    if (userData == null) {
      LogHelper.error("Missing user data", reader);
      return null;
    }
    SyncManager manager = userData.getUserData(ROLE);
    LogHelper.assertError(manager != null, "Missing SyncManager");
    return manager;
  }
}
