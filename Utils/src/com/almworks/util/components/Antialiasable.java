package com.almworks.util.components;

import java.awt.*;

public interface Antialiasable {
  void setAntialiased(boolean antialiased);

  boolean isAntialiased();

  class AAUtil {

    public static void endAntiAliasPaint(RenderingHints savedHints, Graphics g) {
      if (savedHints != null) {
        ((Graphics2D) g).setRenderingHints(savedHints);
      }
    }

    public static RenderingHints beginAntiAliasPaint(Antialiasable component, Graphics g) {
      RenderingHints savedHints = null;
      if (component.isAntialiased()) {
        Graphics2D g2 = (Graphics2D) g;
        savedHints = g2.getRenderingHints();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      }
      return savedHints;
    }
  }
}
