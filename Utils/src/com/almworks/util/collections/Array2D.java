package com.almworks.util.collections;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;



/**
 * @author dyoma
 */
public class Array2D<T> {
  private final List<List<T>> myRows;
  private int myColumns = 0;

  public Array2D() {
    myRows = Collections15.arrayList();
  }

  public void set(int column, int row, @Nullable T data) {
    getRow(row).set(column, data);
  }

  @Nullable
  public T get(int column, int row) {
    return getRow(row).get(column);
  }

  public void insertColumns(int index, int length, @Nullable T value) {
    myColumns += length;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myRows.size(); i++) {
      List<T> row = myRows.get(i);
      if (row != null)
        for (int c = 0; c < length; c++)
          row.add(index, null);
    }
  }

  public void insertRows(int index, int length) {
    for (int i = 0; i < length; i++)
      myRows.add(index, null);
  }

  public void fillColumns(int fromColumn, int toColumn, @Nullable T value) {
    for (int c = fromColumn; c < toColumn; c++)
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < myRows.size(); i++) {
        List<T> row = myRows.get(i);
        if (row != null)
          row.set(c, value);
      }
  }

  public void clearRow(int row) {
    myRows.set(row, null);
  }

  public void fillRows(int fromRow, int toRow, @Nullable T value) {
    for (int row = fromRow; row < toRow; row++) {
      List<T> list = myRows.get(row);
      if (list == null && value== null)
        return;
      if (list == null)
        list = getRow(row);
      Collections.fill(list, value);
    }
  }

  public void allocSize(int column, int rows) {
    if (myColumns < column) {
      int toAdd = myColumns - column;
      //noinspection ForLoopReplaceableByForEach
      for (int r = 0; r < myRows.size(); r++) {
        List<T> list = myRows.get(r);
        if (list != null)
          for (int i = 0; i < toAdd; i++)
            list.add(null);
      }
      myColumns = column;
    }
    while (rows > myRows.size())
      myRows.add(null);
  }

  @NotNull
  private List<T> getRow(int row) {
    List<T> result = myRows.get(row);
    if (result == null) {
      result = Collections15.arrayList(myColumns);
      for (int i = 0; i < myColumns; i++)
        result.add(null);
      myRows.set(row, result);
    }
    assert result.size() == myColumns;
    return result;
  }

  public int getColumns() {
    return myColumns;
  }

  public void removeColumns(int fromIndex, int toIndex) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myRows.size(); i++) {
      List<T> list = myRows.get(i);
      if (list == null)
        continue;
      list.subList(fromIndex, toIndex).clear();
    }
  }

  public void removeRows(int fromIndex, int toIndex) {
    myRows.subList(fromIndex, toIndex).clear();
  }
}
