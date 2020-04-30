package com.almworks.util.ui.widgets;

import org.almworks.util.TypedKey;

/**
 * Wrapper for AWT mouse event. X and Y coordinates are relative to the cell. Pressed button and scrolling values are
 * available if applicable to the event type other wise null values returned.<br>
 * This event wraps all kinds of swing mouse events except mouse-enter, the mouse-move event is sent instead. Also mouse-move
 * (or mouse-exit) event is sent when cell changes location due to relayout.
 * @see com.almworks.util.ui.widgets.Widget#processEvent(EventContext, Object, org.almworks.util.TypedKey)
 */
public interface MouseEventData {
  TypedKey<MouseEventData> KEY = TypedKey.create("mouse");

  /**
   * @return X location of mouse pointer relative to the cell. Not applicable to mouse-exit event
   */
  int getX();

  /**
   * @return Y location of mouse pointer relative to the cell. Not applicable to mouse-exit event
   */
  int getY();

  /**
   * @return AWT mouse id. MOUSE_ENTERED is replaced with MOUSE_MOVE
   * @see java.awt.event.MouseEvent
   */
  int getEventId();

  /**
   * @return changed button
   * @see java.awt.event.MouseEvent#getButton()
   */
  int getButton();

  /**
   * @return mouse wheel changes
   * @see java.awt.event.MouseWheelEvent#getScrollType()
   */
  int getScrollType();

  /**
   * @return mouse wheel changes
   * @see java.awt.event.MouseWheelEvent#getUnitsToScroll()
   */
  int getUnitToScroll();

  /**
   * @return mouse wheel changes
   * @see java.awt.event.MouseWheelEvent#getWheelRotation()  
   */
  int getWheelRotation();
}
