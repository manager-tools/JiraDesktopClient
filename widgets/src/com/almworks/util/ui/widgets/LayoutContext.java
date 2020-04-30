package com.almworks.util.ui.widgets;

import java.awt.*;

/**
 * This context is passed to widget to layout children cells. Layout is the only way to create children.<br>
 * LayoutContext has target rectangle - area were children existence and location need to be updated. Widget may layout children
 * outside of target, but it has no effect.<br>
 * Some times host needs to update location of a particular cell in this case Widget's layout method is call with notnull
 * cell parameter.   
 * @see com.almworks.util.ui.widgets.Widget#layout(LayoutContext, Object, ModifiableHostCell)
 */
public interface LayoutContext extends CellContext {
  /**
   * Creates or updates location of child cell. Cell is identified by id. If widget associated with the is differs from widget
   * accossiated earlier the cell is treated as removed and recreated.
   * @param id cell identifier
   * @param widget widget controlling the cell
   * @param x X coordinate relative to parent cell
   * @param y Y coordinate relative to parent cell
   * @param width cell width
   * @param height cell height
   */
  void setChildBounds(int id, Widget<?> widget, int x, int y, int width, int height);

  void setChildBounds(int id, Widget<?> widget, int x, int y, int width, int height, HostCell.Purpose activate);

  void setChildBounds(int id, Widget<?> widget, Rectangle bounds);

  /**
   * @return X coordinate of target rectangle relative to the cell
   */
  int getTargetX();

  /**
   * @return X coordinate of target rectangle relative to the cell
   */
  int getTargetY();

  /**
   * @return target rectangle width
   */
  int getTargetWidth();

  /**
   * @return target rectangle height
   */
  int getTargetHeight();
}
