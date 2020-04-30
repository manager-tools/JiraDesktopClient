package com.almworks.util.ui.widgets.impl;

import com.almworks.util.commons.Factory;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.widgets.EventContext;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.Widget;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Const;
import org.almworks.util.TypedKey;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

final class CellStack {
  private static final Log<CellStack> log = Log.get(CellStack.class);
  private static final Factory<CellStack> FACTORY = new Factory<CellStack>() {
    @Override
    public CellStack create() {
      return new CellStack();
    }
  };
  private HostCellImpl[] myCells = HostCellImpl.EMPTY_ARRAY;
  private Object[] myValues = Const.EMPTY_OBJECTS;
  private Rectangle[] myRectangles = AwtUtil.EMPTY_RECTANGLES;
  private boolean myBuildRectanles = false;
  private int mySize = 0;

  public static InstancePool<CellStack> createPool() {
    return new InstancePool<CellStack>(FACTORY);
  }

  public void clear() {
    trimTail(0);
    myBuildRectanles = false;
  }

  public void setBuildRectanles(boolean buildRectanles) {
    if (mySize != 0) throw new IllegalStateException();
    myBuildRectanles = buildRectanles;
  }

  public void buildToAncestor(HostCellImpl descendant, HostCellImpl ancestor, Object ancestorValue) {
    if (!ancestor.isActive()) return;
    if (mySize != 0) {
      log.error(this, "Not empty", mySize);
      return;
    }
    HostCellImpl current = descendant;
    while (current != null) {
      myCells = ArrayUtil.ensureCapacity(myCells, mySize + 1);
      myCells[mySize] = current;
      mySize++;
      if (current == ancestor) break;
      current = current.getParent();
    }
    if (current != ancestor) {
      log.error(this, "No path", descendant, ancestor);
      clear();
      return;
    }
    if (!ancestor.isActive()) {
      clear();
      return;
    }
    ArrayUtil.reverse(myCells, 0, mySize);
    assert myCells[0] == ancestor;
    myValues = ArrayUtil.ensureCapacity(myValues, myCells.length, false);
    myValues[0] = ancestorValue;
    if (myBuildRectanles) {
      myRectangles = ArrayUtil.ensureCapacity(myRectangles, myCells.length, true);
      buildClippedBounds(0);
    }
    for (int i = 1; i < mySize; i++) {
      HostCellImpl parent = myCells[i - 1];
      HostCellImpl child = myCells[i];
      if (!child.isActive()) {
        trimTail(i);
        return;
      }
      Object parentValue = myValues[i - 1];
      myValues[i] = getChildValue(parent, parentValue, child);
      buildClippedBounds(i);
    }
  }

  private void trimTail(int newSize) {
    if (newSize > mySize) {
      log.error(this, "Wrong new size", newSize, mySize);
      return;
    }
    if (myCells.length > newSize) Arrays.fill(myCells, newSize, Math.min(mySize, myCells.length), null);
    if (myValues.length > newSize) Arrays.fill(myValues, newSize, Math.min(mySize, myValues.length), null);
    mySize = newSize;
  }

  private static Object getChildValue(HostCell parent, Object parentValue, HostCell child) {
    Widget<Object> widget = (Widget<Object>) parent.getWidget();
    return widget.getChildValue(parent, child.getId(), parentValue);
  }

  public Object topValue() {
    if (isEmpty()) {
      log.error(this, "Not ready:topValue", mySize);
      //noinspection ReturnOfNull
      return null;
    }
    return myValues[mySize - 1];
  }

  public HostCellImpl topCell() {
    if (isEmpty()) {
      log.error(this, "Not ready:topCell", mySize);
      //noinspection ReturnOfNull
      return null;
    }
    return myCells[mySize - 1];
  }

  public Rectangle topRectangle() {
    if (!myBuildRectanles) throw new IllegalStateException();
    return myRectangles[mySize - 1];
  }

  public void pop() {
    if (isEmpty()) {
      log.error(this, "Not ready:pop", mySize);
      //noinspection ReturnOfNull
      return;
    }
    mySize--;
    myCells[mySize] = null;
    myValues[mySize] = null;
  }

  public boolean isEmpty() {
    return mySize == 0;
  }

  public <T> EventContextImpl sendEventToTop(TypedKey<? super T> reason, T event) {
    Object value = topValue();
    HostCellImpl cell = topCell();
    EventContextImpl context = new EventContextImpl(cell.getHostState(), reason, event);
    context.setCurrentCell(cell);
    Widget<Object> widget = (Widget<Object>) cell.getWidget();
    widget.processEvent(context, value, reason);
    return context;
  }

  public void push(HostCellImpl cell, Object value) {
    myCells = ArrayUtil.ensureCapacity(myCells, mySize + 1);
    myValues = ArrayUtil.ensureCapacity(myValues, mySize + 1);
    myRectangles = ArrayUtil.ensureCapacity(myRectangles, mySize + 1);
    myCells[mySize] = cell;
    myValues[mySize] = value;
    buildClippedBounds(mySize);
    mySize++;
  }

  private void buildClippedBounds(int index) {
    if (!myBuildRectanles) return;
    HostCellImpl cell = myCells[index];
    Rectangle rect = myRectangles[index];
    if (rect == null) {
      rect = new Rectangle();
      myRectangles[mySize] = rect;
    }
    cell.getHostBounds(rect);
    if (mySize > 0) AwtUtil.intersection(rect, myRectangles[mySize - 1], rect);
  }

  public void pushChild(HostCellImpl child) {
    if (mySize == 0) throw new IllegalStateException();
    Object value = getChildValue(myCells[mySize - 1], myValues[mySize - 1], child);
    push(child, value);
  }

  public boolean buildForCell(HostCellImpl cell, List<HostCellImpl> tmpPath) {
    tmpPath.clear();
    int index = -1;
    while (cell != null) {
      index = indexOfCell(cell);
      if (index >= 0) break;
      tmpPath.add(cell);
      cell = cell.getParent();
    }
    if (cell == null) return false;
    trimTail(index + 1);
    for (int i = tmpPath.size() - 1; i >= 0; i--) pushChild(tmpPath.get(i));
    tmpPath.clear();
    return true;
  }

  private int indexOfCell(HostCellImpl cell) {
    for (int i = mySize - 1; i >= 0; i--)
      if (myCells[i] == cell) return i;
    return -1;
  }

  public <T> boolean sendWhileNotConsumed(TypedKey<? super T> reason, T data) {
    EventContextImpl lastContext = null;
    while (!isEmpty()) {
      lastContext = sendEventToTop(reason, data);
      if (lastContext.isConsumed()) break;
      if (data == EventContext.CELL_INVALIDATED) {
        HostCellImpl cell = topCell();
        cell.getState().getLayoutManager().invalidate(cell);
      }
      pop();
    }
    return lastContext != null && lastContext.isConsumed();
  }

  public <T> void sendEventFromTopTo(HostCell ancestor, TypedKey<? super T> reason, T data) {
    while (!isEmpty() && topCell() != ancestor) {
      sendEventToTop(reason, data);
      pop();
    }
  }
}
