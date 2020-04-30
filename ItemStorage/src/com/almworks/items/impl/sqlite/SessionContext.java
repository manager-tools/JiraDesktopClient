package com.almworks.items.impl.sqlite;

import org.almworks.util.Collections15;

import java.util.Map;

public class SessionContext {
  private final DatabaseContext myDatabaseContext;
  private final Map mySessionCache = Collections15.hashMap();

  public SessionContext(DatabaseContext databaseContext) {
    myDatabaseContext = databaseContext;
  }

  public DatabaseContext getDatabaseContext() {
    return myDatabaseContext;
  }

  public Map getSessionCache() {
    return mySessionCache;
  }

  public void clear() {
    mySessionCache.clear();
  }
}
