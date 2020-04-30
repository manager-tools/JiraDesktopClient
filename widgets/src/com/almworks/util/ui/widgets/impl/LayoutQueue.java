package com.almworks.util.ui.widgets.impl;

import com.almworks.util.collections.IntArray;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

final class LayoutQueue {
  private final List<HostCellImpl> myCells = Collections15.arrayList();
  private final IntArray myTargets = new IntArray();
  private int myFirst = 0;

  public void addToLayout(HostCellImpl cell) {
    addToLayout(cell, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  public void addToLayout(HostCellImpl cell, int x, int y, int width, int height) {
    if (width <= 0 || height <= 0 || !cell.isActive()) return;
    assert myCells.size() * 4 == myTargets.size();
    myCells.add(cell);
    myTargets.add(x);
    myTargets.add(y);
    myTargets.add(width);
    myTargets.add(height);
  }

  public boolean isEmpty() {
    return myFirst == myCells.size();
  }

  public void pollFirst() {
    if (myFirst >= myCells.size()) throw new NoSuchElementException();
    myFirst++;
  }

  @Nullable
  public HostCellImpl setupTarget(LayoutContextImpl context) {
    if (myFirst >= myCells.size()) throw new NoSuchElementException();
    HostCellImpl cell = myCells.get(myFirst);
    if (!cell.isActive()) return null;
    int firstTarget = 4*myFirst;
    int x = myTargets.get(firstTarget);
    int y = myTargets.get(firstTarget + 1);
    int width = myTargets.get(firstTarget + 2);
    int height = myTargets.get(firstTarget + 3);
    boolean setTarget;
    if (x == Integer.MIN_VALUE && y == Integer.MIN_VALUE && width == Integer.MAX_VALUE && height == Integer.MAX_VALUE) {
      x = cell.getHostX();
      y = cell.getHostY();
      width = cell.getWidth();
      height = cell.getHeight();
      setTarget = false;
    } else setTarget = true;
    context.setTarget(x, y, width, height, setTarget);
    return context.isTargetEmpty() ? null : cell;
  }

  public void clear() {
    myFirst = 0;
    myCells.clear();
    myTargets.clear();
  }
}
