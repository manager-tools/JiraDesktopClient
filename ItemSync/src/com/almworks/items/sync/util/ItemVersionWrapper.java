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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class ItemVersionWrapper implements ItemVersion {
  private final VersionSource mySource;
  private final ItemVersion myDelegate;

  public ItemVersionWrapper(VersionSource source, ItemVersion delegate) {
    mySource = source;
    myDelegate = delegate;
  }

  @Override
  public ItemVersion readValue(DBAttribute<Long> attribute) {
    Long item = getValue(attribute);
    return item != null ? forItem(item) : null;
  }

  @Override
  @NotNull
  public DBReader getReader() {
    return mySource.getReader();
  }

  @Override
  @NotNull
  public ItemVersion forItem(long item) {
    return mySource.forItem(item);
  }

  @Override
  @NotNull
  public ItemVersion forItem(DBIdentifiedObject object) {
    return mySource.forItem(object);
  }

  @Override
  public long findMaterialized(DBIdentifiedObject object) {
    return mySource.findMaterialized(object);
  }

  @Override
  @NotNull
  public List<ItemVersion> readItems(LongList items) {
    return mySource.readItems(items);
  }

  @Override
  public <T> List<T> collectValues(DBAttribute<T> attribute, LongList items) {
    return mySource.collectValues(attribute, items);
  }

  @Override
  @NotNull
  public ItemVersion switchToTrunk() {
    return myDelegate.switchToTrunk();
  }

  @Override
  public long getItem() {
    return myDelegate.getItem();
  }

  @Override
  @Nullable
  public <T> T getValue(DBAttribute<T> attribute) {
    return myDelegate.getValue(attribute);
  }

  @Override
  public <T> T getNNValue(DBAttribute<T> attribute, T nullValue) {
    return myDelegate.getNNValue(attribute, nullValue);
  }

  @Override
  public AttributeMap getAllShadowableMap() {
    return myDelegate.getAllShadowableMap();
  }

  @Override
  public AttributeMap getAllValues() {
    return myDelegate.getAllValues();
  }

  @Override
  public long getIcn() {
    return myDelegate.getIcn();
  }

  @Override
  public LongList getSlavesRecursive() {
    return myDelegate.getSlavesRecursive();
  }

  @Override
  @NotNull
  public LongList getLongSet(DBAttribute<? extends Collection<? extends Long>> attribute) {
    return myDelegate.getLongSet(attribute);
  }

  @Override
  @NotNull
  public LongArray getSlaves(DBAttribute<Long> masterReference) {
    return myDelegate.getSlaves(masterReference);
  }

  @NotNull
  @Override
  public SyncState getSyncState() {
    return myDelegate.getSyncState();
  }

  @Override
  public boolean isInvisible() {
    return myDelegate.isInvisible();
  }

  @Override
  public boolean isAlive() {
    return myDelegate.isAlive();
  }

  @Override
  @NotNull
  public ItemVersion readTrunk(long item) {
    return myDelegate.readTrunk(item);
  }

  @Override
  public boolean equalValue(DBAttribute<Long> attribute, DBIdentifiedObject object) {
    return myDelegate.equalValue(attribute, object);
  }

  @Override
  public <T> T mapValue(DBAttribute<Long> attribute, Map<? extends DBIdentifiedObject, T> map) {
    return myDelegate.mapValue(attribute, map);
  }

  @NotNull
  @Override
  public ItemVersion switchToServer() {
    return myDelegate.switchToServer();
  }

  @NotNull
  @Override
  public HistoryRecord[] getHistory() {
    return myDelegate.getHistory();
  }

  @Override
  public boolean equalItem(ItemReference reference) {
    return myDelegate.equalItem(reference);
  }
}
