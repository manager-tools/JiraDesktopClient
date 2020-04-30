package com.almworks.items.impl.dbadapter;

public class DBColumnSingle extends DBColumnSet {
  private final DBColumn myColumn;

  public DBColumnSingle(DBColumn column) {
    assert column != null;
    myColumn = column;
  }

  public final int size() {
    return 1;
  }

  public final DBColumn get(int index) {
    assert index == 0;
    return index == 0 ? myColumn : null;
  }

  public final DBColumn findByName(String name) {
    return myColumn.getName().equals(name) ? myColumn : null;
  }

  public final boolean contains(DBColumn column) {
    return myColumn == column;
  }

  public final int indexOf(DBColumn column) {
    return myColumn == column ? 0 : -1;
  }

  public DBColumn getColumn() {
    return myColumn;
  }
}
