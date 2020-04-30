package com.almworks.util.ui.widgets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Cell lifecycle feature
 * @see com.almworks.util.ui.widgets.Widget
 */
public interface CellActivate {
  /**
   * Cell is created for widget and ready to hold state. All permanent state won't be lost until {@link #deactivate(HostCell, javax.swing.JComponent)}
   * is called. Once cell activated it surely be deactivated in future.
   * @param cell
   */
  void activate(@NotNull HostCell cell);

  /**
   * Cell is deactivated. If liveComponent is not null then cell has life component attached and now it is released and removed from
   * Swing component tree
   * @param cell
   * @param liveComponent
   */
  void deactivate(@NotNull HostCell cell, @Nullable JComponent liveComponent);
}
