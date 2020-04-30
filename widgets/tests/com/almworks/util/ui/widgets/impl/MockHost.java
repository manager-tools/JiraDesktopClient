package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.genutil.Log;
import junit.framework.Assert;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;

public class MockHost<T> implements IWidgetHostComponent {
  private static final Log<MockHost> log = Log.get(MockHost.class);
  private static final FontRenderContext DEFAULT_FRC = new FontRenderContext(null, true, false);
  private final JComponent myComponent = new JLabel();
  private final HostComponentState<T> myState;
  private boolean myRepaintRequested = false;
  private Rectangle myRepaintRect = new Rectangle();
  private int myWidth = 100;
  private int myHeight = 20;
  private Cursor myCursor;

  @SuppressWarnings({"ThisEscapedInObjectConstruction"})
  public MockHost() {
    myState = new HostComponentState(this);
  }

  public void checkRepaintRequested() {
    Assert.assertTrue(myRepaintRequested);
    myRepaintRequested = false;
    myRepaintRect.setBounds(0, 0, 0, 0);
  }

  public void checkRepaint(int x, int y, int width, int height) {
    Assert.assertEquals(new Rectangle(x, y, width, height), myRepaintRect);
    checkRepaintRequested();
  }

  public void setSize(int width, int height) {
    int oldWidth = myWidth;
    int oldHeight = myHeight;
    myWidth = width;
    myHeight = height;
    myComponent.setBounds(0, 0, width, height);
    myState.hostReshaped(oldWidth, oldHeight);
  }

  public void setCursor(Cursor cursor) {
    myCursor = cursor;
  }

  public void repaint(int x, int y, int width, int height) {
    Rectangle rect = new Rectangle(x, y, width, height);
    if (myRepaintRect.isEmpty()) myRepaintRect.setBounds(rect);
    else myRepaintRect = myRepaintRect.union(rect);
    myRepaintRequested = true;
  }

  public void updateAll() {
    assert false;
  }

  public JComponent getHostComponent() {
    return myComponent;
  }

  public <T> T getHostValue(TypedKey<T> key) {
    assert false;
    return null;
  }

  public void fullRefresh() {
  }

  public void repaintAll() {
    assert false;
  }

  public int getWidth() {
    return myWidth;
  }

  public int getHeight() {
    return myHeight;
  }

  public Font getFont() {
    return myComponent.getFont();
  }

  public FontRenderContext getFontRenderContext() {
    return DEFAULT_FRC;
  }

  public void setRemovingComponent(Component component) {
  }

  @Override
  public void revalidate() {
  }

  @Override
  public void widgetRequestsFocus() {

  }

  public Color getForeground() {
    return myComponent.getForeground();
  }

  public Color getBackground() {
    return myComponent.getBackground();
  }

  public void activate() {
    if (myState.isActive())
      return;
    myCursor = null;
    myState.activate();
  }

  public void deactivate() {
    if (!myState.isActive())
      return;
    myState.deactivate();
  }

  public HostComponentState<T> getState() {
    return myState;
  }

  public void checkCursor(Cursor expected) {
    Assert.assertSame(expected, myCursor);
  }
}
