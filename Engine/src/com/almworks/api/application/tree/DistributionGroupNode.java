package com.almworks.api.application.tree;

import com.almworks.api.application.ItemKeyGroup;
import org.jetbrains.annotations.Nullable;

public interface DistributionGroupNode extends GenericNode {
  @Nullable
  ItemKeyGroup getGroup();
}
