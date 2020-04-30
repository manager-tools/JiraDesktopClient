package com.almworks.items.impl.sqlite;

import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.threads.ThreadSafe;
import org.jetbrains.annotations.NotNull;

/**
 * Manages creation of filters for items.
 * @author igor baltiyskiy
 */

interface OldFilterManager {
  /**
   * Retrieves an item source that filters items according to the specified constraint. The filter will receive notifications about transactions from the specified query processor.
   * If the filter is already attached to that processor, it also attaches to the specified query processor.
   * @param constraint The constraint on the items in the item source
   * @return Instance of an item source that filters items according to the specified constraint and is attached to the specified query processor
   */
  @NotNull
  @ThreadSafe
  FilteringItemSource getFilter(BoolExpr<DP> constraint);
}
