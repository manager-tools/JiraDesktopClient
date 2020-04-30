package com.almworks.items.impl.dbadapter;

public interface DBRow {
  DBRow EMPTY = new Empty();

  DBColumnSet getColumns();

  <T> DBRow setValue(DBColumn<T> column, T value);

  <T> T getValue(DBColumn<T> column);

  <T> DBRow with(DBColumn<T> column, T value);

  Object getColumnValue(int columnIndex);

  <T1, T2> DBRow with(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2);


  public static class Empty implements DBRow {
    public DBColumnSet getColumns() {
      return DBColumnSet.EMPTY;
    }

    public <T> DBRow setValue(DBColumn<T> column, T value) {
      assert false : column;
      return this;
    }

    public <T> T getValue(DBColumn<T> column) {
      assert false : column;
      return null;
    }

    public <T> DBRow with(DBColumn<T> column, T value) {
      return DBRowUtil.create(column, value);
    }

    public Object getColumnValue(int columnIndex) {
      assert false : columnIndex;
      return null;
    }

    public <T1, T2> DBRow with(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2) {
      return DBRowUtil.create(column1, value1, column2, value2);
    }

    private Empty() {}
  }
}
