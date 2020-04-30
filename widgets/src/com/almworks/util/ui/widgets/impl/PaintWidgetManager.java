package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.Widget;
import com.almworks.util.ui.widgets.genutil.Log;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class PaintWidgetManager {
  private static final Log<PaintWidgetManager> log = Log.get(PaintWidgetManager.class);
  private final HostComponentState<?> myState;
  private final InstancePool<CellTreeCollector> myCellCollector = new InstancePool<CellTreeCollector>(CellTreeCollector.FACTORY);
  private final InstancePool<CellStack> myCellStack = CellStack.createPool();
  private final InstancePool<ArrayList<HostCellImpl>> myTmpList = InstancePool.listPool();

  public PaintWidgetManager(HostComponentState<?> state) {
    myState = state;
  }

  public void paint(Graphics2D g) {
    Rectangle clip = g.getClipBounds();
    if (clip == null) clip = new Rectangle(0, 0, myState.getHost().getWidth(), myState.getHost().getHeight());
    CellTreeCollector collector = myCellCollector.getInstance();
    collector.addCell(myState.getRootCell());
    while (collector.getNextChildren()) {
      for (int i = 0; i < collector.getChildCount(); i++) {
        HostCellImpl cell = collector.getChild(i);
        if (cell.intersects(clip))
          collector.addCell(cell);
      }
    }
    CellStack stack = myCellStack.getInstance();
    stack.setBuildRectanles(true);
    stack.push(myState.getRootCell(), myState.getValue());
    ArrayList<HostCellImpl> tmpList = myTmpList.getInstance();
    GraphContextImpl context = new GraphContextImpl(myState, g, clip);
    try {
      List<HostCellImpl> collectedCells = collector.getCollected();
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < collectedCells.size(); i++) {
        HostCellImpl cell = collectedCells.get(i);
        if (!cell.isActive() || !stack.buildForCell(cell, tmpList)) continue;
        if (stack.topCell() != cell) {
          log.error(this, "Wrong cell", cell, stack.topCell());
          continue;
        }
        Widget<Object> widget = (Widget<Object>) cell.getWidget();
        if (context.prepare(cell, stack.topRectangle()))
          widget.paint(context, stack.topValue());
      }
    } finally {
      context.dispose();
      collector.clear();
      myCellCollector.release(collector);
      stack.clear();
      myCellStack.release(stack);
      tmpList.clear();
      myTmpList.release(tmpList);
    }
  }
}
