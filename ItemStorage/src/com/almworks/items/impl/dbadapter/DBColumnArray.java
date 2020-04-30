package com.almworks.items.impl.dbadapter;


public class DBColumnArray extends DBColumnSet {
  private final DBColumn[] myColumns;

  DBColumnArray(DBColumn[] columns) {
    assert columns != null;
    myColumns = columns;
  }

  public final int size() {
    return myColumns.length;
  }

  public final DBColumn get(int index) {
    return myColumns[index];
  }
}
