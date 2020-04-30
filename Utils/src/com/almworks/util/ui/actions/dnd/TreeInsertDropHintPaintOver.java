package com.almworks.util.ui.actions.dnd;

import com.almworks.util.components.ATreeNode;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.GlobalColors;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

public class TreeInsertDropHintPaintOver {
  public static final TreeInsertDropHintPaintOver INSTANCE = new TreeInsertDropHintPaintOver();
  private Object myLastParentNode;
  private int myLastInsertIndex;
  private Rectangle myParentBounds;

  private Dimension myLastDragSourceSize;
  private int myHintWidth;
  private int myHintX;

  public TreeInsertDropHintPaintOver() {
  }

  public int getHeight(JTree tree) {
    return tree.getRowHeight();
  }

  public boolean paintSetup(JTree tree, Object parentNode, int insertIndex, int hintRow, Dimension dragSourceSize) {
    Threads.assertAWTThread();
    if (myLastInsertIndex != insertIndex || myLastParentNode != parentNode ||
      !Util.equals(dragSourceSize, myLastDragSourceSize))
    {
      myLastInsertIndex = insertIndex;
      myLastParentNode = parentNode;
      myLastDragSourceSize = dragSourceSize;
      return prepare(tree, (ATreeNode) parentNode, insertIndex, hintRow, dragSourceSize);
    }
    return true;
  }

  private boolean prepare(JTree tree, ATreeNode parentNode, int insertIndex, int hintRow, Dimension dragSourceSize) {
    TreePath parentPath = tree.getPathForRow(hintRow - 1);
    while (parentPath != null && parentPath.getLastPathComponent() != parentNode) {
      parentPath = parentPath.getParentPath();
    }
    if (parentPath == null)
      return false;
    int count = parentNode.getChildCount();

    Rectangle parentBounds = tree.getPathBounds(parentPath);
    if (parentBounds == null)
      return false;
    myParentBounds = parentBounds;

    int x = -1;
    if (count > 0 && tree.isExpanded(parentPath)) {
      int row = tree.getRowForPath(parentPath);
      if (row >= 0 && row < tree.getRowCount() - 1) {
        Rectangle peerBounds = tree.getRowBounds(row + 1);
        if (peerBounds != null) {
          x = peerBounds.x;
        }
      }
    }
    if (x < 0) {
      x = parentBounds.x + 20;
    }
    myHintX = x;

    if (dragSourceSize == null) {
      myHintWidth = parentBounds.width;
    } else {
      myHintWidth = dragSourceSize.width;
    }

    return true;
  }

  public void paint(Graphics2D g2, JTree tree, int y, int w) {
    g2.setColor(GlobalColors.DRAG_AND_DROP_COLOR);
    int height = 2;
    int width = myHintWidth;
    int x = myHintX;
    Object savedAntialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    y -= height / 2 + 1;
    g2.fillRoundRect(x, y, width, height, height, height);
    g2.fillOval(x - 2, y - 2, 5, 6);

//    g2.setColor(ColorUtil.between(GlobalColors.DRAG_AND_DROP_COLOR, tree.getBackground(), 0.5F));
    int px = myParentBounds.x;
    int py = myParentBounds.y + myParentBounds.height / 2;
    g2.fillOval(px - 4, py - 4, 9, 9);
    g2.drawArc(px, py - (y - py), 2 * (x - px), 2 * (y - py), 180, 90);

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAntialiasing);
  }
}
