package com.almworks.api.engine;

import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface EngineViews {
  DBFilter getItemsOfType(DBItemType type);

  BoolExpr<DP> getPrimaryItemsFilter();

  BoolExpr<DP> getLocalChangesFilter();

  BoolExpr<DP> getConflictsFilter();
}
