package com.almworks.items.sync.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ItemReference;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.AttributeMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public class IllegalItem extends BasicVersionSource implements ItemVersion {
  private final VersionSource mySource;
  private final long myItem;

  public IllegalItem(VersionSource source, long item) {
    assert source != null;
    assert item <= 0 : item;
    mySource = source;
    myItem = item;
  }

  @NotNull
  @Override
  public ItemVersion forItem(long item) {
    return mySource.forItem(item);
  }

  @Override
  public boolean equalValue(DBAttribute<Long> attribute, DBIdentifiedObject object) {
    Long value = getValue(attribute);
    if (value == null || value <= 0) return false;
    long materialized = getReader().findMaterialized(object);
    return materialized == value;
  }

  @Override
  public <T> T getNNValue(DBAttribute<T> attribute, T nullValue) {
    T value = getValue(attribute);
    return value != null ? value : nullValue;
  }

  @Override
  public long getItem() {
    return myItem;
  }

  @Override
  public <T> T getValue(DBAttribute<T> attribute) {
    return null;
  }

  @Override
  public AttributeMap getAllShadowableMap() {
    return new AttributeMap();
  }

  @Override
  public AttributeMap getAllValues() {
    return new AttributeMap();
  }

  @Override
  public long getIcn() {
    return 0;
  }

  @Override
  public LongList getSlavesRecursive() {
    return LongList.EMPTY;
  }

  @NotNull
  @Override
  public LongList getLongSet(DBAttribute<? extends Collection<? extends Long>> attribute) {
    return LongList.EMPTY;
  }

  @NotNull
  @Override
  public LongArray getSlaves(DBAttribute<Long> masterReference) {
    return new LongArray();
  }

  @NotNull
  @Override
  public SyncState getSyncState() {
    return SyncState.SYNC;
  }

  @Override
  public boolean isInvisible() {
    return false;
  }

  @Override
  public boolean isAlive() {
    return false;
  }

  @NotNull
  @Override
  public ItemVersion readTrunk(long item) {
    return SyncUtils.readTrunk(getReader(), item);
  }

  @Override
  public <T> T mapValue(DBAttribute<Long> attribute, Map<? extends DBIdentifiedObject, T> map) {
    return null;
  }

  @Override
  public ItemVersion readValue(DBAttribute<Long> attribute) {
    return null;
  }

  @Override
  @NotNull
  public ItemVersion switchToServer() {
    BranchSource source = BranchSource.server(getReader());
    return source == mySource ? this : new IllegalItem(source, myItem);
  }

  @NotNull
  @Override
  public ItemVersion switchToTrunk() {
    BranchSource source = BranchSource.trunk(getReader());
    return source == mySource ? this : new IllegalItem(source, myItem);
  }

  @Override
  @NotNull
  public DBReader getReader() {
    return mySource.getReader();
  }

  @NotNull
  @Override
  public HistoryRecord[] getHistory() {
    return HistoryRecord.EMPTY_ARRAY;
  }

  @Override
  public boolean equalItem(ItemReference reference) {
    return false;
  }
}
