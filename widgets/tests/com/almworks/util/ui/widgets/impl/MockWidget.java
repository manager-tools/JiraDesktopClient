package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.util.ActiveCellCollector;
import com.almworks.util.ui.widgets.util.WidgetChildList;
import junit.framework.Assert;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

public class MockWidget<T> implements Widget<T> {
  private final WidgetChildList<T> myChildren = new WidgetChildList<T>();
  private final ActiveCellCollector myCells = new ActiveCellCollector();
  private final List<Rectangle> myChildBounds = Collections15.arrayList();
  private final Dimension myPrefSize = new Dimension();
  private final Rectangle myLastLayoutTarget = new Rectangle(-1, -1, -1, -1);
  private LastMouse myLastMouse = null;
  private char myLastTyped = 0;
  private boolean myConsumeEvents = false;
  private boolean myForwardFocus = true;
  private Color myColor;

  @Override
  public int getPreferedWidth(@NotNull CellContext context, @Nullable T value) {
    if (myPrefSize.width + myPrefSize.height > 1) return myPrefSize.width;
    int max = 0;
    for (Rectangle rect : myChildBounds) max = Math.max(max, rect.x + rect.width);
    return max;
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable T value) {
    if (myPrefSize.width + myPrefSize.height > 1) return myPrefSize.height;
    int max = 0;
    for (Rectangle rect : myChildBounds) max = Math.max(max, rect.y + rect.height);
    return max;
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable T value) {
    if (myColor == null) return;
    Graphics2D g = context.getGraphics();
    g.setColor(myColor);
    g.fillRect(0, 0, context.getWidth(), context.getHeight());
  }

  @Override
  public WidgetAttach getAttach() {
    return myChildren;
  }

  @Override
  public CellActivate getActivate() {
    return myCells;
  }

  @Override
  public void updateUI(HostCell cell) {}

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
    if (MouseEventData.KEY == reason) processMouse(context.getData(MouseEventData.KEY));
    else if (EventContext.KEY_EVENT == reason) processKey(context.getData(EventContext.KEY_EVENT));
    else if (FocusTraverse.KEY == reason) processFocus(context, context.getData(FocusTraverse.KEY));
    else return;
    if (myConsumeEvents) context.consume();
  }

  public void setPrefSize(int width, int height) {
    myPrefSize.setSize(width, height);
    myCells.revalidate();
  }

  private void processFocus(CellContext context, FocusTraverse data) {
    if (data.isTraverse()) data.defaultTraverse(context, 0, myChildren.size() - 1);
    else if (myForwardFocus) {
      HostCell child = context.getFirstChild(data.isForward());
      if (child != null) data.moveToChild(child);
      else data.focusMe();
    } else data.focusMe();
  }

  private void processKey(KeyEvent data) {
    if (data.getID() != KeyEvent.KEY_TYPED) Assert.fail();
    Assert.assertEquals(0, myLastTyped);
    myLastTyped = data.getKeyChar();
  }

  private void processMouse(MouseEventData data) {
    int id = data.getEventId();
    if (id == MouseEvent.MOUSE_EXITED) myLastMouse = new LastMouse(MouseEvent.MOUSE_EXITED, 0, 0, 0);
    else {
      boolean button = (id & (MouseEvent.MOUSE_PRESSED | MouseEvent.MOUSE_RELEASED | MouseEvent.MOUSE_CLICKED)) != 0;
      myLastMouse = new LastMouse(data.getEventId(), data.getX(), data.getY(), button ? data.getButton() : -1);
    }
  }

  public void checkLastMouse(int eventId, int x, int y, int button) {
    Assert.assertEquals(myLastMouse, new LastMouse(eventId, x, y, button));
    myLastMouse = null;
  }

  public void checkLastMouseExit() {
    checkLastMouse(MouseEvent.MOUSE_EXITED, 0, 0, 0);
  }

  public void checkLastMouseMove(int x, int y) {
    checkLastMouse(MouseEvent.MOUSE_MOVED, x, y, 0);
  }

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable T value) {
    return value;
  }

  @Override
  public void layout(LayoutContext context, T value, @Nullable ModifiableHostCell cell) {
    if (cell != null) layoutSingle(context, value, cell);
    else layoutAll(context, value);
    myLastLayoutTarget.setBounds(context.getTargetX(), context.getTargetY(), context.getTargetWidth(), context.getTargetHeight());
  }

  private void layoutAll(LayoutContext context, T value) {
    for (int i = 0; i < myChildBounds.size(); i++) {
      Rectangle bounds = myChildBounds.get(i);
      context.setChildBounds(i, myChildren.get(i), bounds);
    }
  }

  private void layoutSingle(LayoutContext context, T value, ModifiableHostCell cell) {
    int id = cell.getId();
    if (id >= myChildren.size()) return;
    context.setChildBounds(id, myChildren.get(id), myChildBounds.get(id));
  }

  public void checkActiveCount(int count) {
    Assert.assertEquals(count, myCells.size());
  }

  public void requestRepaint() {
    myCells.repaint();
  }

  public void ckeckLastKeyTyped(char c) {
    Assert.assertEquals(c, myLastTyped);
    myLastTyped = 0;
  }

  public void checkNoLastMouse() {
    Assert.assertNull(myLastMouse);
  }

  public void addChild(MockWidget<T> widget, Rectangle rectangle) {
    assert myChildren.size() == myChildBounds.size();
    myChildren.addChild(widget);
    myChildBounds.add(rectangle);
    myCells.revalidate();
  }

  public void setConsumeEvents(boolean consumeEvents) {
    myConsumeEvents = consumeEvents;
  }

  public void setForwardFocus(boolean forwardFocus) {
    myForwardFocus = forwardFocus;
  }

  public void checkNotLayouted() {
    Assert.assertFalse(String.valueOf(myLastLayoutTarget), myLastLayoutTarget.width >= 0 && myLastLayoutTarget.height >= 0);
    myLastLayoutTarget.setBounds(-1, -1, -1, -1);
  }

  public void checkLastLayout(int targetX, int targetY, int targetWidth, int targetHeight) {
    Assert.assertEquals(new Rectangle(targetX, targetY, targetWidth, targetHeight), myLastLayoutTarget);
    myLastLayoutTarget.setBounds(-1, -1, -1, -1);
  }

  public void setBounds(MockWidget<T> child, Rectangle bounds) {
    int count = 0;
    for (Widget<?> widget : myChildren) {
      if (child == widget) break;
      count++;
    }
    if (count >= myChildren.size()) Assert.fail(String.valueOf(child));
    myChildBounds.set(count, bounds);
    myCells.revalidate();
  }

  public void setColor(Color color) {
    myColor = color;
  }

  private static class LastMouse {
    private final int myX;
    private final int myY;
    private final int myButton;
    private final int myEvent;

    private LastMouse(int event, int x, int y, int button) {
      myEvent = event;
      myX = x;
      myY = y;
      myButton = button;
    }

    @Override
    public String toString() {
      return myEvent + "@" + myX + "x" + myY + ":" + myButton;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != LastMouse.class) return false;
      LastMouse other = (LastMouse) obj;
      return myX == other.myX && myY == other.myY && myEvent == other.myEvent && myButton == other.myButton;
    }

    @Override
    public int hashCode() {
      return myX ^ myY ^ myEvent ^ myButton;
    }
  }
}
