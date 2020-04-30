package com.almworks.items.sync.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.AttributeMap;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link ItemVersion}. Overrides current values of prevVersion with specified values. Assumes that
 * the specified map provides all values for shadowable attributes and don't provide values for not shadowable attributes.
 */
public class MapItemVersion extends ItemVersionCommonImpl {
  private final AttributeMap myValues;
  private final ItemVersion myPrevVersion;

  public MapItemVersion(AttributeMap values, ItemVersion prevVersion) {
    myValues = values;
    myPrevVersion = prevVersion;
  }

  @Override
  public long getItem() {
    return myPrevVersion.getItem();
  }

  @Override
  public <T> T getValue(DBAttribute<T> attribute) {
    return SyncSchema.hasShadowableValue(getReader(), attribute) ? myValues.get(attribute) : myPrevVersion.getValue(attribute);
  }

  @Override
  public AttributeMap getAllShadowableMap() {
    return myValues.copy();
  }

  @Override
  public AttributeMap getAllValues() {
    AttributeMap map = myPrevVersion.getAllValues();
    for (DBAttribute<?> attribute : Collections15.arrayList(map.keySet())) {
      if (SyncSchema.hasShadowableValue(getReader(), attribute)) map.put(attribute, null);
    }
    map.putAll(myValues);
    return map;
  }

  @Override
  public long getIcn() {
    return myPrevVersion.getIcn();
  }

  @Override
  public LongList getSlavesRecursive() {
    return myPrevVersion.getSlavesRecursive();
  }

  @NotNull
  @Override
  public SyncState getSyncState() {
    return myPrevVersion.getSyncState();
  }

  @NotNull
  @Override
  public ItemVersion readTrunk(long item) {
    return myPrevVersion.readTrunk(item);
  }

  @NotNull
  @Override
  public ItemVersion switchToServer() {
    return myPrevVersion.switchToServer();
  }

  @NotNull
  @Override
  public ItemVersion switchToTrunk() {
    return myPrevVersion.switchToTrunk();
  }

  @NotNull
  @Override
  public DBReader getReader() {
    return myPrevVersion.getReader();
  }

  @NotNull
  @Override
  public ItemVersion forItem(long item) {
    if (item == getItem()) return this;
    return myPrevVersion.forItem(item);
  }
}
