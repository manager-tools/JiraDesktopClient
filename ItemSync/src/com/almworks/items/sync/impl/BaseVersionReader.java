package com.almworks.items.sync.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.ItemReference;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.BasicVersionSource;
import com.almworks.items.sync.util.ItemVersionCommonImpl;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.sync.util.VersionWriterUtil;
import com.almworks.items.util.SyncAttributes;
import org.jetbrains.annotations.NotNull;

public abstract class BaseVersionReader extends BasicVersionSource implements ItemVersion {
  @NotNull
  public ItemVersion forItem(long item) {
    return BranchUtil.instance(getReader()).readItem(item, getBranch());
  }

  protected abstract Branch getBranch();

  @Override
  public boolean equalValue(DBAttribute<Long> attribute, DBIdentifiedObject object) {
    return SyncUtils.equalValue(getReader(), getValue(attribute), object);
  }

  @Override
  public <T> T getNNValue(DBAttribute<T> attribute, T nullValue) {
    return VersionWriterUtil.getNNValue(this, attribute, nullValue);
  }

  @Override
  public String toString() {
    return "VersionReader " + toStringItemInfo();
  }

  @Override
  public boolean isAlive() {
    return !isInvisible() && Boolean.TRUE.equals(getValue(SyncAttributes.EXISTING));
  }

  protected String toStringItemInfo() {
    long item = getItem();
    StringBuilder builder = new StringBuilder();
    builder.append("item=").append(item);
    if (item > 0) {
      Long type = DBAttribute.TYPE.getValue(item, getReader());
      builder.append(" type=").append(type);
      if (type != null && type > 0) builder.append(" (").append(DBAttribute.ID.getValue(type, getReader())).append(")");
    }
    return builder.toString();
  }

  @NotNull
  @Override
  public HistoryRecord[] getHistory() {
    return ItemVersionCommonImpl.getHistory(this);
  }

  @Override
  public boolean equalItem(ItemReference reference) {
    return ItemVersionCommonImpl.equalItem(getReader(), getItem(), reference);
  }
}
