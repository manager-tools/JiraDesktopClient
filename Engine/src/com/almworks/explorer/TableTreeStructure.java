package com.almworks.explorer;

import com.almworks.api.application.LoadedItem;
import com.almworks.util.LogHelper;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.components.TreeStructure;

import java.util.Collections;
import java.util.Set;

import static com.almworks.util.collections.Functional.first;

public abstract class TableTreeStructure implements TreeStructure.MultiParent<LoadedItem, Long, TreeModelBridge<LoadedItem>> {
  public Long getNodeKey(LoadedItem element) {
    return element.getItem();
  }

  @Override
  public Long getNodeParentKey(LoadedItem element) {
    LogHelper.error("Shouldn't be there -- getNodeParentKeys should have been called instead.");
    return first(getNodeParentKeys(element));
  }

  @Override
  public TreeModelBridge<LoadedItem> createTreeNode(LoadedItem element) {
    return TreeModelBridge.create(element);
  }

  public static class FlatTree extends TableTreeStructure {
    public static final TableTreeStructure INSTANCE = new FlatTree();

    @Override
    public Set<Long> getNodeParentKeys(LoadedItem element) {
      return Collections.emptySet();
    }
  }

}
