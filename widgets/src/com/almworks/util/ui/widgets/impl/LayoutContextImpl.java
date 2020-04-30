package com.almworks.util.ui.widgets.impl;

import com.almworks.util.collections.SortedArraySet;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.LayoutContext;
import com.almworks.util.ui.widgets.Widget;
import org.almworks.util.Collections15;

import java.awt.*;
import java.util.Arrays;
import java.util.List;


final class LayoutContextImpl extends CellContextImpl implements LayoutContext {
  private final Rectangle myVisible = new Rectangle();
  private final Rectangle myTargetRect = new Rectangle();
  private final LayoutQueue myQueue;
  private final SortedArraySet<HostCellImpl> myNotLayedoutCells = SortedArraySet.create();
  private final Rectangle myTmpOldBounds = new Rectangle();
  private final List<HostCellImpl> myTempChildren = Collections15.arrayList();
  private final int[] myTmpRectResult = new int[4];
  private boolean mySetTarget;

  public LayoutContextImpl(HostComponentState<?> state, LayoutQueue myQueue) {
    super(state);
    this.myQueue = myQueue;
  }

  public void prepareLayout(Rectangle visible) {
    myVisible.setBounds(visible);
    myTargetRect.setBounds(visible);
  }

  @Override
  public int getTargetX() {
    return myTargetRect.x - getHostX();
  }

  @Override
  public int getTargetY() {
    return myTargetRect.y - getHostY();
  }

  @Override
  public int getTargetWidth() {
    return myTargetRect.width;
  }

  @Override
  public int getTargetHeight() {
    return myTargetRect.height;
  }

  @Override
  public void setChildBounds(int id, Widget<?> widget, Rectangle bounds) {
    setChildBounds(id, widget, bounds.x, bounds.y, bounds.width, bounds.height);
  }

  @Override
  public void setChildBounds(int id, Widget<?> widget, int x, int y, int width, int height) {
    setChildBounds(id, widget, x, y, width, height, null);
  }

  public void setChildBounds(int id, Widget<?> widget, int x, int y, int width, int height, HostCell.Purpose activate) {
    x += getHostX();
    y += getHostY();
    HostCellImpl cell = getActiveCell();
    if (cell == null) return;
    boolean isVisible = myVisible.intersects(x, y, width, height);
    if (activate == null && !isVisible) {
      HostCellImpl child = cell.findChild(id);
      if (child == null) return;
      child.deactivate(HostCell.Purpose.VISIBLE);
      if (!child.isActive()) return;
    }
    HostCellImpl child = cell.getOrCreateChild(widget, id);
    if (child == null) return;
    if (activate != null) child.activate(activate);
    if (isVisible) child.activate(HostCell.Purpose.VISIBLE);
    else child.deactivate(HostCell.Purpose.VISIBLE);
    if (!child.isActive()) return;
    myNotLayedoutCells.remove(child);
    child.getHostBounds(myTmpOldBounds);
    if (AwtUtil.equals(myTmpOldBounds, x, y, width, height)) return;
    if (AwtUtil.isSameSize(myTmpOldBounds, width, height))
      moveCell(child, x, y);
    else
      resizeCell(child, x, y, width, height);

  }

  private void resizeCell(HostCellImpl cell, int x, int y, int width, int height) {
    cell.setBounds(x, y, width, height);
    myQueue.addToLayout(cell);
    myTempChildren.clear();
    cell.getChildrenImpl(myTempChildren);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myTempChildren.size(); i++) {
      HostCellImpl child = myTempChildren.get(i);
      myQueue.addToLayout(child);
    }
    myTempChildren.clear();
  }

  private void moveCell(HostCellImpl cell, int x, int y) {
    int oldX = myTmpOldBounds.x;
    int oldY = myTmpOldBounds.y;
    int oldCX = oldX + myTmpOldBounds.width;
    int oldCY = oldY + myTmpOldBounds.height;
    int targetX = myTargetRect.x;
    int targetY = myTargetRect.y;
    int targetCX = targetX + myTargetRect.width;
    int targetCY = targetY + myTargetRect.height;
    int dx = x - oldX;
    int dy = y - oldY;
    doMoveSubTree(cell, dx, dy);
    myTempChildren.clear();
    myTempChildren.add(cell);
    int i = 0;
    while (i < myTempChildren.size()) {
      HostCellImpl descendant = myTempChildren.get(i);
      if (descendant.isActive()) {
        calcNewIntersection(myTmpRectResult, oldX, oldY, oldCX, oldCY, targetX, targetY, targetCX, targetCY, dy);
        int layoutX = myTmpRectResult[0];
        int layoutY = myTmpRectResult[1];
        int layoutCX = myTmpRectResult[2];
        int layoutCY = myTmpRectResult[3];
        myQueue.addToLayout(descendant, layoutX, layoutY, layoutCX - layoutX, layoutCY - layoutY);
        calcNewIntersection(myTmpRectResult, oldY, oldX, oldCY, oldCX, targetY, targetX, targetCY, targetCX, dx);
        layoutX = myTmpRectResult[1];
        layoutY = myTmpRectResult[0];
        layoutCX = myTmpRectResult[3];
        layoutCY = myTmpRectResult[2];
        myQueue.addToLayout(descendant, layoutX, layoutY, layoutCX - layoutX, layoutCY - layoutY);
      }
      i++;
    }
  }

  private void doMoveSubTree(HostCellImpl ancestor, int dx, int dy) {
    myTempChildren.clear();
    myTempChildren.add(ancestor);
    HostCellImpl.repaintMove(getHost().getHost(), dx, dy, ancestor.getHostX(), ancestor.getHostY(), ancestor.getWidth(), ancestor.getHeight());
    int i = 0;
    while (i < myTempChildren.size()) {
      HostCellImpl cell = myTempChildren.get(i);
      if (!cell.isBoundsIntersect(myVisible)) cell.deactivate(HostCell.Purpose.VISIBLE);
      if (cell.isActive()) {
        cell.moveBounds(dx, dy, false);
        cell.getChildrenImpl(myTempChildren);
      }
      i++;
    }
    myTempChildren.clear();
  }

  private static void calcNewIntersection(int[] result, int x1, int y1, int cx1, int cy1, int x2, int y2, int cx2, int cy2, int dy) {
    if (dy == 0) {
      Arrays.fill(result, -1);
      return;
    }
    result[0] = Math.max(x1, x2);
    result[2] = Math.min(cx1, cx2);
    if (result[0] >= result[2]) {
      Arrays.fill(result, -1);
      return;
    }
    int newY = y1 + dy;
    int newCY = cy1 + dy;
    if (dy > 0) {
      result[1] = Math.max(y2, newY);
      result[3] = Math.max(y1, y2) + dy;
    } else {
      result[1] = Math.min(cy1, cy2) + dy;
      result[3] = Math.min(cy2, newCY);
    }
    if (result[1] >= result[3]) Arrays.fill(result, -1);
  }

  public void setTarget(int x, int y, int width, int height, boolean setTarget) {
    mySetTarget = setTarget;
    myTargetRect.setBounds(x, y, width, height);
    AwtUtil.intersection(myVisible, myTargetRect, myTargetRect);
  }

  public boolean isTargetEmpty() {
    return myTargetRect.isEmpty();
  }

  public void prepareLayoutCell(HostCellImpl cell) {
    setCurrentCell(cell);
    myNotLayedoutCells.clear();
    myTempChildren.clear();
    if (!mySetTarget) {
      cell.getChildrenImpl(myTempChildren);
      myNotLayedoutCells.addAll(myTempChildren);
      myTempChildren.clear();
    }
  }

  public void getNotLayedOut(List<HostCellImpl> target) {
    myNotLayedoutCells.getItems(target);
  }

  public void clear() {
    myNotLayedoutCells.clear();
    myTempChildren.clear();
    setCurrentCell(null);
  }
}
