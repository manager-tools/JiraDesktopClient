package com.almworks.util.ui.widgets.util.list;

import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.widgets.GraphContext;

import javax.swing.*;

public interface RowPaintPolicy {
  /**
   * Paints single row
   * @param row current row index
   * @param y row y offset
   * @param height row height
   */
  void paint(GraphContext context, int row, int y, int height);

  RowPaintPolicy DEFAULT_ZEBRA = new Zebra();

  public static class Zebra implements RowPaintPolicy {
    @Override
    public void paint(GraphContext context, int row, int y, int height) {
      JComponent host = context.getHost().getHostComponent();
      context.setColor(ColorUtil.getStripeBackground(host.getBackground()));
      if (row %2 == 1) context.fillRect(0, y, context.getWidth(), height);
    }
  }
}
