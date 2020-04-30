package com.almworks.items.sync;

import com.almworks.items.api.DBReader;

public interface ItemAutoMerge {
  void preProcess(ModifiableDiff local);

  void resolve(AutoMergeData data);

  interface Selector {
    ItemAutoMerge getOperations(DBReader reader, long item);
  }
}
