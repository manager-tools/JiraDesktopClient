package com.almworks.itemsync;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.sync.ItemAutoMerge;

import java.util.Collection;

public interface MergeOperationsManager {
  void addMergeOperation(ItemAutoMerge operations, DBItemType... types);

  Builder buildOperation(DBItemType type);

  interface Builder {
    void finish();

    void uniteSetValues(DBAttribute<? extends Collection<? extends Long>> ... attributes);

    Builder discardEdit(DBAttribute<?>... attributes);

    Builder addCustom(ItemAutoMerge merge);

    Builder mergeLongSets(DBAttribute<? extends Collection<? extends Long>>... attributes);

    void mergeStringSets(DBAttribute<? extends Collection<? extends String>> attribute);

    Builder addConflictGroup(DBAttribute<?>... attrGroup);
  }

  interface MergeProvider {
    void registerMergeOperations(MergeOperationsManager manager);
  }
}
