package com.almworks.items.api;

import com.almworks.integers.LongList;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.bool.BoolExpr;

public abstract class DBTrigger extends DBIdentifiedObject {
  private final static DBNamespace NS = Database.NS.subNs("trigger");
  public static final DBAttribute<Long> TRIGGER_UPDATE_ICN = NS.longAttr("icn", "Trigger Update ICN", false);
  public static final DBAttribute<LongList> LAST_RESULT_SET = NS.longList("resultSet", "Last Result Set");

  private final BoolExpr<DP> myExpr;
  
  public DBTrigger(String id, BoolExpr<DP> expr) {
    super(id);
    if (expr == null) throw new NullPointerException();
    myExpr = expr;
  }

  public BoolExpr<DP> getExpr() {
    return myExpr;
  }

  public abstract void apply(LongList itemsSorted, DBWriter writer);
}
