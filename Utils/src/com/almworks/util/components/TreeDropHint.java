package com.almworks.util.components;

import com.almworks.util.ui.actions.dnd.DropHint;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

public class TreeDropHint extends DropHint {
  private final int myHintRow;
  private final TreeDropPoint myDropPoint;
  private final Dimension myDragSourceSize;

  public TreeDropHint(int hintRow, TreeDropPoint dropPoint, Dimension dragSourceSize) {
    myHintRow = hintRow;
    myDropPoint = dropPoint;
    myDragSourceSize = dragSourceSize;
  }

  public int getHintRow() {
    return myHintRow;
  }

  public TreeDropPoint getDropPoint() {
    return myDropPoint;
  }

  @Nullable
  public Dimension getDragSourceSize() {
    return myDragSourceSize;
  }


  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    TreeDropHint that = (TreeDropHint) o;

    if (myHintRow != that.myHintRow)
      return false;
    if (myDragSourceSize != null ? !myDragSourceSize.equals(that.myDragSourceSize) : that.myDragSourceSize != null)
      return false;
    if (!myDropPoint.equals(that.myDropPoint))
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myHintRow;
    result = 31 * result + myDropPoint.hashCode();
    result = 31 * result + (myDragSourceSize != null ? myDragSourceSize.hashCode() : 0);
    return result;
  }

  public String toString() {
    return "ITH[" + myHintRow + ";" + myDropPoint + ";" + ";" + myDragSourceSize + ";" + myValid + "]";
  }

  public boolean isValid(JTree tree) {
    assert myValid : this;
    Object node = myDropPoint.getNode();
    if (myDropPoint.isInsertNode()) {
      int insertionIndex = myDropPoint.getInsertionIndex();
      assert myHintRow > 0 && node != null && insertionIndex >= 0 : this;
      assert myHintRow <= tree.getRowCount() : this + " " + tree;
      TreePath path = tree.getPathForRow(myHintRow - 1);
      assert path != null : this + " " + tree;
      if (insertionIndex == 0) {
        assert path.getLastPathComponent() == node : this + " " + path;
      } else {
        boolean found = false;
        for (TreePath p = path.getParentPath(); p != null; p = p.getParentPath()) {
          if (p.getLastPathComponent() == node) {
            found = true;
            break;
          }
        }
        assert found : this + " " + path;
      }
    } else {
      assert node != null : this;
    }
    return true;
  }
}
