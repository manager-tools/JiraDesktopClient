package com.almworks.util.model;

import org.almworks.util.Const;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SquareCollectionModelEvent <R, C, V> extends ContentModelEvent {
  private static final Object[][] EMPTYxEMPTY = {};
  private final /*R*/Object[] myRows;
  private final /*C*/Object[] myColumns;
  private final /*V*/Object[][] myValues;

  public SquareCollectionModelEvent(ContentModel source, R[] rows, C[] columns, V[][] values) {
    super(source);
    if (rows != null && columns != null && values != null) {
      assert rows.length == values.length;
      if (rows.length > 0)
        assert columns.length == values[0].length;
    }
    myRows =  (rows == null ? (R[]) Const.EMPTY_OBJECTS : rows);
    myColumns = columns == null ? (C[])Const.EMPTY_OBJECTS : columns;
    myValues = values == null ? (V[][])EMPTYxEMPTY : values;
  }

  public Object[] getRows() {
    return myRows;
  }

  public Object[] getColumns() {
    return myColumns;
  }

  public Object[][] getValues() {
    return myValues;
  }

  public static SquareCollectionModelEvent create(SquareCollectionModel model, Object[] rows, Object[] columns,
    Object[][] values) {
    return new SquareCollectionModelEvent(model, rows, columns, values);
  }

  public static SquareCollectionModelEvent createRows(SquareCollectionModel model, Object[] rows) {
    return create(model, rows, null, null);
  }

  public static SquareCollectionModelEvent createColumns(SquareCollectionModel model, Object[] columns) {
    return create(model, null, columns, null);
  }

  public String toString() {
    StringBuffer r = new StringBuffer("SquareCollectionModelEvent:");
    boolean empty = true;
    if (myRows.length > 0) {
      r.append('R').append(myRows.length);
      empty = false;
    }
    if (myColumns.length > 0) {
      if (!empty)
        r.append('-');
      r.append('C').append(myColumns.length);
      empty = false;
    }
    if (myValues.length > 0) {
      if (!empty)
        r.append('-');
      r.append('V').append(myValues.length).append('x').append(myValues[0].length);
      empty = false;
    }
    if (empty)
      r.append('0');
    return r.toString();
  }

}
