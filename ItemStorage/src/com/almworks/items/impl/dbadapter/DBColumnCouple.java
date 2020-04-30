package com.almworks.items.impl.dbadapter;

public class DBColumnCouple extends DBColumnSet {
  protected final DBColumn myColumn1;
  protected final DBColumn myColumn2;

  public DBColumnCouple(DBColumn column1, DBColumn column2) {
    myColumn1 = column1;
    myColumn2 = column2;
  }

  public final int size() {
      return 2;
  }

  public DBColumn get(int index) {
    switch (index) {
    case 0:
      return myColumn1;
    case 1:
      return myColumn2;
    default:
      assert false : index;
      return null;
    }
  }

  public boolean contains(DBColumn column) {
    return myColumn1.equals(column) || myColumn2.equals(column);
  }

  public DBColumn getColumn1() {
    return myColumn1;
  }

  public DBColumn getColumn2() {
    return myColumn2;
  }
}