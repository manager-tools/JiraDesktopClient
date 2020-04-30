package com.almworks.items.sync.edit;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemDiff;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.ItemDiffImpl;
import com.almworks.items.sync.util.MergeHistoryState;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.DatabaseUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MergeDataImpl implements AutoMergeData {
  private final Set<DBAttribute<?>> myConflicts = Collections15.hashSet();
  private final Map<DBAttribute<?>, Object> myResolution = Collections15.hashMap();
  private final Set<DBAttribute<?>> myDiscarded = Collections15.hashSet();
  private final ItemDiff myLocal;
  private final ItemDiff myServer;
  private final MergeHistoryState myHistoryState;

  private MergeDataImpl(ItemDiff local, ItemDiff server, MergeHistoryState historyState) {
    myLocal = local;
    myServer = server;
    myHistoryState = historyState;
    assert local.getItem() == server.getItem();
  }

  public static MergeDataImpl create(@NotNull ItemDiff local, @NotNull ItemDiff server) {
    long item = local.getItem();
    if (item != server.getItem()) Log.error("Different items merged " + local + " " + server);
    MergeDataImpl data = new MergeDataImpl(local, server, MergeHistoryState.load(local));
    Collection<? extends DBAttribute<?>> localChanges = local.getChanged();
    Collection<? extends DBAttribute<?>> serverChanges = server.getChanged();
    data.myConflicts.addAll(localChanges);
    data.myConflicts.retainAll(serverChanges);
    if (!data.myConflicts.contains(SyncSchema.INVISIBLE)
      && local.getNewerVersion().isInvisible() != server.getNewerVersion().isInvisible()) {
      if ((localChanges.contains(SyncSchema.INVISIBLE) && local.getNewerVersion().isInvisible() && !serverChanges.isEmpty())
        || serverChanges.contains(SyncSchema.INVISIBLE) && server.getNewerVersion().isInvisible() && !localChanges.isEmpty())
        data.myConflicts.add(SyncSchema.INVISIBLE);
    }
    for (Iterator<DBAttribute<?>> iterator = data.myConflicts.iterator(); iterator.hasNext();) {
      DBAttribute<?> attribute = iterator.next();
      if (ItemDiffImpl.isEqualNewer(local, server, attribute)) iterator.remove();
    }
    return data;
  }

  public Map<DBAttribute<?>, Object> getResolution() {
    return Collections.unmodifiableMap(myResolution);
  }

  public boolean isConflictResolved() {
    return isResolvedDelete() || myConflicts.isEmpty();
  }

  @Override
  public long getItem() {
    return myLocal.getItem();
  }

  @Override
  public Collection<DBAttribute<?>> getUnresolved() {
    return Collections15.arrayList(myConflicts);
  }

  @Override
  public void discardEdit(DBAttribute<?>... attributes) {
    resolveToVersion(myServer, myLocal, (DBAttribute<Object>[])attributes);
  }

  @Override
  public void resolveToLocal(DBAttribute<?>... attributes) {
    resolveToVersion(myLocal, myServer, (DBAttribute<Object>[]) attributes);
  }

  private <T> void resolveToVersion(ItemDiff source, ItemDiff ignored, DBAttribute<T>[] attributes) {
    for (DBAttribute<T> attribute : attributes) {
      if (!ignored.isChanged(attribute)) continue;
      T serverValue = source.getNewerValue(attribute);
      setResolution(attribute, serverValue);
    }
  }

  @Override
  public void setCompositeResolution(DBAttribute<? extends Collection<? extends Long>> attribute, LongList resolution) {
    if (attribute == null) return;
    if (resolution == null) resolution = LongList.EMPTY;
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    Collection<Long> objResolution;
    switch (composition) {
    case SCALAR: Log.error("Expected composite " + attribute); return;
    case LIST: objResolution = resolution.toList(); break;
    case SET: objResolution = Collections15.hashSet(resolution.toList()); break;
    default: Log.error("Unknown composition " + attribute); return;
    }
    //noinspection unchecked
    setResolution((DBAttribute) attribute, objResolution);
  }

  @Override
  public void setCompositeResolution(DBAttribute<? extends Collection<? extends String>> attribute,
    Collection<String> resolution)
  {
    if (attribute == null) return;
    if (resolution == null) resolution = Collections.emptySet();
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    Collection<String> fixedResolution;
    switch (composition) {
    case SCALAR: Log.error("Expected composite " + attribute); return;
    case SET: fixedResolution = resolution instanceof Set<?> ? resolution : Collections15.hashSet(resolution); break;
    case LIST: fixedResolution = resolution instanceof List<?> ? resolution : Collections15.arrayList(resolution); break;
    default: Log.error("Unknown composition " + attribute); return;
    }
    //noinspection unchecked
    setResolution((DBAttribute) attribute, fixedResolution);
  }

  @Override
  public <T> void setResolution(DBAttribute<T> attribute, T value) {
    T serverNew = myServer.getNewerValue(attribute);
    if (SyncUtils.isEqualValue(getReader(), attribute, serverNew, value)) myDiscarded.add(attribute);
    myResolution.put(attribute, value);
    myConflicts.remove(attribute);
  }

  @Override
  public ItemDiff getLocal() {
    return myLocal;
  }

  @Override
  public ItemDiff getServer() {
    return myServer;
  }

  public boolean isResolvedDelete() {
    if (!Boolean.TRUE.equals(myServer.getNewerValue(SyncSchema.INVISIBLE))) return false;
    if (myLocal.isChanged(SyncSchema.INVISIBLE)) {
      return Boolean.TRUE.equals(myResolution.get(SyncSchema.INVISIBLE));
    } else return Boolean.TRUE.equals(myLocal.getNewerValue(SyncSchema.INVISIBLE));
  }

  public boolean isDiscardEdit() {
    return !hasHistory() && myDiscarded.containsAll(myLocal.getChanged());
  }

  @Override
  public DBReader getReader() {
    return getLocal().getReader();
  }

  @Override
  public String toString() {
    return
      "MergeData {" +
      "\n  server old: " + myServer.getElderVersion().getAllShadowableMap() +
      "\n  server new: " + myServer.getNewerVersion().getAllShadowableMap() +
      "\n  local old: " + myLocal.getElderVersion().getAllShadowableMap() +
      "\n  local new: " + myLocal.getNewerVersion().getAllShadowableMap() +
      "\n}";
  }

  @Override
  public <T> boolean isNewEqual(DBAttribute<T> attribute) {
    if (!getLocal().isChanged(attribute) || !getServer().isChanged(attribute)) return true;
    T local = getLocal().getNewerValue(attribute);
    T server = getServer().getNewerValue(attribute);
    return DatabaseUtil.isEqualValue(attribute, local, server);
  }

  @Override
  public List<HistoryRecord> getHistory() {
    return myHistoryState.getHistory();
  }

  @Override
  public void removeHistoryRecord(int recordId) {
    myHistoryState.removeHistoryRecord(recordId);
  }

  public List<HistoryRecord> getUpdatedHistory() {
    return myHistoryState.getUpdatedHistory();
  }

  public boolean hasHistory() {
    return myHistoryState.hasHistory();
  }
}
