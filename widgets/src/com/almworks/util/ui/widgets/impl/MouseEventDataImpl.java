package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.MouseEventData;
import com.almworks.util.ui.widgets.genutil.Log;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

class MouseEventDataImpl implements MouseEventData {
  private static final Log<MouseEventDataImpl> log = Log.get(MouseEventDataImpl.class);
  public static final MouseEventData EXIT = new MouseEventDataImpl(-1, -1, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0);
  private int myX;
  private int myY;
  private final int myId;
  private final int myButton;
  private final int myScrollType;
  private final int myUnitToScroll;
  private final int myWheelRotation;

  private MouseEventDataImpl(int x, int y, int id, int button, int scrollType, int unitToScroll, int wheelRotation) {
    myX = x;
    myY = y;
    myId = id;
    myButton = button;
    myScrollType = scrollType;
    myUnitToScroll = unitToScroll;
    myWheelRotation = wheelRotation;
  }

  public static MouseEventDataImpl create(int id, int x, int y, int button, @Nullable MouseEvent event) {
    if (id == MouseEvent.MOUSE_ENTERED) id = MouseEvent.MOUSE_MOVED;
    switch (id) {
      case MouseEvent.MOUSE_MOVED: return new MouseEventDataImpl(x, y, id, 0, 0, 0, 0);
      case MouseEvent.MOUSE_EXITED: throw new IllegalArgumentException("Use constant for exit event");
      case MouseEvent.MOUSE_CLICKED:
      case MouseEvent.MOUSE_PRESSED:
      case MouseEvent.MOUSE_RELEASED: return new MouseEventDataImpl(x, y, id, button, 0, 0, 0);
      case MouseEvent.MOUSE_DRAGGED: return new MouseEventDataImpl(x, y, id, 0, 0, 0, 0);
      case MouseEvent.MOUSE_WHEEL:
        return event instanceof MouseWheelEvent ? createMouseWheel(x, y, button, (MouseWheelEvent) event) : new MouseEventDataImpl(x, y, id, button, 0, 0, 0);
      default: assert false;
        log.errorStatic("Unknown id", id);
        return null;
    }

  }

  private static MouseEventDataImpl createMouseWheel(int x, int y, int button, MouseWheelEvent event) {
    return new MouseEventDataImpl(x, y, MouseEvent.MOUSE_WHEEL, button, event.getScrollType(), event.getUnitsToScroll(), event.getWheelRotation());
  }

  public int getX() {
    return myX;
  }

  public int getY() {
    return myY;
  }

  public int getEventId() {
    return myId;
  }

  public int getButton() {
    return myButton;
  }

  @Override
  public int getScrollType() {
    return myScrollType;
  }

  @Override
  public int getUnitToScroll() {
    return myUnitToScroll;
  }

  @Override
  public int getWheelRotation() {
    return myWheelRotation;
  }

  @Override
  public String toString() {
    int id = getEventId();
    String name;
    String button = "";
    switch (id) {
    case MouseEvent.MOUSE_MOVED: name = "Move"; break;
    case MouseEvent.MOUSE_EXITED: name = "Exit"; break;
    case MouseEvent.MOUSE_CLICKED: name = "Click"; button = String.valueOf(myButton); break;
    case MouseEvent.MOUSE_PRESSED: name = "Press"; button = String.valueOf(myButton); break;
    case MouseEvent.MOUSE_RELEASED: name = "Release"; button = String.valueOf(myButton); break;
    case MouseEvent.MOUSE_DRAGGED: name = "Drag"; break;
    case MouseEvent.MOUSE_WHEEL: name = "Wheel"; break;
    default: name = "Unk" + id;
    }
    return "Mouse" + id + "@" + myX + "x" + myY + (button.length() > 0 ? "(" + myButton + ")" : "");
  }

  void setX(int x) {
    myX = x;
  }

  void setY(int y) {
    myY = y;
  }
}
