package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongCollector;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.UploadDrain;
import com.almworks.items.sync.UploadProcess;
import com.almworks.items.sync.impl.AttributeInfo;
import com.almworks.items.sync.impl.HolderCache;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class UploadProcessImpl implements UploadProcess {
  private final SyncManagerImpl myManager;

  public UploadProcessImpl(SyncManagerImpl manager) {
    myManager = manager;
  }

  @Override
  public DBResult<Object> writeUploadState(DownloadProcedure<? super UploadDrain> procedure) {
    return new UploadDrainImpl(procedure).start();
  }

  @Override
  public void uploadDone() {
    priCancelUpload(null);
  }

  @Override
  public void cancelUpload(long item) {
    cancelUpload(new LongList.Single(item));
  }

  @Override
  public void cancelUpload(LongList items) {
    priCancelUpload(items);
  }

  public SyncManagerImpl getManager() {
    return myManager;
  }

  private UploadLocks getUploadLocks() {
    return myManager.getUploadLocks();
  }

  /**
   * Cancel upload
   * @param items to cancel upload, null means cancel the whole upload (all locked items)
   */
  DBResult<Object> priCancelUpload(@Nullable LongList items) {
    final long[] itemsArray = items != null ? items.toNativeArray() : null;
    return writeUploadState(new DownloadProcedure<UploadDrain>() {
      @Override
      public void write(UploadDrain drain) throws DBOperationCancelledException {
        long[] toCancel = itemsArray != null ? itemsArray : getUploadLocks().getLocked(UploadProcessImpl.this);
        for (long item : toCancel) {
          drain.cancelUpload(item);
        }
      }

      @Override
      public void onFinished(DBResult<?> result) {
        if (!result.isSuccessful()) {
          LogHelper.error("Failed to cancel upload. Items are still locked", result.getError(), result.getErrors());
        }
      }
    });
  }

  public boolean register(LongList items) {
    return getUploadLocks().registerTask(this, items);
  }

  public boolean register(long item) {
    return getUploadLocks().registerTask(this, new LongList.Single(item));
  }

  public void unregisterAll() {
    getUploadLocks().unregisterTask(this);
  }

  public void unregister(LongList items) {
    getUploadLocks().unregister(this, items);
  }

  public boolean isRegistered() {
    return getUploadLocks().isRegistered(this);
  }

  public boolean canLock(LongArray items) {
    return !getUploadLocks().isAnyLocked(items);
  }

  private class UploadDrainImpl extends BaseDownloadDrain implements UploadDrain {
    private final LongSet myDownloaded = new LongSet();
    private final DownloadProcedure<? super UploadDrain> myProcedure;

    public UploadDrainImpl(DownloadProcedure<? super UploadDrain> procedure) {
      super(myManager);
      myProcedure = procedure;
    }

    @Override
    public ItemVersionCreator setAllDone(long item) {
      AttributeMap task = HolderCache.instance(getReader()).getUploadTask(item);
      return finishUpload(item, task != null ? task.keySet() : null, 0);
    }

    @Override
    public ItemVersionCreator finishUpload(long item, Collection<? extends DBAttribute<?>> state, int history) {
      HolderCache holders = HolderCache.instance(getReader());
      AttributeMap task = holders.getUploadTask(item);
      if (task == null) {
        LogHelper.error("Not during upload", item, state, history);
        return changeItem(item);
      }
      if (state == null) state = Collections15.emptyCollection();
      if (history < 0) {
        LogHelper.error("Negative history", history);
        history = 0;
      }
      boolean inProgress = getUploadLocks().isLocked(UploadProcessImpl.this, item);
      if (!inProgress) {
        LogHelper.error("Another upload in progress", item, task);
        return null;
      }
      AttributeMap base = holders.getBase(item);
      if (base == null) {
        LogHelper.error("Missing base", item);
        base = holders.getHolder(item, null, true).getAllShadowableMap();
      }
      AttributeMap doneUpload = new AttributeMap();
      doneUpload.putAll(base);
      Set<DBAttribute<?>> notDone = Collections15.hashSet();
      boolean anyUploaded = false;
      for (DBAttribute<?> attribute : task.keySet())
        if (state.contains(attribute)) {
          doneUpload.putFrom(task, attribute);
          anyUploaded = true;
        } else notDone.add(attribute);
      AttributeInfo attributeInfo = AttributeInfo.instance(getReader());
      // if attribute value is null in task but it was uploaded it means that null value is uploaded
      for (DBAttribute<?> attribute : state)
        if (!task.containsKey(attribute)) {
          if (attributeInfo.isShadowable(attribute)) {
            doneUpload.put(attribute, null);
            anyUploaded = true;
          }
        }
      // If base contains not null not mentioned in task and uploaded it means that upload had to change it to null but hadn't done
      for (DBAttribute<?> attribute : base.keySet())
        if (!state.contains(attribute) && !task.containsKey(attribute) && attributeInfo.isShadowable(attribute))
          notDone.add(attribute);
      holders.setUploadTask(item, null);
      holders.setDoneUpload(item, anyUploaded ? doneUpload : null, history);
      myDownloaded.add(item);
      ItemVersionCreator creator = changeItem(item);
      if (!notDone.isEmpty()) logPartialUpload(notDone, creator, doneUpload, task);
      return creator;
    }

    private void logPartialUpload(Set<DBAttribute<?>> notDone, ItemVersionCreator creator, AttributeMap doneUpload, AttributeMap task) {
      ArrayList<String> actuallyNotDone = Collections15.arrayList();
      for (DBAttribute<?> attribute : notDone) {
        Object done = doneUpload.get(attribute);
        Object request = task.get(attribute);
        if (!DatabaseUtil.isEqualValue((DBAttribute<Object>)attribute, done, request))
          actuallyNotDone.add(String.format("<a=%s, task='%s', done='%s'>", attribute, request, done));
      }
      if (actuallyNotDone.isEmpty()) LogHelper.debug("Not reported upload (but there is no change)", creator, notDone);
      else LogHelper.warning("Partial upload", creator, actuallyNotDone, "\nNot reported", notDone);
    }

    @Override
    protected void collectToMerge(LongCollector target) {
      super.collectToMerge(target);
      target.addAll(myDownloaded);
    }

    @Override
    public void cancelUpload(long item) {
      finishUpload(item, null, 0);
    }

    @Override
    public LongList getLockedForUpload() {
      long[] items = getUploadLocks().getLocked(UploadProcessImpl.this);
      if (items.length <= 0) return LongList.EMPTY;
      LongSet result = LongSet.create(items);
      result.removeAll(myDownloaded);
      return result;
    }

    @Override
    protected void performTransaction() {
      myProcedure.write(this);
    }

    @Override
    protected void onTransactionFinished(DBResult<?> result) {
      if (result.isSuccessful()) UploadProcessImpl.this.unregister(myDownloaded);
      myProcedure.onFinished(result);
    }
  }
}
