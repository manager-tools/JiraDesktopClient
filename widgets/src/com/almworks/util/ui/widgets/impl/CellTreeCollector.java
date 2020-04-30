package com.almworks.util.ui.widgets.impl;

import com.almworks.util.commons.Factory;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.Collections15;

import java.util.List;

final class CellTreeCollector {
  private static final Log<CellTreeCollector> log = Log.get(CellTreeCollector.class);
  private final List<HostCellImpl> myTmpList = Collections15.arrayList();
  private final List<HostCellImpl> myCollected = Collections15.arrayList();
  private int myPosition = 0;
  public static final Factory<CellTreeCollector> FACTORY = new Factory<CellTreeCollector>() {
    @Override
    public CellTreeCollector create() {
      return new CellTreeCollector();
    }
  };

  public void addCell(HostCellImpl cell) {
    myCollected.add(cell);
  }

  public boolean isAllCollected() {
    return myPosition == myCollected.size();
  }

  public boolean getNextChildren() {
    myTmpList.clear();
    while (!isAllCollected()) {
      HostCellImpl parent = myCollected.get(myPosition);
      myPosition++;
      if (parent.isActive()) {
        parent.getChildrenImpl(myTmpList);
        if (!myTmpList.isEmpty()) return true;
      }
    }
    return false;
  }

  public int getChildCount() {
    return myTmpList.size();
  }

  public HostCellImpl getChild(int index) {
    return myTmpList.get(index);
  }

  public List<HostCellImpl> getCollected() {
    return myCollected;
  }

  public void clear() {
    myCollected.clear();
    myTmpList.clear();
    myPosition = 0;
  }
}
