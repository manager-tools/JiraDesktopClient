package com.almworks.items.sync.impl;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.util.ItemVersionCommonImpl;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

abstract class VersionReader extends BaseVersionReader {
  public abstract VersionHolder getHolder();

  public DBAttribute<AttributeMap> getShadow() {
    return getHolder().getShadow();
  }

  @Override
  @NotNull
  public DBReader getReader() {
    return getHolder().getReader();
  }

  @Override
  public <T> T getValue(DBAttribute<T> attribute) {
    if (attribute == null) return null;
    return getHolder().getValue(attribute);
  }

  @Override
  public AttributeMap getAllShadowableMap() {
    return getHolder().getAllShadowableMap();
  }

  @Override
  public AttributeMap getAllValues() {
    return getHolder().getAllValues();
  }

  @Override
  public long getIcn() {
    return getReader().getItemIcn(getItem());
  }

  protected BranchUtil getUtil() {
    return BranchUtil.instance(getReader());
  }

  @Override
  public LongList getSlavesRecursive() {
    return SyncUtils.getSlavesSubtree(getReader(), getItem());
  }

  @NotNull
  @Override
  public LongList getLongSet(DBAttribute<? extends Collection<? extends Long>> attribute) {
    return ItemVersionCommonImpl.getLongSet(getValue(attribute));
  }

  @NotNull
  @Override
  public LongArray getSlaves(DBAttribute<Long> masterReference) {
    return ItemVersionCommonImpl.getSlaves(getReader(), getItem(), masterReference);
  }

  @NotNull
  @Override
  public SyncState getSyncState() {
    HolderCache holders = HolderCache.instance(getReader());
    AttributeMap base = holders.getBase(getItem());
    if (base == null) return SyncState.SYNC;
    if (SyncSchema.isInvisible(base)) return SyncState.NEW;
    boolean deleted = SyncUtils.isTrunkInvisible(getReader(), getItem());
    AttributeMap conflict = holders.getConflict(getItem());
    if (conflict == null) return deleted ? SyncState.LOCAL_DELETE : SyncState.EDITED;
    boolean remoteDelete = SyncSchema.isInvisible(conflict);
    if (remoteDelete) return SyncState.MODIFIED_CORPSE;
    return deleted ? SyncState.DELETE_MODIFIED : SyncState.CONFLICT;
  }

  @Override
  public boolean isInvisible() {
    return ItemVersionCommonImpl.isInvisible(getValue(SyncSchema.INVISIBLE));
  }

  @NotNull
  @Override
  public ItemVersion readTrunk(long item) {
    return getUtil().readItem(item, false);
  }

  @Override
  public <T> T mapValue(DBAttribute<Long> attribute, Map<? extends DBIdentifiedObject, T> map) {
    return SyncUtils.mapValue(getReader(), getValue(attribute), map);
  }

  @Override
  public ItemVersion readValue(DBAttribute<Long> attribute) {
    Long value = getValue(attribute);
    return value != null ? forItem(value) : null;
  }

  @NotNull
  @Override
  public ItemVersion switchToServer() {
    return SyncUtils.readServer(getReader(), getItem());
  }

  @NotNull
  @Override
  public ItemVersion switchToTrunk() {
    Branch branch = getBranch();
    return branch == Branch.TRUNK ? this : SyncUtils.readTrunk(getReader(), getItem());
  }
}
