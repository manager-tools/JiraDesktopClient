package com.almworks.util.components;

import com.almworks.util.ui.actions.dnd.DropHint;

public class ListDropHint extends DropHint {
  private final ListDropPoint myDropPoint;

  public ListDropHint(ListDropPoint dropPoint) {
    myDropPoint = dropPoint;
  }

  public ListDropPoint getDropPoint() {
    return myDropPoint;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ListDropHint that = (ListDropHint) o;

    if (myDropPoint != null ? !myDropPoint.equals(that.myDropPoint) : that.myDropPoint != null)
      return false;

    return true;
  }

  public int hashCode() {
    return (myDropPoint != null ? myDropPoint.hashCode() : 0);
  }
}
