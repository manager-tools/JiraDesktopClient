package com.almworks.util.ui.widgets;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Event context is used to deliver events. Event consists of reason - the event type (passed as parameter) and event data
 * data is passed as part of EventContext. Also EventContext extends CellContext interface and provides access to cell.<br>
 * Some events are send to only one cell other are send the cell and all it's ancectors. Some events that are send to ancestors
 * are consumable - if a widget consumes event the dispatch is stopped and farther ancestors don't get the event.
 */
public interface EventContext extends CellContext {
  /**
   * Send after cell is resized or moved in host's coordinate space. Send only to cell that has life component or requested
   * reshape notifications.
   * If cell has life component the widget has to move or resize component according to new cell bounds
   */
  TypedKey<Object> CELL_RESHAPED = TypedKey.create("cellReshaped");
  /**
   * Send when the cell becomes focused.
   */
  TypedKey<Object> FOCUS_GAINED = TypedKey.create("focusGained");
  /**
   * Send when the cell losing focus. This event may not be delivered if cell is already deactivated.
   */
  TypedKey<Object> FOCUS_LOST = TypedKey.create("focusLost");
  /**
   * Send when descendant cell gains focus (send to all ancestors, not consumable)
   */
  TypedKey<HostCell> DESCENDANT_GAINED_FOCUS = TypedKey.create("descendantGainedFocus");
  /**
   * Send when descendant cell losing focus (send to all ancestors, not consumable)
   */
  TypedKey<HostCell> DESCENDANT_LOST_FOCUS = TypedKey.create("descendantLostFocus");
  /**
   * Send to focus owning cell and all it's ancestors until consumed. If any cell consumes the event than corresponding
   * Swing event is consumed too.
   */
  TypedKey<KeyEvent> KEY_EVENT = TypedKey.create("keyEvent");
  /**
   * When a cell required revalidation this event is sent to all it's ancestors until consumed. <br>
   * Widget may:
   * <ul>
   * <li>Ignore if it doesn't controll cell's space (tries to allocate prefered size)</li>
   * <li>Consume and invalidate itself. This means that widget controls the invalidated cell size and location, but if the current
   * cell is reshaped the layout of child may be changed. So the current cell requires own revalidation to allocate different space for decendant</li>
   * <li>Silently consume. If widget is sure that changes of prefered size of invalidated cell doesn't change the allocated space it
   * should consume the event and don't disturb it's own ancestors</li>
   * (send to all ancestors, consumable)
   */
  TypedKey<HostCell> CELL_INVALIDATED = TypedKey.create("cellInvalidated");
  /**
   * Wrapper for Swing mouse event. This event is sent when mouse event occurs or when mouse changes location in cells coordinate space
   * due to mouse move or cell reshape.
   * @see MouseEventData
   */
  TypedKey<MouseEventData> MOUSE = MouseEventData.KEY;
  /**
   * Focus traverse is performing. The widget should select child cell to be focused or focus itself. If widget isn't focusable
   * (and don't provide focusable children) it should ignore the event)
   * @see com.almworks.util.ui.widgets.FocusTraverse
   */
  TypedKey<FocusTraverse> FOCUS_TRAVERSE = FocusTraverse.KEY;
  /**
   * This event is send when cell value (may be) changed. {@link com.almworks.util.ui.widgets.impl.HostComponentState}
   * sends the event to all active cells when new value is set. Any widget that converts value for its children should send
   * the event if child value may change.<br>
   * This event has no attached data<br>
   * Because of the event is always should be send to all descendants, widgets do not need to handle the event just for resending
   * it to own children. Handle the event only when actual activity is required
   * To notify all descendants see {@link com.almworks.util.ui.widgets.util.WidgetUtil#postEventToAllDescendants(HostCell, org.almworks.util.TypedKey, Object) postEventToAllDescendants}
   * @see #repaint()
   * @see #invalidate() 
   */
  TypedKey<?> VALUE_CHANGED = TypedKey.create("valueChanged");

  /**
   * @param key event reason key
   * @return associated event data
   */
  @Nullable
  <T> T getData(TypedKey<? extends T> key);

  /**
   * Mark the event consumed. If event isn't consumable this call has no effect
   */
  void consume();

  /**
   * Set the host mouse cursor. This method has effect only when processing mouse events.
   * @param cursor
   */
  void setCursor(Cursor cursor);
}
