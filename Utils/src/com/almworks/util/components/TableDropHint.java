package com.almworks.util.components;

import com.almworks.util.ui.actions.dnd.DropHint;

import javax.swing.*;
import java.awt.*;

public abstract class TableDropHint extends DropHint {
  private final TableDropPoint myDropPoint;

  public TableDropHint(TableDropPoint dropPoint) {
    myDropPoint = dropPoint;
  }

  public TableDropPoint getDropPoint() {
    return myDropPoint;
  }


  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    TableDropHint that = (TableDropHint) o;

    return myDropPoint.equals(that.myDropPoint);
  }

  public int hashCode() {
    return myDropPoint.hashCode();
  }

  public boolean isValid() {
    return super.isValid() && myDropPoint.isValid();
  }

  public abstract void paint(Graphics g, JTable table);

  public abstract void repaint(JTable table);
}
