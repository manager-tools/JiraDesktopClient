package com.almworks.util.ui.actions.dnd;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public interface DropHintProvider<H extends DropHint, C extends JComponent & DndComponentAdapter<H>> {
  /**
   *
   * @param component
   * @param p
   * @param context
   * @param transfer
   * @return true if hint update is needed
   */
  boolean prepareDropHint(C component, Point p, DragContext context, ContextTransfer transfer);

  /**
   *
   * @param component
   * @param context
   * @return drop hint, or null if not available
   */
  @Nullable
  H createDropHint(C component, DragContext context);

  void cleanContext(DragContext context);
}
