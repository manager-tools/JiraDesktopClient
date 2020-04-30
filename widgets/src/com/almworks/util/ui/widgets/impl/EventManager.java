package com.almworks.util.ui.widgets.impl;

import com.almworks.util.collections.IntArray;
import com.almworks.util.ui.widgets.EventContext;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.util.List;

final class EventManager {
  private static final Log<EventManager> log = Log.get(EventManager.class);
  private static final int FLAG_TO_FIRST_ANCESTOR = 1;
  private final List<HostCellImpl> myTarget = Collections15.arrayList();
  private final List<TypedKey<?>> myReasons = Collections15.arrayList();
  private final List<Object> myDatas = Collections15.arrayList();
  private final IntArray myFlags = new IntArray();
  private final InstancePool<CellStack> myStack = CellStack.createPool();
  private final HostComponentState<?> myState;
  private final List<HostCellImpl> myTmpCells = Collections15.arrayList();

  EventManager(HostComponentState<?> state) {
    myState = state;
  }

  public boolean flushPending() {
    myTmpCells.clear();
    if (myTarget.isEmpty()) return false;
    int index = 0;
    CellStack stack = myStack.getInstance();
    try {
      while (index < myTarget.size()) {
        if (stack.isEmpty()) stack.push(myState.getRootCell(), myState.getValue());
        HostCellImpl cell = myTarget.get(index);
        TypedKey<Object> reason = (TypedKey<Object>) myReasons.get(index);
        Object data = myDatas.get(index);
        int flags = myFlags.get(index);
        index++;
        if (!stack.buildForCell(cell, myTmpCells)) continue;
        sendEvent(stack, reason, data, flags);
      }
      myTarget.clear();
      myReasons.clear();
      myDatas.clear();
      myFlags.clear();
    } finally {
      stack.clear();
      myStack.release(stack);
      myTmpCells.clear();
      if (index < myTarget.size()) {
        myTarget.subList(0, index).clear();
        myDatas.subList(0, index).clear();
        myReasons.subList(0, index).clear();
        myFlags.removeRange(0, index);
      }
    }
    return true;
  }

  public <T> void sendEvent(HostCellImpl cell, TypedKey<T> reason, T data) {
    flushPending();
    CellStack stack = myStack.getInstance();
    try {
      stack.clear();
      myTmpCells.clear();
      stack.push(myState.getRootCell(), myState.getValue());
      if (stack.buildForCell(cell, myTmpCells)) {
        if (!stack.sendWhileNotConsumed(reason, data)) processNotConsumed(reason, data);
      }
    } finally {
      stack.clear();
      myStack.release(stack);
      myTmpCells.clear();
    }
    flushPending();
  }

  private void sendEvent(CellStack stack, TypedKey<Object> reason, Object data, int flags) {
    if (flags == 0) stack.sendEventToTop(reason, data);
    else if (flags == FLAG_TO_FIRST_ANCESTOR) {
      stack.pop();
      if (!stack.sendWhileNotConsumed(reason, data)) processNotConsumed(reason, data);
    } else {
      log.error(this, "Wrong flags", flags);
    }
  }

  public void clear() {
    myTmpCells.clear();
    myTarget.clear();
    myReasons.clear();
    myDatas.clear();
    myFlags.clear();
  }

  public <T> void postEvent(HostCellImpl cell, TypedKey<? super T> reason, T data) {
    postEvent(cell, reason, data, 0);
  }

  public <T> void postEventToAncestors(HostCellImpl descendant, TypedKey<T> reason, T data) {
    postEvent(descendant, reason, data, FLAG_TO_FIRST_ANCESTOR);
  }

  private <T> void postEvent(HostCellImpl cell, TypedKey<? super T> reason, T data, int flags) {
    myTarget.add(cell);
    myReasons.add(reason);
    myDatas.add(data);
    myFlags.add(flags);
    myState.requestFlush();
  }

  public <T> void processNotConsumed(TypedKey<? super T> reason, T data) {
    if (reason == EventContext.CELL_INVALIDATED) {
      JComponent component = myState.getHostComponent();
      component.invalidate();
      component.revalidate();
      component.repaint();
    }
  }
}
