package com.almworks.util.components.plaf.macosx;

import com.almworks.util.ui.swing.SwingTreeUtil;

import javax.swing.*;
import java.awt.*;

/**
 * A variation of ScrollPaneLayout that keeps the lower right corner free
 * when only a single scrollbar is shown and the scrollpane is at the
 * edge of its window.
 */
class MacCornerScrollPaneLayout extends ScrollPaneLayout {
  @Override
  public void layoutContainer(Container container) {
    super.layoutContainer(container);
    if(isSingleScrollbar() && isAtWindowEdge(container)) {
      adjustVerticalScrollbar();
      adjustHorizontalScrollbar();
    }
  }

  private boolean isSingleScrollbar() {
    return isSingleScrollbar(vsb, hsb) || isSingleScrollbar(hsb, vsb);
  }

  private boolean isSingleScrollbar(JScrollBar target, JScrollBar other) {
    return target != null && target.isVisible()
      && (other == null || !other.isVisible());
  }

  private boolean isAtWindowEdge(Container container) {
    if(container.isShowing()) {
      final Window window = SwingTreeUtil.getOwningWindow(container);
      if(window != null) {
        final Point cmp = getLowerRightCorner(container);
        final Point win = getLowerRightCorner(window);
        return cmp.equals(win);
      }
    }
    return false;
  }

  private Point getLowerRightCorner(Component comp) {
    final Point pos = comp.getLocationOnScreen();
    final Dimension size = comp.getSize();
    pos.translate(size.width, size.height);
    return pos;
  }

  private void adjustVerticalScrollbar() {
    if(isSingleScrollbar(vsb, hsb)) {
      final Rectangle bounds = vsb.getBounds();
      bounds.height = Math.max(0, bounds.height - bounds.width);
      vsb.setBounds(bounds);
    }
  }

  private void adjustHorizontalScrollbar() {
    if(isSingleScrollbar(hsb, vsb)) {
      final Rectangle bounds = hsb.getBounds();
      bounds.width = Math.max(0, bounds.width - bounds.height);
      hsb.setBounds(bounds);
    }
  }
}
