package com.almworks.items.impl.dbadapter;

import org.jetbrains.annotations.NotNull;

public class DBAnyTypeColumn extends BaseObjectColumn<Object> {
  public DBAnyTypeColumn(String name) {
    super(name);
  }

  public Object toUserValue(Object value) {
    return value;
  }

  @NotNull
  public DBColumnType getDatabaseClass() {
    return DBColumnType.ANY;
  }
}
