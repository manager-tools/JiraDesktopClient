package com.almworks.items.impl.dbadapter;

import java.util.Arrays;

public class DBIndex {
  private final DBColumn[] myColumns;
  private final boolean[] myDesc;
  private final boolean myUnique;

  public DBIndex(boolean unique, DBColumn[] columns, boolean[] desc) {
    assert columns != null;
    assert desc != null;
    assert columns.length > 0;
    assert desc.length == columns.length : desc.length + " " + columns.length;
    assert checkColumns(columns);
    myUnique = unique;
    myColumns = columns;
    myDesc = desc;
  }

  private static boolean checkColumns(DBColumn[] columns) {
    for (int i = 0; i < columns.length - 1; i++) {
      for (int j = i + 1; j < columns.length; j++) {
        if (columns[i].getName().equals(columns[j].getName())) {
          assert false : columns[i].getName();
        }
      }
    }
    return true;
  }

  public int getColumnCount() {
    return myColumns.length;
  }

  public DBColumn getColumn(int index) {
    return myColumns[index];
  }

  public boolean isDescending(int index) {
    return myDesc[index];
  }

  public boolean isUnique() {
    return myUnique;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    DBIndex dbIndex = (DBIndex) o;

    if (myUnique != dbIndex.myUnique)
      return false;
    if (!Arrays.equals(myColumns, dbIndex.myColumns))
      return false;
    if (!Arrays.equals(myDesc, dbIndex.myDesc))
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = Arrays.hashCode(myColumns);
    result = 31 * result + Arrays.hashCode(myDesc);
    result = 31 * result + (myUnique ? 1 : 0);
    return result;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(myUnique ? "unique(" : "index(");
    for (int i = 0; i < myColumns.length; i++) {
      if (i > 0)
        builder.append(' ');
      builder.append(myColumns[i].getName());
      builder.append(':');
      builder.append(myDesc[i] ? 'D' : 'A');
    }
    builder.append(')');
    return builder.toString();
  }

  public boolean containsAll(DBRow sample) {
    for (DBColumn column : myColumns) {
      int ci = sample.getColumns().indexOf(column);
      if (ci < 0 || sample.getColumnValue(ci) == null)
        return false;
    }
    return true;
  }
}
