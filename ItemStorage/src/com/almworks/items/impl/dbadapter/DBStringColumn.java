package com.almworks.items.impl.dbadapter;

import org.jetbrains.annotations.NotNull;

public class DBStringColumn extends BaseObjectColumn<String> {
  public DBStringColumn(String name) {
    super(name);
  }

  @NotNull
  public DBColumnType getDatabaseClass() {
    return DBColumnType.TEXT;
  }

  public String toUserValue(Object databaseValue) {
    return databaseValue == null ? null : String.valueOf(databaseValue);
  }
}