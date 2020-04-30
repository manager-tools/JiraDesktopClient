package com.almworks.api.engine;

import com.almworks.integers.LongList;
import com.almworks.items.api.DP;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.bool.BoolExpr;
import org.jetbrains.annotations.NotNull;

public interface PrimaryItemStructure {
  @NotNull
  BoolExpr<DP> getPrimaryItemsFilter();

  @NotNull
  BoolExpr<DP> getLocallyChangedFilter();

  @NotNull
  BoolExpr<DP> getConflictingItemsFilter();

  @NotNull
  BoolExpr<DP> getUploadableItemsFilter();

  @NotNull
  LongList loadEditableSlaves(ItemVersion primary);
}
