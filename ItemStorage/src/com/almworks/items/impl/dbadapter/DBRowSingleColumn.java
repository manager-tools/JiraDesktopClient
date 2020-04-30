package com.almworks.items.impl.dbadapter;

public class DBRowSingleColumn implements DBRow {
  private final DBColumnSingle myColumn;
  private Object myValue;

  public DBRowSingleColumn(DBColumn column) {
    this(new DBColumnSingle(column));
  }

  public DBRowSingleColumn(DBColumnSingle column) {
    myColumn = column;
  }

  public <T> DBRowSingleColumn setValue(DBColumn<T> column, T value) {
    if (!myColumn.getColumn().equals(column)) {
      assert false : column;
      return this;
    }
    myValue = value;
    return this;
  }

  public <T> T getValue(DBColumn<T> column) {
    if (!myColumn.getColumn().equals(column)) {
      assert false : column;
      return null;
    }
    return (T) myValue;
  }

  public final <T> DBRow with(DBColumn<T> column, T value) {
    if (myColumn.getColumn().equals(column))
      return new DBRowSingleColumn(myColumn).setValue(column, value);
    return DBRowUtil.create(myColumn.getColumn(), myValue, column, value);
  }

  public final Object getColumnValue(int columnIndex) {
    assert columnIndex == 0 : columnIndex;
    return columnIndex == 0 ? myValue : null;
  }

  public final <T1, T2> DBRow with(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2) {
    if (column1.equals(column2))
      return with(column2, value2);
    DBColumn c = myColumn.getColumn();
    if (c.equals(column1) || c.equals(column2))
      return DBRowUtil.create(column1, value1, column2, value2);
    return DBRowUtil.create(myColumn.getColumn(), myValue, column1, value1, column2, value2);
  }

  public final DBColumnSingle getColumns() {
    return myColumn;
  }

  protected void setValue(Object value) {
    myValue = value;
  }

  protected Object getValue() {
    return myValue;
  }
}
