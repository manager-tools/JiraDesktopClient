package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBWriter;
import com.almworks.items.api.WriteTransaction;
import com.almworks.items.sync.ItemUploader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.impl.HolderCache;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Log;

class SyncUploadTask2 implements WriteTransaction<Boolean> {
  private final UploadProcessImpl myProcess;
  private final ItemUploader myUploader;

  public SyncUploadTask2(SyncManagerImpl manager, ItemUploader uploader) {
    myUploader = uploader;
    myProcess = new UploadProcessImpl(manager);
  }

  public void perform() throws InterruptedException {
    try {
      Boolean prepared = myProcess.getManager().enquireWrite(this).waitForCompletion();
      if (!Boolean.TRUE.equals(prepared)) return;
      myUploader.doUpload(myProcess);
      myProcess.priCancelUpload(null).waitForCompletion();
    } finally {
      try {
        myProcess.priCancelUpload(null).waitForCompletion();
      } finally {
        myProcess.unregisterAll();
      }
    }
  }

  @Override
  public Boolean transaction(DBWriter writer) throws DBOperationCancelledException {
    PrepareUpload prepare = new PrepareUpload(writer, myProcess);
    myUploader.prepare(prepare);
    if (!myProcess.isRegistered()) return null;
    return true;
  }

  private static class PrepareUpload implements ItemUploader.UploadPrepare {
    private final DBWriter myWriter;
    private final UploadProcessImpl myProcess;
    private final LongSet myPrepared = new LongSet();
    private final TLongObjectHashMap<byte[]> myPrevAttempt = new TLongObjectHashMap<>();

    public PrepareUpload(DBWriter writer, UploadProcessImpl process) {
      myWriter = writer;
      myProcess = process;
    }

    @Override
    public VersionSource getTrunk() {
      return BranchSource.trunk(myWriter);
    }

    @Override
    public ItemVersion addToUpload(long item) {
      if (myPrepared.contains(item)) return getTrunk().forItem(item);
      if (!canUpload(item)) return null;
      if (!myProcess.register(item)) return null;
      prepare(item);
      return getTrunk().forItem(item);
    }

    @Override
    public boolean addAllToUpload(LongList items) {
      if (items == null || items.isEmpty()) return true;
      LongArray filtered = null;
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (item <= 0 || myPrepared.contains(item)) {
          if (filtered == null) filtered = LongArray.copy(items.subList(0, i));
          continue;
        }
        if (!canUpload(item)) return false;
        if (filtered != null) filtered.add(item);
      }
      if (filtered != null) items = filtered;
      if (!myProcess.register(items)) return false;
      for (int i = 0; i < items.size(); i++) prepare(items.get(i));
      return true;
    }

    @Override
    public boolean isUploadable(long item) {
      if (item <= 0) return false;
      if (isPrepared(item)) return true;
      if (!canUpload(item)) return false;
      return myProcess.canLock(LongArray.create(item));
    }

    @Override
    public boolean isPrepared(long item) {
      return myPrepared.contains(item);
    }

    @Override
    public void cancelUpload() {
      removeFromUpload(LongArray.copy(myPrepared));
    }

    @Override
    public void removeFromUpload(long item) {
      removeFromUpload(LongArray.create(item));
    }

    @Override
    public void removeFromUpload(LongList items) {
      LongArray cancelled = new LongArray();
      try {
        for (int i = 0; i < items.size(); i++) {
          long item = items.get(i);
          if (!myPrepared.remove(item)) continue;
          HolderCache holders = HolderCache.instance(myWriter);
          holders.setUploadTask(item, null);
          if (myPrevAttempt.containsKey(item)) {
            myWriter.setValue(item, SyncSchema.UPLOAD_ATTEMPT, myPrevAttempt.get(item));
            myPrevAttempt.remove(item);
          }
          cancelled.add(item);
        }
      } finally {
        myProcess.unregister(cancelled);
      }
    }

    @Override
    public boolean setUploadAttempt(long item, byte[] attempt) {
      if (!myPrepared.contains(item)) return false;
      if (!myPrevAttempt.contains(item)) myPrevAttempt.put(item, myWriter.getValue(item, SyncSchema.UPLOAD_ATTEMPT));
      myWriter.setValue(item, SyncSchema.UPLOAD_ATTEMPT, attempt);
      return true;
    }

    private void prepare(long item) {
      if (myPrepared.contains(item)) {
        LogHelper.error("Already prepared", item);
        return;
      }
      myPrepared.add(item);
      HolderCache holders = HolderCache.instance(myWriter);
      assert canUpload(item) : item;
      ItemVersion trunk = SyncUtils.readTrunk(myWriter, item);
      holders.setUploadTask(item, trunk.getAllShadowableMap());
    }

    private boolean canUpload(long item) {
      if (SyncUtils.isRemoved(SyncUtils.readTrunk(myWriter, item))) return false;
      HolderCache holders = HolderCache.instance(myWriter);
      if (holders.hasUploadTask(item)) return false;
      if (AutoMerge.needsMerge(myWriter, item)) {
        Log.warn("Cannot upload, merge is required for " + item);
        return false;
      }
      return holders.hasBase(item) && !holders.hasDoneUpload(item);
    }
  }
}
