package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import org.almworks.util.Util;

public class TableDropPoint {
  private final ATable<?> myTable;
  private final Object myTargetElement;
  private final int myTargetRow;

  public TableDropPoint(ATable<?> table, Object targetElement, int targetRow) {
    myTable = table;
    myTargetElement = targetElement;
    myTargetRow = targetRow;
  }

  public ATable<?> getTable() {
    return myTable;
  }

  public Object getTargetElement() {
    return myTargetElement;
  }

  public int getTargetRow() {
    return myTargetRow;
  }


  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof TableDropPoint))
      return false;

    TableDropPoint that = (TableDropPoint) o;

    return myTargetRow == that.myTargetRow && myTable.equals(that.myTable) &&
      Util.equals(myTargetElement, that.myTargetElement);
  }

  public int hashCode() {
    int result;
    result = myTable.hashCode();
    result = 31 * result + myTargetElement.hashCode();
    result = 31 * result + myTargetRow;
    return result;
  }

  public boolean isValid() {
    return !(!myTable.isDisplayable() || !myTable.isVisible()) && checkModelElement();
  }

  protected boolean checkModelElement() {
    AListModel<?> model = myTable.getCollectionModel();
    if (myTargetRow < 0 || myTargetRow >= model.getSize())
      return false;
    Object value = model.getAt(myTargetRow);
    return myTargetElement == value;
  }
}
