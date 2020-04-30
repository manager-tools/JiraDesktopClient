package com.almworks.util.components;

public abstract class AGridCellFunction<R, C, V> {
  public abstract V getValue(R row, C column, int rowIndex, int columnIndex);
}
