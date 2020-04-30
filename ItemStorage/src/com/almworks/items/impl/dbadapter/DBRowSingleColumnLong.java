package com.almworks.items.impl.dbadapter;

public final class DBRowSingleColumnLong implements DBRow  {
  private final DBColumnSingle myColumn;
  private Long myObject;
  private long myValue;

  public DBRowSingleColumnLong(DBColumn column) {
    myColumn = new DBColumnSingle(column);
  }

  public DBRowSingleColumnLong(DBColumnSingle column) {
    myColumn = column;
  }

  public DBRowSingleColumnLong(DBColumn column, long value) {
    myColumn = new DBColumnSingle(column);
    myValue = value;
  }

  public <T> DBRow setValue(DBColumn<T> column, T value) {
    if (!myColumn.getColumn().equals(column)) {
      assert false;
      return this;
    }
    try {
      Long longValue = (Long) value;
      myValue = longValue != null ? longValue : 0;
      myObject = longValue;
    } catch (ClassCastException e) {
      assert false : value + " " + column;
    } catch (NullPointerException e) {
      assert false : "null";
    }
    return this;
  }

  public <T> T getValue(DBColumn<T> column) {
    if (!myColumn.getColumn().equals(column)) {
      assert false;
      return null;
    }
    return (T) getObjectValue();
  }

  public long getLongValue() {
    return myValue;
  }

  private Long getObjectValue() {
    Long v = myObject;
    if (v == null) {
      myObject = v = myValue;
    }
    return v;
  }

  public <T> DBRow with(DBColumn<T> column, T value) {
    if (myColumn.getColumn().equals(column))
      return new DBRowSingleColumnLong(myColumn).setValue(column, value);
    return DBRowUtil.create(myColumn.getColumn(), getObjectValue(), column, value);
  }

  public Object getColumnValue(int columnIndex) {
    if (columnIndex != 0) {
      assert false;
      return null;
    }
    return getObjectValue();
  }

  public <T1, T2> DBRow with(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2) {
    if (column1.equals(column2))
      return with(column2, value2);
    if (myColumn.getColumn().equals(column1) || myColumn.getColumn().equals(column2))
      return DBRowUtil.create(column1, value1, column2, value2);
    return DBRowUtil.create(myColumn.getColumn(), getObjectValue(), column1, value1, column2, value2);
  }

  public void setValue(long value) {
    if (myValue == value)
      return;
    myValue = value;
    myObject = null;
  }

  public DBColumnSet getColumns() {
    return myColumn;
  }

  public String toString() {
    return String.valueOf(myColumn) + "=" + myValue;
  }
}
