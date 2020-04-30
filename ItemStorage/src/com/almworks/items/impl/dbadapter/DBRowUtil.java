package com.almworks.items.impl.dbadapter;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The class is final: if final is removed, revise access to private members of other instances.
 */
public final class DBRowUtil implements DBRow {
  private final DBColumnSet myColumns;
  private final Object[] myValues;

  private DBRowUtil(DBColumnSet columns) {
    myColumns = columns;
    myValues = new Object[columns.size()];
  }

  private DBRowUtil(DBColumn... columns) {
    this(DBColumnSet.create(columns));
  }

  public static <T> DBRowSingleColumn create(@NotNull DBColumn<T> column, T value) {
    return new DBRowSingleColumn(column).setValue(column, value);
  }

  public static DBRowSingleColumnLong create(DBLongColumn column, long value) {
    return new DBRowSingleColumnLong(column, value);
  }

  public static <T1, T2> DBRow create(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2) {
    return new DBRowTwoColumns(column1, value1, column2, value2);
  }

  public static <T1, T2, T3> DBRow create(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2,
    DBColumn<T3> column3, T3 value3)
  {
    return new DBRowUtil(column1, column2, column3).setValue(column1, value1)
      .setValue(column2, value2)
      .setValue(column3, value3);
  }

  public static <T1, T2, T3, T4> DBRow create(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2,
    DBColumn<T3> column3, T3 value3, DBColumn<T4> column4, T4 value4)
  {
    return new DBRowUtil(column1, column2, column3, column4).setValue(column1, value1)
      .setValue(column2, value2)
      .setValue(column3, value3)
      .setValue(column4, value4);
  }

  public static <T1, T2, T3, T4, T5> DBRow create(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2,
    DBColumn<T3> column3, T3 value3, DBColumn<T4> column4, T4 value4, DBColumn<T5> column5, T5 value5)
  {
    return new DBRowUtil(column1, column2, column3, column4, column5).setValue(column1, value1)
      .setValue(column2, value2)
      .setValue(column3, value3)
      .setValue(column4, value4)
      .setValue(column5, value5);
  }

  public DBColumnSet getColumns() {
    return myColumns;
  }

  public <T> DBRow setValue(DBColumn<T> column, T value) {
    int ci = myColumns.indexOf(column);
    if (ci < 0 || ci >= myValues.length) {
      assert false : column + " " + this;
    } else {
      myValues[ci] = value;
    }
    return this;
  }

  public <T> T getValue(DBColumn<T> column) {
    int ci = myColumns.indexOf(column);
    if (ci < 0 || ci >= myValues.length) {
      assert false : column + " " + this;
      return null;
    } else {
      return (T)myValues[ci];
    }
  }

  public String toString() {
    return toString(new StringBuilder()).toString();
  }

  private StringBuilder toString(StringBuilder builder) {
    builder.append("R[");
    String prefix = "";
    for (Object value : myValues) {
      builder.append(prefix);
      builder.append(String.valueOf(value));
      prefix = ",";
    }
    builder.append(']');
    myColumns.toString(builder);
    return builder;
  }

  public void copyTo(DBRow dest, DBColumnSet columns) {
    copy(this, dest, columns);
  }

  public static DBRow union(DBRow... rows) {
    Set<DBColumn> columns = Collections15.linkedHashSet();
    for (DBRow row : rows) {
      row.getColumns().addTo(columns);
    }
    DBColumnSet resultColumns = DBColumnSet.create(columns);
    DBRowUtil result = new DBRowUtil(resultColumns);
    for (int ri = 0; ri < resultColumns.size(); ri++) {
      DBColumn column = resultColumns.get(ri);
      int i;
      for (i = rows.length - 1; i >= 0; i--) {
        DBRow row = rows[i];
        int ci = row.getColumns().indexOf(column);
        if (ci >= 0) {
          result.myValues[ri] = row.getColumnValue(ci);
          break;
        }
      }
      if (i < 0) {
        assert false : column;
      }
    }
    return result;
  }

  public <T> DBRow with(DBColumn<T> column, T value) {
    int ci = myColumns.indexOf(column);
    if (ci >= 0) {
      DBRowUtil copy = new DBRowUtil(myColumns);
      for (int i = 0; i < myColumns.size(); i++)
        copy.myValues[i] = i != ci ?  myValues[i] : value;
      return copy;
    }
    DBRowUtil copy = new DBRowUtil(myColumns.with(column));
    for (int i = 0; i < myColumns.size(); i++) {
      assert copy.myColumns.get(i) == myColumns.get(i);
      copy.myValues[i] = myValues[i];
    }
    assert copy.myColumns.get(myColumns.size()) == column;
    copy.myValues[myColumns.size()] = value;
    return copy;
  }

  public Object getColumnValue(int columnIndex) {
    if (!(columnIndex >= 0 && columnIndex < myColumns.size())) {
      assert false;
      return null;
    }
    return myValues[columnIndex];
  }

  public <T1, T2> DBRow with(DBColumn<T1> column1, T1 value1, DBColumn<T2> column2, T2 value2) {
    return with(column1, value1).with(column2, value2);
  }

  public static boolean hasAllDataFrom(DBRow row, DBRow fromRow) {
    DBColumnSet columns = row.getColumns();
    DBColumnSet fromColumns = fromRow.getColumns();
    for (int i = 0; i < fromColumns.size(); i++) {
      DBColumn column = fromColumns.get(i);
      int j = columns.indexOf(column);
      if (j < 0)
        return false;
      if (!column.areEqual(fromRow.getColumnValue(i), row.getColumnValue(j)))
        return false;
    }
    return true;
  }

  public static DBRow create(DBColumnSet columns) {
    if (columns.size() == 0)
      return EMPTY;
    if (columns.size() == 1)
      return new DBRowSingleColumn(columns.get(0));
    return new DBRowUtil(columns);
  }

  public static void copyTo(DBRow row, DBRow copyTo) {
    DBColumnSet destColumns = copyTo.getColumns();
    DBColumnSet srcColumns = row.getColumns();
    for (int i = 0; i < destColumns.size(); i++) {
      DBColumn column = destColumns.get(i);
      int si = srcColumns.indexOf(column);
      if (si < 0)
        continue;
      copyTo.setValue(column, row.getColumnValue(si));
    }
  }

  public static DBRow columns(DBColumn column1, DBColumn column2) {
    assert column1 != null;
    assert column2 != null;
    return new DBRowTwoColumns(column1, null, column2, null);
  }

  public static DBRow columns(DBColumn column) {
    return new DBRowSingleColumn(column);
  }

  public static DBRow columns(DBColumn ...columns) {
    if (columns == null || columns.length == 0)
      return EMPTY;
    if (columns.length == 1)
      return new DBRowSingleColumn(columns[0]);
    if (columns.length == 2)
      return new DBRowTwoColumns(columns[0], null, columns[1], null);
    return new DBRowUtil(DBColumnSet.create(columns));
  }

  public static void copy(DBRow from, DBRow to, DBColumnSet columns) {
    DBColumnSet destColumns = to.getColumns();
    DBColumnSet srcColumns = from.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      DBColumn column = columns.get(i);
      int si = srcColumns.indexOf(column);
      if (si < 0) {
        assert false : si + " " + column + " " + from;
        continue;
      }
      to.setValue(column, from.getColumnValue(si));
    }
  }
}
