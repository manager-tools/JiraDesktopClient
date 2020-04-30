package com.almworks.util.components;

import com.almworks.util.collections.SimpleModifiable;

public class AGridSelectionModel {
  public static final int NOTHING_SELECTED = 0;
  public static final int CELL_SELECTED = 1;
  public static final int ROW_SELECTED = 2;
  public static final int COLUMN_SELECTED = 3;
  public static final int ALL_SELECTED = 4;

  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private int mySelectionType;
  private int myRowFrom;
  private int myRowTo;
  private int myColumnFrom;
  private int myColumnTo;

  public AGridSelectionModel() {
  }

  public SimpleModifiable getModifiable() {
    return myModifiable;
  }

  public int getSelectionType() {
    return mySelectionType;
  }

  public int getRowFrom() {
    return myRowFrom;
  }

  public int getRowTo() {
    return myRowTo;
  }

  public int getColumnFrom() {
    return myColumnFrom;
  }

  public int getColumnTo() {
    return myColumnTo;
  }

  public void update(int selectionType, int rowFrom, int rowTo, int columnFrom, int columnTo) {
    if (selectionType != mySelectionType || myRowFrom != rowFrom || myRowTo != rowTo || myColumnFrom != columnFrom ||
      myColumnTo != columnTo)
    {
      mySelectionType = selectionType;
      myRowFrom = rowFrom;
      myRowTo = rowTo;
      myColumnFrom = columnFrom;
      myColumnTo = columnTo;
      myModifiable.fireChanged();
    }
  }
}
