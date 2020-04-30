package com.almworks.items.api;

import com.almworks.util.bool.BoolExpr;
import org.almworks.util.detach.Lifespan;

public class DBFilter {
  private final Database myDatabase;
  private final BoolExpr<DP> myFilter;

  public DBFilter(Database database, BoolExpr<DP> filter) {
    myDatabase = database;
    myFilter = filter;
  }

  public Database getDatabase() {
    return myDatabase;
  }

  public DBLiveQuery liveQuery(final Lifespan life, final DBLiveQuery.Listener listener) {
    return myDatabase.liveQuery(life, myFilter, listener);
  }

  public DBFilter filter(BoolExpr<DP> expr) {
    return new DBFilter(myDatabase, myFilter.and(expr));
  }

  public DBQuery query(DBReader reader) {
    return reader.query(myFilter);
  }

  public BoolExpr<DP> getExpr() {
    return myFilter;
  }
}
