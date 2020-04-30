package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.sqlite4java.SQLiteException;

public interface TableResolver {
  /**
   * Gets physical table name, possibly creating it if it doesn't exist.
   *
   * @param definition table definition
   * @param allowCreate if true and there's no such table in the db, a table will be created
   *
   * @return table name which can be used in SQL, null if table not exists and was not requested to be created
   */
  String getTableName(DBTable definition, boolean allowCreate) throws SQLiteException;
}
