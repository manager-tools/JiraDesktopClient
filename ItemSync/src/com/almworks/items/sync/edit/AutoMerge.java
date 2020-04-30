package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.BranchUtil;
import com.almworks.items.sync.impl.HolderCache;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.ItemDiffImpl;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

class AutoMerge {
  private final DBWriter myWriter;
  private final long myItem;
  @Nullable
  private final ItemVersion myDoneUpload;
  private final int myDoneUploadHistory;
  @Nullable
  private final ItemVersion myDownload;
  @Nullable
  private final ItemVersion myConflict;
  @Nullable
  private final ItemVersion myBase;
  private final ItemAutoMerge myOperations;
  private final SyncManagerImpl myManager;
  private final EditCounterpart myJustEdited;

  private AutoMerge(DBWriter writer, long item, ItemVersion doneUpload, int doneUploadHistory, ItemAutoMerge operations,
    SyncManagerImpl manager, ItemVersion download, ItemVersion conflict, ItemVersion base, EditCounterpart justEdited) {
    myWriter = writer;
    myItem = item;
    myDoneUpload = doneUpload;
    myDoneUploadHistory = doneUploadHistory;
    myOperations = operations;
    myManager = manager;
    myDownload = download;
    myConflict = conflict;
    myBase = base;
    myJustEdited = justEdited;
  }

  /**
   * Performs auto merge of the item and probably some of its slaves.<br>
   * Successful merge means that item is not locked and merge procedure is performed. Successfully merged item may become (or stay) in conflict
   * state. Successfully merged item has no DOWNLOAD and UPLOAD shadows.
   * @return merged items or null if no items merged
   */
  @Nullable
  public static MergeEventImpl autoMerge(DBWriter writer, long item, ItemAutoMerge operations, SyncManagerImpl manager,
    EditCounterpart justEdited) {
    ItemVersion doneUpload = doneUpload(writer, item);
    int doneHistory = Util.NN(writer.getValue(item, SyncSchema.DONE_UPLOAD_HISTORY), 0);
    AutoMerge autoMerge = new AutoMerge(writer, item, doneUpload, doneHistory, operations, manager,
      download(writer, item), SyncUtils.readConflictIfExists(writer, item), SyncUtils.readBaseIfExists(writer, item),
      justEdited);
    return autoMerge.perform();
  }

  private static ItemVersion download(DBReader reader, long item) {
    return BranchUtil.readServerShadow(reader, item, SyncSchema.DOWNLOAD, false);
  }

  private static ItemVersion doneUpload(DBReader reader, long item) {
    return BranchUtil.readServerShadow(reader, item, SyncSchema.DONE_UPLOAD, false);
  }

  @Nullable
  public MergeEventImpl perform() {
    ItemDiffImpl local;
    if (isAfterUpload()) local = getDoneUploadDiff();
    else local = myBase != null ? ItemDiffImpl.createToTrunk(myBase, 0) : null;
    if (local != null && local.hasChanges()) myOperations.preProcess(local);
    if (local == null || !local.hasChanges()) return discardLocal(); // No local changes, local edit discarded, last server copied
    assert myBase != null;
    ItemVersion newServer = getLastNewServer();
    if (isAfterUpload()) { // Just uploaded
      return finishUpload(newServer);
    }
    // Has not empty local changes
    List<HistoryRecord> newHistory = local.getUpdatedHistory();
    if (newHistory != null) myWriter.setValue(myItem, SyncAttributes.CHANGE_HISTORY, HistoryRecord.serialize(newHistory));
    if (newServer == null) return MergeEventImpl.sync(myItem);// No newer server version, history updated, nothing to do
    ItemDiffImpl server = ItemDiffImpl.createServerDiff(myBase, newServer);
    MergeDataImpl data = MergeDataImpl.create(local, server);
    myOperations.resolve(data);
    if (!data.isConflictResolved()) { // Unresolvable conflict detected
      setNewConflict(newServer);
      return new MergeEventImpl().addItem(myItem, SyncState.CONFLICT); // Unresolvable conflict, last DOWNLOAD copied to CONFLICT
    }
    if (data.isResolvedDelete()) { // Resolved to delete
      return physicalDeleteSubtree();  // Deleted or deferred
    }
    if (data.isDiscardEdit()) return discardLocal();
    setNewBase(newServer);
    newHistory = data.getUpdatedHistory();
    if (newHistory != null) myWriter.setValue(myItem, SyncAttributes.CHANGE_HISTORY, HistoryRecord.serialize(newHistory));
    if (server.hasChanges()) {
      Map<DBAttribute<?>, Object> resolution = data.getResolution();
      for (Map.Entry<DBAttribute<?>, Object> entry : resolution.entrySet()) // Write resolution to trunk
        myWriter.setValue(myItem, (DBAttribute<Object>)entry.getKey(), entry.getValue());
      for (DBAttribute<?> a : server.getChanged()) { // Write server changes to trunk (since there is no conflict with local edit)
        if (resolution.containsKey(a)) continue;
        DBAttribute<Object> attr = (DBAttribute<Object>) a;
        myWriter.setValue(myItem, attr, newServer.getValue(attr));
      }
    }
    if (newServer == myBase && myDownload == null && myConflict == null) return new MergeEventImpl().addItem(myItem, SyncState.EDITED);
    return new AutoMerge(myWriter, myItem, null, 0, myOperations, myManager, null, null, myBase, myJustEdited).perform();
  }

  private boolean isAfterUpload() {
    return myDoneUpload != null || myDoneUploadHistory > 0;
  }

  private ItemDiffImpl getDoneUploadDiff() {
    return ItemDiffImpl.createToTrunk(myDoneUpload != null ? myDoneUpload : myBase, myDoneUploadHistory);
  }

  private MergeEventImpl finishUpload(ItemVersion newServer) {
    assert isAfterUpload();
    if (newServer == null || myBase == null) {
      if (myBase == null) LogHelper.error("Missing upload reason", myItem, newServer, myBase, myDoneUpload, myDoneUploadHistory);
      else LogHelper.error("Missing new server", myItem, myBase, myConflict, myDownload, myDoneUpload, myDoneUploadHistory);
      forgetUpload(myItem);
      return new AutoMerge(myWriter, myItem, null, 0, myOperations, myManager, null, myConflict, myBase, myJustEdited).perform();
    }
    if (myConflict != null) {
      Log.error("Upload finished when item has conflict " + myItem);
      forgetUpload(myItem);
      return new AutoMerge(myWriter, myItem, null, 0, myOperations, myManager, null, myConflict, myBase, myJustEdited).perform();
    }
    ItemDiffImpl local = getDoneUploadDiff();
    if (local.hasChanges()) myOperations.preProcess(local);
    if (!local.hasChanges()) return discardLocal(); // No local changes, local edit discarded, last server copied
    Collection<? extends DBAttribute<?>> serverAttributes;
    if (myDoneUpload != null && !local.hasHistory()) {
      HashSet<DBAttribute<?>> attrs = Collections15.hashSet();
      attrs.addAll(myBase.getAllShadowableMap().keySet());
      attrs.addAll(newServer.getAllShadowableMap().keySet());
      serverAttributes = attrs;
    } else {
      ItemDiff server = ItemDiffImpl.createServerDiff(myBase, newServer);
      serverAttributes = server.getChanged();
    }
    List<HistoryRecord> newHistory = local.getUpdatedHistory();
    if (newHistory != null) myWriter.setValue(myItem, SyncAttributes.CHANGE_HISTORY, HistoryRecord.serialize(newHistory));
    for (DBAttribute<?> a : serverAttributes) {
      DBAttribute<Object> attribute = (DBAttribute<Object>) a;
      if (!local.isChanged(attribute)) myWriter.setValue(myItem, attribute, newServer.getValue(attribute));
    }
    setNewBase(newServer);
    return new AutoMerge(myWriter, myItem, null, 0, myOperations, myManager, null, null, SyncUtils.readBaseIfExists(myWriter, myItem), myJustEdited).perform();
  }

  @Nullable
  private ItemVersion getLastNewServer() {
    if (myDownload != null) return myDownload;
    else if (myConflict != null) return myConflict;
    return myBase;
  }

  @Nullable
  private MergeEventImpl discardLocal() {
    ItemVersion server = getLastServer();
    if (server == null) return MergeEventImpl.sync(myItem);
    if (server.getNNValue(SyncSchema.INVISIBLE, false)) return physicalDeleteSubtree();
    SyncSchema.discardSingle(myWriter, myItem);
    markSync(myItem);
    return MergeEventImpl.sync(myItem);
  }

  @Nullable
  private ItemVersion getLastServer() {
    if (myDownload != null) return myDownload;
    return myConflict != null ? myConflict : myBase;
  }

  @Nullable
  private MergeEventImpl physicalDeleteSubtree() {
    LongList slaves = SyncUtils.getSlavesSubtree(myWriter, myItem);
    if (!myManager.lockOrMergeLater(myWriter, slaves, myJustEdited, LongArray.create(myItem))) return null;
    for (int i = 0; i < slaves.size(); i++) physicalDeleteSingle(slaves.get(i));
    return MergeEventImpl.sync(myItem).addItems(slaves, SyncState.SYNC);
  }

  private void physicalDeleteSingle(long item) {
    myWriter.clearItem(item);
    markSync(item);
  }

  private void markSync(long item) {
    mergeDone(item, null, null);
  }

  private void setNewBase(ItemVersion base) {
    mergeDone(myItem, base, null);
  }

  private void setNewConflict(ItemVersion conflict) {
    mergeDone(myItem, myBase, conflict);
  }

  private void mergeDone(long item, @Nullable ItemVersion base, @Nullable ItemVersion conflict) {
    HolderCache holders = HolderCache.instance(myWriter);
    holders.setBase(item, base != null ? base.getAllShadowableMap() : null);
    holders.setConflict(item, conflict != null ? conflict.getAllShadowableMap() : null);
    forgetUpload(item);
    if (base == null) myWriter.setValue(item, SyncSchema.UPLOAD_ATTEMPT, null);
  }

  private void forgetUpload(long item) {
    HolderCache holders = HolderCache.instance(myWriter);
    holders.setDownload(item, null);
    holders.setDoneUpload(item, null, null);
    holders.setUploadTask(item, null);
  }

  public static boolean needsMerge(DBReader reader, long item) {
    return HolderCache.instance(reader).hasConflict(item);
  }
}
