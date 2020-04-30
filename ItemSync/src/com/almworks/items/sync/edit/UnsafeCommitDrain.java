package com.almworks.items.sync.edit;

import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.AttributeMap;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;

class UnsafeCommitDrain extends BaseEditDrain {
  UnsafeCommitDrain(SyncManagerImpl manager, EditCommit commit) {
    super(manager, null, commit);
  }

  @Override
  public ItemVersionCreator changeItem(long item) {
    return unsafeChange(item);
  }

  @NotNull
  @Override
  protected TLongObjectHashMap<AttributeMap> getBases() {
    return new TLongObjectHashMap<>();
  }
}
