package com.almworks.util.ui.widgets.impl;

import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.genutil.Log;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public abstract class BaseWidgetTestCase extends GUITestCase {
  private static final Log<BaseWidgetTestCase> log = Log.get(BaseWidgetTestCase.class);
  protected MockHost<String> myHost;
  protected HostComponentState<String> myState;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHost = new MockHost<String>();
    myHost.setSize(20, 10);
    myState = myHost.getState();
  }

  @Override
  protected void tearDown() throws Exception {
    myHost.deactivate();
    super.tearDown();
  }

  protected MouseEvent mouseMove(int x, int y) {
    return new MouseEvent(myHost.getHostComponent(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false, 0);
  }

  protected KeyEvent keyTyped(char keyChar) {
    return new KeyEvent(myHost.getHostComponent(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, 0, keyChar);
  }

  protected MouseWheelEvent mouseWheel(int x, int y, int wheel) {
    return new MouseWheelEvent(myHost.getHostComponent(), MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, wheel);
  }

  protected static void checkBounds(HostCell cell, int x, int y, int width, int height) {
    assertEquals(new Rectangle(x, y, width, height), cell.getHostBounds(new Rectangle()));
  }
}
