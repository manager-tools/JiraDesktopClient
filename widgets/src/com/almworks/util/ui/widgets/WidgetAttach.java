package com.almworks.util.ui.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Feature of widget to have static lifecycle. Calls to attach and detach notifies widget that some host is interested in
 * using the widget.
 * @see com.almworks.util.ui.widgets.Widget
 */
public interface WidgetAttach {
  /**
   * Static lifecycle is started. {@link #detach(WidgetHost)} is surely be called with same argument in future.
   * @param host
   */
  void attach(@NotNull WidgetHost host);

  /**
   * Static lifecycle is ended. The widget may still be used by other hosts. 
   * @param host
   */
  void detach(@NotNull WidgetHost host);
}
