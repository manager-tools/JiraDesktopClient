package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.util.GridWidget;
import com.almworks.util.ui.widgets.util.SegmentsLayout;

public class WidgetTests extends BaseWidgetTestCase {
  public void testGridWidget() {
    myHost.setSize(20, 5);
    GridWidget<String> grid = new GridWidget<String>();
    myState.setWidget(grid);
    myState.setValue("a");
    myState.activate();
    myState.flushPendingEvents();
    grid.setLayout(new SegmentsLayout(1, 1), new SegmentsLayout(1, 1));
    grid.setDimension(2, 1);
    myState.flushPendingEvents();
    MockWidget<String> child1 = new MockWidget<String>();
    MockWidget<String> child2 = new MockWidget<String>();
    child1.setPrefSize(1, 5);
    child2.setPrefSize(1, 5);
    grid.addChild(child1);
    grid.setChild(1, 0, child2);
    assertEquals(2, myState.getPreferedWidth());
    assertEquals(5, myState.getPreferedHeight(2));
    myState.flushPendingEvents();
    child1.checkLastLayout(0, 0, 10, 5);
    child2.checkLastLayout(0, 0, 10, 5);
    HostCell cell1 = grid.findCell(myState.getRootCell(), 0, 0);
    HostCell cell2 = grid.findCell(myState.getRootCell(), 1, 0);
    assertSame(child1, cell1.getWidget());
    assertSame(child2, cell2.getWidget());
    checkBounds(cell1, 0, 0, 10, 5);
    checkBounds(cell2, 10, 0, 10, 5);
    child1.setPrefSize(15, 5);
    myState.flushPendingEvents();
    child1.checkLastLayout(0, 0, 17, 5);
    child2.checkLastLayout(0, 0, 3, 5);
    assertTrue(cell1.isActive());
    assertTrue(cell2.isActive());
    checkBounds(cell1, 0, 0, 17, 5);
    checkBounds(cell2, 17, 0, 3, 5);
  }

}
