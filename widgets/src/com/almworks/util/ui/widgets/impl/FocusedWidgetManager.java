package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.widgets.EventContext;
import com.almworks.util.ui.widgets.FocusTraverse;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.genutil.Log;
import com.almworks.util.ui.widgets.util.WidgetUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.KeyEvent;

final class FocusedWidgetManager {
  private static final Log<FocusedWidgetManager> log = Log.get(FocusedWidgetManager.class);
  private static final int FOCUS_KEEP = 0;
  private static final int FOCUS_DEEP = 1;
  private static final int FOCUS_FORWARD = 2;
  private static final int FOCUS_BACKWARD = 3;
  private final HostComponentState<?> myState;
  private final InstancePool<CellStack> myStack = CellStack.createPool();
  private int myFocusOtherRequested = FOCUS_KEEP;
  private HostCellImpl myFocusRequest = null;
  private HostCellImpl myFocused = null;

  public FocusedWidgetManager(HostComponentState<?> state) {
    myState = state;
  }

  public void dispatch(KeyEvent event) {
    flushPending();
    if (myFocused == null || !myFocused.isActive()) return;
    if (dispatchFocusTraverse(event)) return;
    CellStack stack = myStack.getInstance();
    try {
      stack.buildToAncestor(myFocused, myState.getRootCell(), myState.getValue());
      while (!stack.isEmpty()) {
        EventContextImpl context = stack.sendEventToTop(EventContext.KEY_EVENT, event);
        if (context.isConsumed()) {
          event.consume();
          break;
        }
        stack.pop();
      }
    } finally {
      stack.clear();
      myStack.release(stack);
    }
  }

  private boolean dispatchFocusTraverse(KeyEvent event) {
    int action = AwtUtil.getFocusAction(event, myState.getHost().getHostComponent());
    if (action != -1) {
      event.consume();
      switch (action) {
      case KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS:
        myFocusOtherRequested = FOCUS_FORWARD;
        flushPending();
        return true;
      case KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS:
        myFocusOtherRequested = FOCUS_BACKWARD;
        flushPending();
        return true;
      case KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS:
        log.error(this, "Unsupported UP_CYCLE_TRAVERSAL_KEYS");
        return false;
      case KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS:
        log.error(this, "Unsupported DOWN_CYCLE_TRAVERSAL_KEYS");
        return false;
      default:
        log.error(this, "Unknown action", action);
        return false;
      }
    }
    return false;
  }

  public HostCellImpl getFocusedCell() {
    return myFocused;
  }

  public void requestFocus(HostCellImpl cell) {
    if (cell != null && cell.isActive()) {
      myFocusRequest = cell;
      myFocusOtherRequested = FOCUS_KEEP;
      myState.requestFlush();
      myState.getHost().widgetRequestsFocus();
    }
  }

  public boolean flushPending() {
    boolean changed = false;
    if (myFocusRequest != null && myFocusRequest.isActive()) {
      setFocusedCell(myFocusRequest);
      changed = true;
    }
    myFocusRequest = null;
    if (myFocusOtherRequested != FOCUS_KEEP) {
      traverseFocus();
      changed = true;
    }
    if (myFocused != null && myFocused.isActive()) return changed;
    traverseFocus(myFocused, true, true);
    return true;
  }

  private void setFocusedCell(HostCellImpl requested) {
    CellStack stack = myStack.getInstance();
    try {
      HostCell notChangedAncestor = WidgetUtil.getCommonAncestor(myFocused, requested);
      if (myFocused != null) {
        stack.buildToAncestor(myFocused, myState.getRootCell(), myState.getValue());
        stack.sendEventToTop(EventContext.FOCUS_LOST, null);
        stack.sendEventFromTopTo(notChangedAncestor, EventContext.DESCENDANT_LOST_FOCUS, myFocused);
        myFocused.deactivate(HostCell.Purpose.FOCUSED);
        myFocused = null;
      }
      stack.clear();
      stack.buildToAncestor(requested, myState.getRootCell(), myState.getValue());
      myFocused = requested;
      myFocused.activate(HostCell.Purpose.FOCUSED);
      stack.sendEventToTop(EventContext.FOCUS_GAINED, null);
      stack.sendEventFromTopTo(notChangedAncestor, EventContext.DESCENDANT_GAINED_FOCUS, myFocused);
    } finally {
      stack.clear();
      myStack.release(stack);
    }
  }

  private void traverseFocus() {
    boolean traverse = myFocusOtherRequested == FOCUS_FORWARD || myFocusOtherRequested == FOCUS_BACKWARD;
    boolean forward = myFocusOtherRequested == FOCUS_FORWARD || myFocusOtherRequested == FOCUS_DEEP || myFocusOtherRequested == FOCUS_KEEP;
    traverseFocus(myFocused, forward, traverse);
  }

  public void clear() {
    myFocused = null;
    myFocusRequest = null;
    myFocusOtherRequested = FOCUS_KEEP;
  }

  public void focusedCellDeactivated(HostCellImpl cell) {
    if (cell != myFocused) return;
    if (myFocusRequest != null) return;
    myFocusOtherRequested = FOCUS_FORWARD;
    myState.requestFlush();
  }

  public void moveFocusDeep() {
    myFocusOtherRequested = FOCUS_DEEP;
  }

  public void traverseFocus(HostCellImpl initial, boolean forward, boolean traverse) {
    HostCellImpl newFocus = new FocusTraverseMethod(myState, forward, traverse).perform(initial);
    if (newFocus == null || !newFocus.isActive())
      newFocus = myState.getRootCell();
    setFocusedCell(newFocus);
    myFocusOtherRequested = FOCUS_KEEP;
  }

  public HostCellImpl getFocusRequestCell() {
    return myFocusRequest;
  }

  static class FocusTraverseMethod extends FocusTraverse {
    private static final Log<FocusTraverseMethod> log = Log.get(FocusTraverseMethod.class);
    private final HostComponentState<?> myState;
    private final boolean myForward;
    private final boolean myTraverse;
    private int myPrevChild;
    private boolean myPrevChildSet = false;

    FocusTraverseMethod(HostComponentState<?> state, boolean forward, boolean traverse) {
      myState = state;
      myForward = forward;
      myTraverse = traverse;
    }

    @Nullable
    public HostCellImpl perform(HostCellImpl initial) {
      if (initial == null)
        initial = myState.getRootCell();
      while (initial != null && !initial.isActive()) {
        myPrevChildSet = true;
        myPrevChild = initial.getId();
        initial = initial.getParent();
      }
      if (initial == null)
        return null;
      CellStack stack = new CellStack();
      if (myTraverse) {
        stack.buildToAncestor(initial, myState.getRootCell(), myState.getValue());
        while (!stack.isEmpty()) {
          HostCellImpl cell = stack.topCell();
          stack.sendEventToTop(KEY, this);
          HostCell child = getChild();
          if ((child instanceof HostCellImpl) && child.isActive() && cell == child.getParent()) {
            HostCellImpl result = traverseToChildren(stack, child);
            if (result != null && result.isActive())
              return result;
          }
          myPrevChildSet = true;
          myPrevChild = cell.getId();
          stack.pop();
        }
        return new FocusTraverseMethod(myState, myForward, false).perform(null);
      }
      stack.clear();
      stack.buildToAncestor(initial, myState.getRootCell(), myState.getValue());
      return traverseDeep(stack);
    }

    @Nullable
    private HostCellImpl traverseDeep(CellStack stack) {
      myPrevChild = -1;
      myPrevChildSet = false;
      clear();
      stack.sendEventToTop(KEY, this);
      boolean focusThis = isFocusThis();
      HostCell aChild = getChild();
      HostCellImpl result = traverseToChildren(stack, aChild);
      if (result != null && result.isActive())
        return result;
      if (focusThis)
        return stack.topCell();
      return null;
    }

    @Nullable
    private HostCellImpl traverseToChildren(CellStack stack, HostCell aChild) {
      HostCellImpl cell = stack.topCell();
      if (cell == null || !cell.isActive())
        return null;
      int count = cell.getChildCount();
      while ((aChild instanceof HostCellImpl) && aChild.isActive()) {
        if (aChild.getParent() != cell) {
          log.error(this, "Wrong child", cell, cell.getWidget(), aChild);
          assert false : cell + " " + cell.getWidget() + " " + aChild;
          return null;
        }
        HostCellImpl child = (HostCellImpl) aChild;
        stack.pushChild(child);
        HostCellImpl result = traverseDeep(stack);
        stack.pop();
        if (result != null && result.isActive())
          return result;
        myPrevChildSet = true;
        myPrevChild = child.getId();
        stack.sendEventToTop(KEY, this);
        aChild = getChild();
        if (count <= 0) {
          log.warn(this, "Focusable child not found", cell, myTraverse, myForward);
          return null;
        }
        count--;
      }
      return null;
    }

    public int getPrevChild() {
      assert myPrevChildSet;
      return myPrevChild;
    }

    @Override
    public boolean isTraverse() {
      return myTraverse;
    }

    @Override
    public boolean isForward() {
      return myForward;
    }

    @Override
    public boolean hasPrevChild() {
      return myPrevChildSet;
    }
  }

}
