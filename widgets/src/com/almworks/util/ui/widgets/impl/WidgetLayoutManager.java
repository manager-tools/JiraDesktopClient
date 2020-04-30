package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.Widget;
import com.almworks.util.ui.widgets.genutil.Log;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


class WidgetLayoutManager {
  private static final Log<WidgetLayoutManager> log = Log.get(WidgetLayoutManager.class);
  private final HostComponentState<?> myState;
  private final InstancePool<CellStack> myStack = CellStack.createPool();
  private final InstancePool<ArrayList<HostCellImpl>> myTmpCells = InstancePool.listPool();
  private final InstancePool<ArrayList<HostCellImpl>> myAllCells = InstancePool.listPool();
  private boolean myBusy = false;
  private final LayoutContextImpl myContext;
  private final LayoutQueue myQueue = new LayoutQueue();

  public WidgetLayoutManager(HostComponentState<?> state) {
    myState = state;
    myContext = new LayoutContextImpl(state, myQueue);
  }

  public boolean flushPending() {
    if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException(Thread.currentThread().toString());
    if (myBusy) {
      log.error(this, "cannot layout", new Throwable());
      return false;
    }
    if (myQueue.isEmpty()) return false;
    CellStack stack = myStack.getInstance();
    IWidgetHostComponent host = myState.getHost();
    Rectangle visible = new Rectangle(0, 0, host.getWidth(), host.getHeight());
    myContext.prepareLayout(visible);
    myBusy = true;
    try {
      doLayoutSubtree(stack);
    } finally {
      myBusy = false;
      myContext.clear();
      stack.clear();
      myStack.release(stack);
    }
    return true;
  }

  private void doLayoutSubtree(CellStack cellStack) {
    ArrayList<HostCellImpl> tmpCells = myTmpCells.getInstance();
    cellStack.push(myState.getRootCell(), myState.getValue());
    while (!myQueue.isEmpty()) {
      HostCellImpl cell = myQueue.setupTarget(myContext);
      if (cell != null) {
        myContext.prepareLayoutCell(cell);
        if (cellStack.buildForCell(cell, tmpCells)) {
          Widget<Object> widget = (Widget<Object>) cell.getWidget();
          Object value = cellStack.topValue();
          widget.layout(myContext, value, null);
          tmpCells.clear();
          myContext.getNotLayedOut(tmpCells);
          //noinspection ForLoopReplaceableByForEach
          for (int i = 0; i < tmpCells.size(); i++) {
            HostCellImpl child = tmpCells.get(i);
            child.deactivate(HostCell.Purpose.VISIBLE);
            if (child.isActive()) widget.layout(myContext, value, child);
          }
        }
      }
      myQueue.pollFirst();
    }
    tmpCells.clear();
    myTmpCells.release(tmpCells);
    myQueue.clear();
  }

  public void invalidate(HostCellImpl cell) {
    myQueue.addToLayout(cell);
    myState.requestFlush();
  }

  public void clear() {
    myBusy = false;
    myQueue.clear();
    myContext.clear();
  }

  public void hostReshaped(int oldWidth, int oldHeight) {
    HostCellImpl rootCell = myState.getRootCell();
    IWidgetHostComponent host = myState.getHost();
    int newWidth = host.getWidth();
    int newHeight = host.getHeight();
    rootCell.setBounds(0, 0, newWidth, newHeight);
    rootCell.invalidate();
    ArrayList<HostCellImpl> allCells = myAllCells.getInstance();
    ArrayList<HostCellImpl> tmpCells = myTmpCells.getInstance();
    rootCell.getChildrenImpl(allCells);
    Rectangle oldBounds = new Rectangle(0, 0, oldWidth, oldHeight);
    Rectangle newBounds = new Rectangle(0, 0, newWidth, newHeight);
    Rectangle tmpRect = new Rectangle();
    for (int i = 0; i < allCells.size(); i++) {
      HostCellImpl cell = allCells.get(i);
      if (!cell.intersects(newBounds)) deactivateAll(cell, HostCell.Purpose.VISIBLE, tmpCells);
      else {
        cell.getChildrenImpl(allCells);
        cell.getHostBounds(tmpRect);
        if (!oldBounds.contains(tmpRect)) cell.invalidate();
      }
    }
    tmpCells.clear();
    myTmpCells.release(tmpCells);
    allCells.clear();
    myAllCells.release(allCells);
    flushPending();
  }

  private void deactivateAll(HostCellImpl ancestor, HostCell.Purpose purpose, List<HostCellImpl> tmpCells) {
    tmpCells.clear();
    tmpCells.add(ancestor);
    ancestor.getChildrenImpl(tmpCells);
    for (HostCellImpl cell : tmpCells) cell.deactivate(purpose);
    tmpCells.clear();
  }
}
