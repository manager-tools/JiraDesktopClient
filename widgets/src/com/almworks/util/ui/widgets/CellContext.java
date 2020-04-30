package com.almworks.util.ui.widgets;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * CellContext is the presentation of a cell active or not active. It provides access to host, stored state, physical dimensions of cell,
 * and allows widget to request revalidation, repaint and focus traverse. Also it contains widely used utility methods.
 * CellContext doesn't guarantees that state isn't going to be lost (until context is active). Inactive context may immidiatly
 * forget any state values.
 */
public interface CellContext {
  /**
   * Checks if context is active (associated with active cell)
   */
  boolean isActive();

  /**
   * Store state value
   * @param key value key
   * @param value value to be stored
   * @param permanent not permanent value can be dropped from active cell with out any notification. Permanent value don't be lost
   * until cell is deactivated (inactive context may foreget any value immidiatly)
   * @param <T>
   */
  <T> void putStateValue(TypedKey<? super T> key, @Nullable T value, boolean permanent);

  /**
   * Return state value previously stored with {@link #putStateValue(org.almworks.util.TypedKey, Object, boolean)}.
   * If no value is stored (or value was dropped) null is returned
   */
  @Nullable
  <T> T getStateValue(TypedKey<? extends T> key);

  /**
   * Same as {@link #getStateValue(org.almworks.util.TypedKey)} but returns default value for missing or null value.
   * @see #getStateValue(org.almworks.util.TypedKey)
   */
  <T> T getStateValue(TypedKey<? extends T> key, T nullValue);

  /**
   * @return active cell associated with context or null if none
   */
  @Nullable
  HostCell getActiveCell();

  /**
   * @return Host of this context
   */
  WidgetHost getHost();

  /**
   * Asks child widget to calculate prefered width. Id identifies cell that is going to be (or already) accossiated with
   * child widget
   * @param id child cell identifier
   * @param child child widget
   * @param value child cell value
   * @return prefered width
   */
  <T> int getChildPreferedWidth(int id, Widget<? super T> child, T value);

  /**
   * Same as {@link #getChildPreferedWidth(int, Widget, Object)} but calculates height
   * @see #getChildPreferedWidth(int, Widget, Object)
   */
  <T> int getChildPreferedHeight(int id, Widget<? super T> child, T value, int width);

  /**
   * Cell X coordinate is host's space. Inactive cell can be located anywhere.
   */
  int getHostX();

  /**
   * Cell Y coordinate is host's space. Inactive cell can be located anywhere.
   */
  int getHostY();

  /**
   * Current cell width. Inactive cell has zero or negative width. Active cell may has zero width too.
   * @return width for active cell. Zero or negative for inactive cell.
   */
  int getWidth();

  /**
   * Current cell height.
   * @see #getWidth()
   */
  int getHeight();

  /**
   * Returns host bounds. If target is not null bounds are stored here and target is returned. If target is null new
   * Rectangle is created and returned.
   * @param target buffer to store cell bounds
   * @return rectangle with size and location of cell
   * @see #getHostX()
   * @see #getWidth()
   */
  @NotNull
  Rectangle getHostBounds(@Nullable Rectangle target);

  /**
   * Notify host that this cell requires keyboard focus. Only active cell can request focus. Calling this method on
   * inactive context has no effect.
   */
  void requestFocus();

  /**
   * Notify host that current cell requiers to be repainted. Calling on inactive context or zero-size cell has no effect
   */
  void repaint();

  /**
   * Request revalidation. Notify that this cell probably should change size or location.<br>
   * This request forces parent widget to relayout children but the parent cell itself is going to be kept (until parent
   * widget processes {@link com.almworks.util.ui.widgets.EventContext#CELL_INVALIDATED} event and invalidate own cell.
   */
  void invalidate();

  /**
   * Delete this cell and all descendant cells. 
   */
  void deleteAll();

  /**
   * Check if this cell is having keyboard focus. This checks only host local focus, the host itself may not has focus at the moment.
   * Only active cell can be focused.
   * @return true if host sends keyboard events to this cell first
   */
  boolean isFocused();

  /**
   * @return life component currently associated with the cell.
   */
  @Nullable
  JComponent getLiveComponent();

  /**
   * Removes life component from cell (if any)
   */
  void removeLiveComponent();

  /**
   * Ask host to deliver event to this cell later. The event is placed to the host event queue if cell is active. Calling
   * this method on inactive context has no effect
   * @param reason event type
   * @param data event data
   */
  <T> void postEvent(TypedKey<T> reason, T data);

  /**
   * Gets first or last child of this cell. Children are ordered by their identifiers (low ids first)
   * @param fromBeginning if true means child with lowest id, false means greatest
   * @return a child cell if cell is active and has atleast one active child. null otherwise
   */
  @Nullable
  HostCell getFirstChild(boolean fromBeginning);

  /**
   * Find child with specified id
   * @return child cell with specified id or null if cell isn't active or has no active child with this id
   */
  @Nullable
  HostCell findChild(int id);
}
