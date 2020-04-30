package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.MouseEventData;
import com.almworks.util.ui.widgets.genutil.Log;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

final class MouseDispatcher {
  private static final Log<MouseDispatcher> log = Log.get(MouseDispatcher.class);
  private final InstancePool<CellStack> myStack = CellStack.createPool();
  private final HostComponentState<?> myState;
  private HostCellImpl myMouseOwner = null;
  private boolean myResendMouse = false;
  private int myLastX = -1;
  private int myLastY = -1;

  MouseDispatcher(HostComponentState<?> state) {
    myState = state;
  }

  public void dispatch(MouseEvent event) {
    dispatch(event.getID(), event.getX(), event.getY(), event.getButton(), event);
  }

  private void dispatch(int id, int x, int y, int button, @Nullable MouseEvent event) {
    boolean isMouseExit = id == MouseEvent.MOUSE_EXITED;
    myLastX = isMouseExit ? -1 : x;
    myLastY = isMouseExit ? -1 : y;
    myResendMouse = false;
    CellStack stack = myStack.getInstance();
    try {
      if (myMouseOwner != null) {
        stack.buildToAncestor(myMouseOwner, myState.getRootCell(), myState.getValue());
        while (!stack.isEmpty()) {
          HostCellImpl cell = stack.topCell();
          myMouseOwner = cell;
          if (cell.isActive()) {
            if (!isMouseExit && cell.contains(x, y)) break;
            stack.sendEventToTop(MouseEventData.KEY, MouseEventDataImpl.EXIT);
            cell.deactivate(HostCell.Purpose.MOUSE_HOVER);
          }
          stack.pop();
        }
      } else if (!isMouseExit)
        stack.push(myState.getRootCell(), myState.getValue());
      if (stack.isEmpty() || isMouseExit) {
        myMouseOwner = null;
        return;
      }
      HostCellImpl cell = stack.topCell();
      cell.activate(HostCell.Purpose.MOUSE_HOVER);
      myMouseOwner = cell;
      while (true) {
        HostCellImpl child = cell.findChild(x, y);
        if (child == null) break;
        stack.pushChild(child);
        child.activate(HostCell.Purpose.MOUSE_HOVER);
        cell = child;
        myMouseOwner = cell;
      }
      MouseEventDataImpl data = MouseEventDataImpl.create(id, x, y, button, event);
      Cursor cursor = null;
      while (!stack.isEmpty()) {
        HostCellImpl topCell = stack.topCell();
        data.setX(x - topCell.getHostX());
        data.setY(y - topCell.getHostY());
        EventContextImpl context = stack.sendEventToTop(MouseEventData.KEY, data);
        stack.pop();
        if (context.getCursor() != null) cursor = context.getCursor();
        if (context.isConsumed()) break;
      }
      while (!stack.isEmpty()) {
        stack.sendEventToTop(MouseEventData.KEY, MouseEventDataImpl.EXIT);
        stack.pop();
      }
      myState.getHost().setCursor(cursor);
    } finally {
      stack.clear();
      myStack.release(stack);
    }
  }

  public void clear() {
    myMouseOwner = null;
  }

  public void resendMouse() {
    myResendMouse = true;
    myState.requestFlush();
  }

  public boolean flushPending() {
    if (!myResendMouse || myLastX < 0 || myLastY < 0) {
      myResendMouse = false;
      return false;
    }
    dispatch(MouseEvent.MOUSE_MOVED, myLastX, myLastY, 0, null);
    myResendMouse = false;
    return true;
  }

  public void cellReshaped(HostCellImpl cell) {
    if (myResendMouse || myMouseOwner == null) return;
    if (myMouseOwner == cell)
    if (!myMouseOwner.inAncestorOf(cell)) return;
    myResendMouse = true;
  }
}
