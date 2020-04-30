package com.almworks.util.ui.widgets.impl;

import com.almworks.util.components.SizeCalculator1D;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.Widget;
import com.almworks.util.ui.widgets.util.GridWidget;
import com.almworks.util.ui.widgets.util.ScrollPolicy;
import com.almworks.util.ui.widgets.util.ScrollWidget;
import com.almworks.util.ui.widgets.util.SegmentsLayout;

import javax.swing.*;

public class ScrollWidgetTests extends BaseWidgetTestCase {
  public void testScroll() {
    GridWidget<String> grid = new GridWidget();
    ScrollWidget<String> scroll = createAndActivate(grid, 20, 10);
    grid.setLayout(new SegmentsLayout(1, 1), new SegmentsLayout(1, 1));
    grid.setDimension(1, 100);
    MockWidget<String>[] children = new MockWidget[100];
    for (int i = 0; i < 100; i++) {
      MockWidget<String> child = new MockWidget<String>();
      child.setPrefSize(10, 5);
      children[i] = child;
      grid.addChild(child);
    }
    myState.flushPendingEvents();
    children[0].checkActiveCount(1);
    children[1].checkActiveCount(1);
    children[2].checkActiveCount(0);
    children[3].checkActiveCount(0);
    HostCell center = scroll.getCenterCell(myState.getRootCell());
    assertTrue(center.isActive());
    assertSame(grid, center.getWidget());

    checkBounds(center, 0, 0, 20, 500);
    myState.dispatchMouse(mouseMove(2, 2));
    myState.dispatchMouse(mouseWheel(2, 2, 2));
    checkBounds(center, 0, -2, 20, 500);
    children[0].checkActiveCount(1);
    children[1].checkActiveCount(1);
    children[2].checkActiveCount(1);
    children[3].checkActiveCount(0);
    children[0].checkLastMouseMove(2, 4);

    myState.dispatchMouse(mouseWheel(2, 2, 3));
    checkBounds(center, 0, -5, 20, 500);
    children[0].checkActiveCount(0);
    children[1].checkActiveCount(1);
    children[2].checkActiveCount(1);
    children[3].checkActiveCount(0);
    children[1].checkLastMouseMove(2, 2);
  }

  public void testChangeCenter() {
    ScrollWidget<String> scroll = createAndActivate(null, 20, 10);
    MockWidget<String> child1 = new MockWidget();
    child1.setPrefSize(30, 15);
    scroll.setCenter(child1);
    myState.flushPendingEvents();
    child1.checkActiveCount(1);
    HostCell center = scroll.getCenterCell(myState.getRootCell());
    checkBounds(center, 0, 0, 20, 15);

    MockWidget<String> child2 = new MockWidget();
    child2.setPrefSize(10, 40);
    scroll.setCenter(child2);
    myState.flushPendingEvents();
    child1.checkActiveCount(0);
    child2.checkActiveCount(1);
    assertFalse(center.isActive());
    center = scroll.getCenterCell(myState.getRootCell());
    checkBounds(center, 0, 0, 20, 40);
  }

  public void testExpandArea() {
    MockWidget<String> child = new MockWidget();
    child.setPrefSize(400, 300);
    ScrollWidget<String> scroll = createAndActivate(child, 200, 100, true);
    JScrollBar vert = scroll.getScrollBar(myState.getRootCell(), true);
    JScrollBar hor = scroll.getScrollBar(myState.getRootCell(), false);
    HostCell center = scroll.getCenterCell(myState.getRootCell());
    checkBar(vert, 300, 0, 100);
    checkBar(hor, 400, 0, 200);
    checkBounds(center, 0, 0, 400, 300);

    setBar(vert, 100);
    setBar(hor, 100);
    checkBounds(center, -100, -100, 400, 300);

    setHostSize(scroll, 300, 200);
    checkBounds(center, -100, -100, 400, 300);
    checkBar(vert, 300, 100, 200);
    checkBar(hor, 400, 100, 300);

    setHostSize(scroll, 400, 300);
    myState.flushPendingEvents();
    checkBounds(center, 0, 0, 400, 300);
    checkBar(vert, 300, 0, 300);
    checkBar(hor, 400, 0, 400);
  }

  public void testAllAreas() {
    MockWidget<String> luCorner = new MockWidget();
    MockWidget<String> ruCorner = new MockWidget();
    MockWidget<String> rbCorner = new MockWidget();
    MockWidget<String> lbCorner = new MockWidget();
    MockWidget<String> lStripe = new MockWidget();
    MockWidget<String> uStripe = new MockWidget();
    MockWidget<String> center = new MockWidget();
    luCorner.setPrefSize(30, 30);
    ruCorner.setPrefSize(30, 30);
    rbCorner.setPrefSize(30, 30);
    lbCorner.setPrefSize(30, 30);
    lStripe.setPrefSize(19, 19);
    uStripe.setPrefSize(21, 21);
    ScrollWidget<String> scroll = createAndActivate(center, 100, 100, true);
    scroll.setCenter(center);
    scroll.setLeftStripe(lStripe);
    scroll.setUpStripe(uStripe);
    scroll.setCorner(ScrollWidget.UP_LEFT, luCorner);
    scroll.setCorner(ScrollWidget.UP_RIGHT, ruCorner);
    scroll.setCorner(ScrollWidget.BOTTOM_RIGHT, rbCorner);
    scroll.setCorner(ScrollWidget.BOTTOM_LEFT, lbCorner);
    myState.flushPendingEvents();

    JScrollBar vert = scroll.getScrollBar(myState.getRootCell(), true);
    JScrollBar hor = scroll.getScrollBar(myState.getRootCell(), false);
    int vWidth = vert.getWidth();
    int hHeight = hor.getHeight();
    HostCell centerCell = scroll.getCenterCell(myState.getRootCell());
    HostCell leftCell = scroll.getLeftStripeCell(myState.getRootCell());
    HostCell upCell = scroll.getUpStripeCell(myState.getRootCell());
    HostCell luCell = scroll.getConerCell(myState.getRootCell(), ScrollWidget.UP_LEFT);
    HostCell ruCell = scroll.getConerCell(myState.getRootCell(), ScrollWidget.UP_RIGHT);
    HostCell rbCell = scroll.getConerCell(myState.getRootCell(), ScrollWidget.BOTTOM_RIGHT);
    HostCell lbCell = scroll.getConerCell(myState.getRootCell(), ScrollWidget.BOTTOM_LEFT);
    checkBounds(centerCell, 19, 21, 81, 79);
    checkBounds(leftCell, 0, 21, 19, 79);
    checkBounds(upCell, 19, 0, 81, 21);
    checkBounds(luCell, 0, 0, 19, 21);
    checkBounds(ruCell, 100, 0, vWidth, 21);
    checkBounds(rbCell, 100, 100, vWidth, hHeight);
    checkBounds(lbCell, 0, 100, 19, hHeight);

  }

  private void setBar(JScrollBar bar, int value) {
    bar.setValue(value);
    myState.flushPendingEvents();
  }

  private static void checkBar(JScrollBar bar, int max, int value, int extent) {
    assertEquals(0, bar.getMinimum());
    assertEquals(max, bar.getMaximum());
    assertEquals(value, bar.getValue());
    assertEquals(extent, bar.getModel().getExtent());
  }

  private ScrollWidget<String> createAndActivate(Widget<String> center, int clientWidth, int clientHeight) {
    return createAndActivate(center, clientWidth, clientHeight, false);
  }

  private ScrollWidget<String> createAndActivate(Widget<String> center, int clientWidth, int clientHeight, boolean horizontal) {
    myHost.setSize(clientWidth, clientHeight);
    ScrollWidget<String> scroll = new ScrollWidget(center, new ScrollPolicy.Fixed(SizeCalculator1D.fixedPixels(1), true), horizontal);
    myState.setWidget(scroll);
    myState.setValue("");
    myState.activate();
    setHostSize(scroll, clientWidth, clientHeight);
    return scroll;
  }

  private void setHostSize(ScrollWidget<String> scroll, int clientWidth, int clientHeight) {
    JScrollBar vert = scroll.getScrollBar(myState.getRootCell(), true);
    int dWidth = vert != null ? vert.getPreferredSize().width : 0;
    JScrollBar hor = scroll.getScrollBar(myState.getRootCell(), false);
    int dHeight = hor != null ? hor.getPreferredSize().height : 0;
    myHost.setSize(clientWidth + dWidth, clientHeight + dHeight);
  }
}
