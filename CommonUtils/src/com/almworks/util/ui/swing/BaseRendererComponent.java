package com.almworks.util.ui.swing;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
@SuppressWarnings({"RefusedBequest"})
public class BaseRendererComponent extends JComponent {
  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void validate() {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void revalidate() {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void repaint(long tm, int x, int y, int width, int height) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void repaint(Rectangle r) { }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void invalidate() {}
}
