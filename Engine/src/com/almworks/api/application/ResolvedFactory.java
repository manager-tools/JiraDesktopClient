package com.almworks.api.application;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public interface ResolvedFactory<T extends ResolvedItem> {
  public T createResolvedItem(long item, DBReader reader, ItemKeyCache cache) throws BadItemException;

  class DefaultResolvedFactory implements ResolvedFactory<ResolvedItem> {
    private final DBAttribute<String> myNameAttr;
    @Nullable
    private final DBAttribute<String> myUniqueIdAttr;

    public DefaultResolvedFactory(@NotNull DBAttribute<String> nameAttr, @Nullable DBAttribute<String> uniqueIdAttr) {
      myUniqueIdAttr = uniqueIdAttr;
      myNameAttr = nameAttr;
    }

    public ResolvedItem createResolvedItem(long item, DBReader reader, ItemKeyCache cache) {
      ItemVersion trunk = SyncUtils.readTrunk(reader, item);
      String text = trunk.getValue(myNameAttr);
      String uniqueId = myUniqueIdAttr != null ? trunk.getValue(myUniqueIdAttr) : null;
      if (text == null) text = uniqueId;
      ResolvedItem resolved = text != null ? ResolvedItem.create(trunk, text, uniqueId) : null;
      if (resolved == null) {
        assert false : item;
        Log.debug("null key for " + item);
      }
      return resolved;
    }
  }
}
