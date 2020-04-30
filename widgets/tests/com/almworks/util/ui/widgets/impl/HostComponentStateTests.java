package com.almworks.util.ui.widgets.impl;

import java.awt.*;
import java.awt.image.BufferedImage;

public class HostComponentStateTests extends BaseWidgetTestCase {
  public void testSingleWidget() {
    myState.setValue("a");
    MockWidget<String> widget = new MockWidget<String>();
    myState.setWidget(widget);
    myState.activate();
    HostCellImpl root = myState.getRootCell();
    myHost.checkRepaint(0, 0, 20, 10);
    assertEquals(root.getHostBounds(null), new Rectangle(0, 0, 20, 10));
    widget.checkActiveCount(1);
    widget.requestRepaint();
    myHost.checkRepaint(0, 0, 20, 10);
    myState.setValue("b");
    myHost.checkRepaint(0, 0, 20, 10);
    myState.dispatchKeyEvent(keyTyped('q'));
    widget.ckeckLastKeyTyped('q');
    myState.dispatchMouse(mouseMove(2, 2));
    widget.checkLastMouseMove(2, 2);

    MockWidget<String> widget2 = new MockWidget<String>();
    myState.setWidget(widget2);
    myHost.checkRepaint(0, 0, 20, 10);
    widget.checkActiveCount(0);
    widget2.checkActiveCount(1);
  }

  public void testTwoWidgets() {
    MockWidget<String> top = new MockWidget<String>();
    MockWidget<String> child1 = new MockWidget<String>();
    top.addChild(child1, new Rectangle(5, 5, 5, 1));
    myState.activate();
    myState.setValue("a");
    myState.setWidget(top);
    myState.flushPendingEvents();
    top.checkLastLayout(0, 0, 20, 10);
    child1.checkLastLayout(0, 0, 5, 1);
    top.checkActiveCount(1);
    child1.checkActiveCount(1);
    myHost.checkRepaint(0, 0, 20, 10);
    child1.requestRepaint();
    myHost.checkRepaint(5, 5, 5, 1);
    myState.dispatchKeyEvent(keyTyped('w'));
    child1.ckeckLastKeyTyped('w');
    top.ckeckLastKeyTyped('w');
    myState.dispatchMouse(mouseMove(4, 4));
    child1.checkNoLastMouse();
    top.checkLastMouseMove(4, 4);
    myState.dispatchMouse(mouseMove(5, 5));
    child1.checkLastMouseMove(0, 0);
    top.checkLastMouseMove(5, 5);
    child1.setConsumeEvents(true);
    myState.dispatchKeyEvent(keyTyped('e'));
    child1.ckeckLastKeyTyped('e');
    top.ckeckLastKeyTyped((char) 0);
    myState.dispatchMouse(mouseMove(6, 5));
    child1.checkLastMouseMove(1, 0);
    top.checkLastMouseExit();
    myState.dispatchMouse(mouseMove(4, 4));
    child1.checkLastMouseExit();
    top.checkLastMouseMove(4, 4);
    myState.dispatchMouse(mouseMove(0, 0));

    MockWidget<String> child2 = new MockWidget<String>();
    top.addChild(child2, new Rectangle(4, 4, 5, 1));
    myState.flushPendingEvents();
    top.checkLastLayout(0, 0, 20, 10);
    child1.checkNotLayouted();
    child2.checkLastLayout(0, 0, 5, 1);
    myHost.checkRepaint(4, 4, 5, 1);

    top.setBounds(child1, new Rectangle(5, 3, 4, 1));
    myState.flushPendingEvents();
    myHost.checkRepaint(5, 3, 5, 3);
    top.checkLastLayout(0, 0, 20, 10);
    child1.checkLastLayout(0, 0, 4, 1);
    child2.checkNotLayouted();
    child1.requestRepaint();
    myHost.checkRepaint(5, 3, 4, 1);

    top.setBounds(child2, new Rectangle(100, 100, 5, 1));
    myState.flushPendingEvents();
    myHost.checkRepaint(4, 4, 5, 1);
    top.checkLastLayout(0, 0, 20, 10);
    child1.checkNotLayouted();
    child2.checkNotLayouted();
    child2.checkActiveCount(0);
    child1.checkActiveCount(1);
  }

  public void testChildLayout() {
    MockWidget<String> top = new MockWidget<String>();
    MockWidget<String> child1 = new MockWidget<String>();
    top.addChild(child1, new Rectangle(2, 2, 5, 5));
    myState.activate();
    myState.setWidget(top);
    top.checkLastLayout(0, 0, 20, 10);
    child1.checkLastLayout(0, 0, 5, 5);

    MockWidget<String> child11 = new MockWidget<String>();
    child1.addChild(child11, new Rectangle(1, 1, 2, 3));
    myState.flushPendingEvents();
    top.checkNotLayouted();
    child1.checkLastLayout(0, 0, 5, 5);
    child11.checkLastLayout(0, 0, 2, 3);
    myHost.checkRepaint(0, 0, 20, 10);

    child11.requestRepaint();
    myHost.checkRepaint(3, 3, 2, 3);

    child1.setBounds(child11, new Rectangle(1, 2, 2, 3));
    myState.flushPendingEvents();
    top.checkNotLayouted();
    child1.checkLastLayout(0, 0, 5, 5);
    child11.checkNotLayouted();
    myHost.checkRepaint(3, 3, 2, 4);
    child11.requestRepaint();
    myHost.checkRepaint(3, 4, 2, 3);

    child1.setBounds(child11, new Rectangle(1, 1, 2, 2));
    myState.flushPendingEvents();
    top.checkNotLayouted();
    child1.checkLastLayout(0, 0, 5, 5);
    child11.checkLastLayout(0, 0, 2, 2);
    child11.requestRepaint();
    myHost.checkRepaint(3, 3, 2, 4);
  }

  public void testResizeHost() {
    MockWidget<String> top = new MockWidget<String>();
    top.setForwardFocus(false);
    MockWidget<String> childL = new MockWidget<String>();
    top.addChild(childL, new Rectangle(10, 0, 10, 10));
    MockWidget<String> childD = new MockWidget<String>();
    top.addChild(childD, new Rectangle(0, 10, 10, 10));
    myHost.setSize(20, 20);
    myState.setWidget(top);
    myState.activate();
    myHost.checkRepaint(0, 0, 20, 20);
    top.checkActiveCount(1);
    childL.checkActiveCount(1);
    childD.checkActiveCount(1);
    top.checkLastLayout(0, 0, 20, 20);
    childL.checkLastLayout(0, 0, 10, 10);
    childD.checkLastLayout(0, 0, 10, 10);

    myHost.setSize(10, 20);
    top.checkLastLayout(0, 0, 10, 20);
    childL.checkNotLayouted();
    childD.checkNotLayouted();
    top.checkActiveCount(1);
    childL.checkActiveCount(0);
    childD.checkActiveCount(1);

    myHost.setSize(10, 10);
    top.checkLastLayout(0, 0, 10, 10);
    childL.checkNotLayouted();
    childD.checkNotLayouted();
    top.checkActiveCount(1);
    childL.checkActiveCount(0);
    childD.checkActiveCount(0);

    myHost.setSize(20, 10);
    top.checkLastLayout(0, 0, 20, 10);
    childL.checkLastLayout(0, 0, 10, 10);
    childD.checkNotLayouted();
    top.checkActiveCount(1);
    childL.checkActiveCount(1);
    childD.checkActiveCount(0);

    myHost.setSize(20, 20);
    top.checkLastLayout(0, 0, 20, 20);
    childL.checkNotLayouted();
    childD.checkLastLayout(0, 0, 10, 10);
    top.checkActiveCount(1);
    childL.checkActiveCount(1);
    childD.checkActiveCount(1);
  }

  public void testMouseMoveAndExit() {
    MockWidget<String> top = new MockWidget<String>();
    MockWidget<String> child1 = new MockWidget<String>();
    top.addChild(child1, new Rectangle(5, 5, 20, 20));
    MockWidget<String> child11 = new MockWidget<String>();
    child1.addChild(child11, new Rectangle(5, 5, 10, 10));
    child11.setConsumeEvents(true);
    myState.setWidget(top);
    myState.activate();
    myHost.setSize(25, 15);

    myHost.checkRepaint(0, 0, 25, 25);
    top.requestRepaint();
    myHost.checkRepaint(0, 0, 25, 15);
    child1.requestRepaint();
    myHost.checkRepaint(5, 5, 20, 20);
    child11.requestRepaint();
    myHost.checkRepaint(10, 10, 10, 10);
    
    myState.dispatchMouse(mouseMove(1, 1));
    top.checkLastMouseMove(1, 1);
    myState.dispatchMouse(mouseMove(6, 6));
    child1.checkLastMouseMove(1, 1);
    top.checkLastMouseMove(6, 6);
    myState.dispatchMouse(mouseMove(11, 11));
    child11.checkLastMouseMove(1, 1);
    child1.checkLastMouseExit();
    top.checkLastMouseExit();
  }

  public void testRepaintClip() {
    MockWidget<String> top = new MockWidget<String>();
    MockWidget<String> child = new MockWidget<String>();
    top.addChild(child, new Rectangle(3, 3, 2, 2));
    myState.setWidget(top);
    myState.activate();
    myHost.checkRepaint(0, 0, 20, 10);

    top.setBounds(child, new Rectangle(3, 4, 2, 2));
    myState.flushPendingEvents();
    myHost.checkRepaint(3, 3, 2, 3);

    top.setBounds(child, new Rectangle(4, 4, 2, 2));
    myState.flushPendingEvents();
    myHost.checkRepaint(3, 4, 3, 2);

    top.setBounds(child, new Rectangle(4, 3, 2, 2));
    myState.flushPendingEvents();
    myHost.checkRepaint(4, 3, 2, 3);

    top.setBounds(child, new Rectangle(3, 3, 2, 2));
    myState.flushPendingEvents();
    myHost.checkRepaint(3, 3, 3, 2);
  }

  public void testMouseExitOnChangeBounds() {
    MockWidget<String> top = new MockWidget<String>();
    MockWidget<String> child = new MockWidget<String>();
    top.addChild(child, new Rectangle(2, 2, 5, 5));
    myState.setWidget(top);
    myState.activate();
    myHost.checkRepaint(0, 0, 20, 10);

    myState.dispatchMouse(mouseMove(3, 3));
    child.checkLastMouseMove(1, 1);
    top.setBounds(child, new Rectangle(1, 1, 5, 5));
    myState.flushPendingEvents();
    child.checkLastMouseMove(2, 2);
    top.setBounds(child, new Rectangle(2, 2, 4, 4));
    myState.flushPendingEvents();
    child.checkLastMouseMove(1, 1);

    top.setBounds(child, new Rectangle(4, 4, 4, 4));
    myState.flushPendingEvents();
    child.checkLastMouseExit();
    top.setBounds(child, new Rectangle(1, 1, 5, 5));
    myState.flushPendingEvents();
    child.checkLastMouseMove(2, 2);
    top.setBounds(child, new Rectangle(4, 4, 4, 4));
    myState.flushPendingEvents();
    child.checkLastMouseExit();
    top.setBounds(child, new Rectangle(1, 1, 4, 4));
    myState.flushPendingEvents();
    child.checkLastMouseMove(2, 2);
  }

  public void testPaint() {
    myHost.setSize(20, 20);
    MockWidget<String> top = new MockWidget<String>();
    MockWidget<String> child = new MockWidget<String>();
    top.addChild(child, new Rectangle(2, 2, 5, 5));
    MockWidget<String> inner = new MockWidget<String>();
    child.addChild(inner, new Rectangle(2, 2, 10, 10));
    myState.setWidget(top);
    myState.activate();

    inner.setColor(Color.WHITE);
    BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
    myState.paint(image.createGraphics());
    checkPaintedBounds(image, new Rectangle(4, 4, 3, 3));
  }

  private void checkPaintedBounds(BufferedImage image, Rectangle rectangle) {
    for (int x = 0; x < image.getWidth(); x++)
      for (int y = 0; y < image.getHeight(); y++) {
        String point = x + "@" + y;
        if (rectangle.contains(x, y)) assertNotEqual(point, 0, image.getRGB(x, y));
        else assertEquals(point, 0, image.getRGB(x, y));
      }
  }

}
