package com.almworks.sumtable;

import com.almworks.api.application.tree.QueryResult;
import com.almworks.items.api.Database;

public interface SummaryTableQueryExecutor {
  void runQuery(QueryResult queryResult, STFilter counter, STFilter column, STFilter row, Integer count, boolean newTab);

  Database getDatabase();
}
