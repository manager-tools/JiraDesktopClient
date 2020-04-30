package com.almworks.util.components.plaf.macosx;

import com.almworks.util.components.AActionButton;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.swing.AwtUtil;

import java.awt.*;

/**
 * A variant of {@link AActionButton} for using in table corners
 * on Mac OS X.
 */
public class MacCornerButton extends AActionButton {
  private static final Color GRADIENT_COLOR = new Color(224, 224, 224);

  public MacCornerButton() {
    super();
  }

  public MacCornerButton(AnAction action) {
    super(action);
  }

  @Override
  public void paint(Graphics g) {
    if(g instanceof Graphics2D) {
      AwtUtil.applyRenderingHints(g);
      final Color c1;
      final Color c2;
      if(getModel().isPressed()) {
        c1 = GRADIENT_COLOR;
        c2 = Color.LIGHT_GRAY;
      } else {
        c1 = Color.WHITE;
        c2 = GRADIENT_COLOR;
      }

      final Graphics2D g2 = (Graphics2D)g.create();

      final int width = getWidth();
      final int halfHeight = getHeight() / 2;

      final GradientPaint gp1 = new GradientPaint(0, 0, c1, 0, halfHeight, c2);
      g2.setPaint(gp1);
      g2.fillRect(0, 0, width, halfHeight);

      final GradientPaint gp2 = new GradientPaint(0, halfHeight, c2, 0, getHeight(), c1);
      g2.setPaint(gp2);
      g2.fillRect(0, halfHeight, width, halfHeight);

      g2.dispose();
    }

    super.paint(g);
  }
}
