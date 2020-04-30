package com.almworks.items.sync.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ItemReference;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.collections.LongSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Base implementation of {@link ItemVersion}. Implements utility shortcut methods - which can be implemented via other methods
 * of {@link ItemVersion} interface
 */
public abstract class ItemVersionCommonImpl extends BasicVersionSource implements ItemVersion {
  @Override
  public boolean equalValue(DBAttribute<Long> attribute, DBIdentifiedObject object) {
    return SyncUtils.equalValue(getReader(), getValue(attribute), object);
  }

  @Override
  public <T> T getNNValue(DBAttribute<T> attribute, T nullValue) {
    T value = getValue(attribute);
    return value != null ? value : nullValue;
  }

  @NotNull
  @Override
  public LongList getLongSet(DBAttribute<? extends Collection<? extends Long>> attribute) {
    return getLongSet(getValue(attribute));
  }

  @NotNull
  @Override
  public LongArray getSlaves(DBAttribute<Long> masterReference) {
    return getSlaves(getReader(), getItem(), masterReference);
  }

  @Override
  public boolean isInvisible() {
    return isInvisible(getValue(SyncSchema.INVISIBLE));
  }

  @Override
  public boolean isAlive() {
    return !isInvisible() && Boolean.TRUE.equals(getValue(SyncAttributes.EXISTING));
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
  public HistoryRecord[] getHistory() {
    return getHistory(this);
  }

  @Override
  public boolean equalItem(ItemReference reference) {
    return equalItem(getReader(), getItem(), reference);
  }

  public static boolean equalItem(DBReader reader, long item, ItemReference reference) {
    if (reference == null) return false;
    long referred = reference.findItem(reader);
    return referred >= 0 && item == referred;
  }

  @NotNull
  public static HistoryRecord[] getHistory(ItemVersion version) {
    HistoryRecord[] records = HistoryRecord.restore(version.getValue(SyncAttributes.CHANGE_HISTORY));
    return records != null ? records : HistoryRecord.EMPTY_ARRAY;
  }

  public static LongList getLongSet(Collection<? extends Long> list) {
    if (list == null || list.isEmpty()) return LongList.EMPTY;
    LongSet result = new LongSet(list.size());
    for (Long aLong : list) if (aLong != null) result.add(aLong);
    return result;
  }

  public static LongArray getSlaves(DBReader reader, long master, DBAttribute<Long> masterReference) {
    return reader.query(DPEquals.create(masterReference, master)).copyItemsSorted();
  }

  public static boolean isInvisible(Boolean value) {
    return value != null && value;
  }
}
