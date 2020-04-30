package com.almworks.items.sync.util.merge;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.util.SyncAttributes;

import java.util.Collection;

/**
 * Discard all edit, except create/delete. This algorithm is intended to use with items which are submit-only.<br>
 * If the item happens to be changed after successful submit (in case of edit during submit) the user changes are
 * discarded since there is no way to upload it.
 */
public class DiscardAll extends SimpleAutoMerge {
  @Override
  public void resolve(AutoMergeData data) {
    Collection<DBAttribute<?>> unresolved = data.getUnresolved();
    for (DBAttribute<?> attribute : unresolved)
      if (!SyncAttributes.INVISIBLE.equals(attribute))
        data.discardEdit(attribute);
  }
}
