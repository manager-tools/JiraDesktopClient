package com.almworks.items.sync.edit;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.impl.Branch;
import com.almworks.items.sync.impl.BranchUtil;
import com.almworks.items.sync.impl.HolderCache;
import com.almworks.items.util.AttributeMap;

abstract class BaseDownloadDrain extends BaseDBDrain {
  BaseDownloadDrain(SyncManagerImpl manager) {
    super(manager, Branch.SERVER);
  }

  @Override
  public ItemVersionCreator changeItem(long item) {
    DBReader reader = getReader();
    BranchUtil.Write util = BranchUtil.instance(getWriter());
    ItemVersion existing = util.getExisting(item, Branch.SERVER);
    if (existing instanceof ItemVersionCreator) return (ItemVersionCreator) existing;
    AttributeMap upload = HolderCache.instance(reader).getUploadTask(item);
    if (upload != null) return util.dummyCreator(item, this);
    return super.changeItem(item);
  }
}
