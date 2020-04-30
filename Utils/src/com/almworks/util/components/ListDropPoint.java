package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;

import javax.swing.*;

public class ListDropPoint {
  private final FlatCollectionComponent myList;
  private final Object myTargetElement;
  private final int myTargetRow;
  private final boolean myInsert;

  public ListDropPoint(FlatCollectionComponent list, Object targetElement, int targetRow, boolean insert) {
    myList = list;
    myTargetElement = targetElement;
    myTargetRow = targetRow;
    myInsert = insert;
  }

  public JComponent getList() {
    return myList.toComponent();
  }

  public Object getTargetElement() {
    return myTargetElement;
  }

  public int getTargetRow() {
    return myTargetRow;
  }

  public boolean isInsert() {
    return myInsert;
  }


  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ListDropPoint that = (ListDropPoint) o;

    if (myInsert != that.myInsert)
      return false;
    if (myTargetRow != that.myTargetRow)
      return false;
    if (myList != null ? !myList.equals(that.myList) : that.myList != null)
      return false;
    if (myTargetElement != null ? !myTargetElement.equals(that.myTargetElement) : that.myTargetElement != null)
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myList != null ? myList.hashCode() : 0);
    result = 31 * result + (myTargetElement != null ? myTargetElement.hashCode() : 0);
    result = 31 * result + myTargetRow;
    result = 31 * result + (myInsert ? 1 : 0);
    return result;
  }

  public boolean isValid() {
    JComponent listComponent = myList.toComponent();
    if (!listComponent.isDisplayable() || !listComponent.isVisible())
      return false;
    AListModel model = myList.getCollectionModel();
    int size = model.getSize();
    if (myTargetRow < 0 || myTargetRow > size)
      return false;
    if (!myInsert) {
      if (myTargetRow == size)
          return false;
      Object value = model.getAt(myTargetRow);
      return myTargetElement == value;
    } else {
      return true;
    }
  }
}
