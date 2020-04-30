package com.almworks.items.impl.dbadapter;

public class DBRowTwoColumns implements DBRow {
  private final DBColumnCouple myColumns;
  private Object myValue1;
  private Object myValue2;

  public DBRowTwoColumns(DBColumn column1, Object value1, DBColumn column2, Object value2) {
    myColumns = new DBColumnCouple(column1, column2);
    myValue1 = value1;
    myValue2 = value2;
  }

  public DBRowTwoColumns(DBColumnCouple columns, Object value1, Object value2) {
    myColumns = columns;
    myValue1 = value1;
    myValue2 = value2;
  }

  public DBColumnSet getColumns() {
    return myColumns;
  }


  public <T> DBRow setValue(DBColumn<T> column, T value) {
    if (myColumns.getColumn1().equals(column))
      myValue1 = value;
    else if (myColumns.getColumn2().equals(column))
      myValue2 = value;
    else
      assert false : column;
    return this;
  }

  public <T> T getValue(DBColumn<T> column) {
    if (myColumns.getColumn1().equals(column))
      return (T) myValue1;
    if (myColumns.getColumn2().equals(column))
      return (T) myValue2;
    assert false : column;
    return null;
  }

  public <T> DBRow with(DBColumn<T> column, T value) {
    if (myColumns.contains(column))
      return new DBRowTwoColumns(myColumns, myValue1, myValue2).setValue(column, value);
    return DBRowUtil.create(myColumns.getColumn1(), myValue1, myColumns.getColumn2(), myValue2, column, value);
  }

  public Object getColumnValue(int columnIndex) {
    switch (columnIndex) {
    case 0:
      return myValue1;
    case 1:
      return myValue2;
    default:
      assert false : columnIndex;
      return null;
    }
  }

  public <T1, T2> DBRow with(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2) {
    if (column1.equals(column2))
      return with(column2, value2);
    boolean has1 = myColumns.contains(column1);
    boolean has2 = myColumns.contains(column2);
    if (has1) {
      if (has2) {
        return new DBRowTwoColumns(column1, value1, column2, value2);
      } else {
        return DBRowUtil.create(column1, value1, myColumns.getColumn2(), myValue2, column2, value2);
      }
    } else {
      if (has2) {
        return DBRowUtil.create(myColumns.getColumn1(), myValue1, column2, value2, column2, value2);
      } else {
        DBRow result = DBRowUtil.columns(myColumns.getColumn1(), myColumns.getColumn2(), column1, column2);
        result.setValue(myColumns.getColumn1(), myValue1);
        result.setValue(myColumns.getColumn2(), myValue2);
        result.setValue(column1, value1);
        result.setValue(column2, value2);
        return result;
      }
    }
  }
}
