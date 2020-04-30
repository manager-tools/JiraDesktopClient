package com.almworks.explorer.loader;

import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.properties.PropertyMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Do not store it for long, it contains DB transaction.
 */
public class ItemUpdateEvent {
  private final ItemModelRegistry myRegistry;
  private final long myItem;
  private PropertyMap myValues = null;
  /**
   * Last item ICN on the moment of event creation. Used to quick-check if event is still valid.
   */
  private final long myIcn;
  @NotNull
  private final DBReader myReader;

  public ItemUpdateEvent(ItemModelRegistry registry, long item, @NotNull DBReader reader) {
    myRegistry = registry;
    myItem = item;
    myReader = reader;
    myIcn = reader.getItemIcn(item);
  }

  @Nullable
  public PropertyMap extractValues() {
    synchronized (this) {
      if (myValues == null) {
        ItemVersion local = SyncUtils.readTrunk(myReader, myItem);
        myValues = myRegistry.extractValues(local);
      }
      return myValues;
    }
  }

  public long getItem() {
    return myItem;
  }

  public long getIcn() {
    return myIcn;
  }

  @NotNull
  public DBReader getReader() {
    return myReader;
  }
}