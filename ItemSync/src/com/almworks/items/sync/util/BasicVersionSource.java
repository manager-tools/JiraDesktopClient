package com.almworks.items.sync.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author dyoma
 */
public abstract class BasicVersionSource implements VersionSource {
  @NotNull
  @Override
  public ItemVersion forItem(DBIdentifiedObject object) {
    long item = findMaterialized(object);
    return item > 0 ? forItem(item) : new IllegalItem(this, item);
  }

  @Override
  public long findMaterialized(DBIdentifiedObject object) {
    return getReader().findMaterialized(object);
  }

  @NotNull
  @Override
  public List<ItemVersion> readItems(LongList items) {
    return VersionWriterUtil.readItems(this, items);
  }

  @Override
  public <T> List<T> collectValues(DBAttribute<T> attribute, LongList items) {
    return VersionWriterUtil.collectValues(this, attribute, items);
  }
}
