package com.almworks.util.components.plaf;

import com.almworks.util.NoObfuscation;
import com.almworks.util.components.AToolbarButton;
import com.almworks.util.ui.ColorUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class AToolbarButtonUI extends BasicButtonUI implements NoObfuscation {
  private static final Insets BUTTON_INSETS = new Insets(2, 2, 2, 2);
  private static final EmptyBorder BUTTON_BORDER = new EmptyBorder(4, 4, 4, 4);

  private static final ComponentUI INSTANCE = new AToolbarButtonUI();

  private static final float[] HIGHLIGHT_BRIGHTNESSES = new float[]{0.85F, 0.82F, 0.71F, 0.60F, 0.49F};
  private static final float[] HIGHLIGHT_SATURATIONS = new float[]{0.02F, 0.13F, 0.27F, 0.43F, 0.61F};

  public static ComponentUI createUI(JComponent c) {
    return INSTANCE;
  }

  public void installUI(JComponent c) {
    super.installUI(c);
    AToolbarButton button = (AToolbarButton) c;
    button.setFocusable(false);
    button.setRolloverEnabled(true);
    button.setMargin(BUTTON_INSETS); // not used
    button.setBorder(BUTTON_BORDER);
    button.setHighlight(UIManager.getColor("AToolbarButton.highlight"));
  }

  public void paint(Graphics g, JComponent c) {
    AToolbarButton button = (AToolbarButton) c;
    ButtonModel model = button.getModel();
    Color highlight = button.getHighlight();
    if (model != null && highlight != null) {
      if (model.isEnabled()) {
        Color background = getFillColor(highlight, model);
        if (background != null) {
          paintFrame(c, g, background, highlight);
        }
      } else {
        if (model.isSelected()) {
          Color background = ColorUtil.adjustHSB(highlight, -1, 0, HIGHLIGHT_BRIGHTNESSES[0]);
          highlight = ColorUtil.adjustHSB(highlight, -1, 0, -1);
          paintFrame(c, g, background, highlight);
        }
      }
    }
    super.paint(g, c);
  }

  /**
   * Imported from {@link net.java.plaf.windows.xp.XPButtonUI}
   */
  protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
    int offset = b.isEnabled() ? 0 : 1;
    int h = b.getSize().height;
    int yb = textRect.y;
    int yt = textRect.y + textRect.height;
    if (h - yt - 2 == yb - 1)
      textRect.y++;
    textRect.x += offset;
    textRect.y += offset;
    super.paintText(g, b, textRect, text);
    textRect.x -= offset;
    textRect.y -= offset;
  }


  private void paintFrame(JComponent c, Graphics g, Color fill, Color highlight) {
    Insets insets = c.getInsets();
    g.setColor(fill);
/*
    Rectangle r = new Rectangle(insets.left, insets.top,
      c.getWidth() - insets.left - insets.right - 1, c.getHeight() - insets.top - insets.bottom - 1);
*/
    Rectangle r = new Rectangle(0, 0, c.getWidth() - 1, c.getHeight() - 1);
    g.fillRect(r.x, r.y, r.width, r.height);
    g.setColor(highlight);
    g.drawRect(r.x, r.y, r.width, r.height);
  }

  /**
   * @return null if button does not need to be highlighted
   */
  private Color getFillColor(Color highlight, ButtonModel model) {
    int grade = 0;
    if (model.isSelected())
      grade += 1;
    if (model.isArmed())
      grade += 1;
    if (model.isRollover())
      grade += 2;
    if (grade == 0)
      return null;
    return ColorUtil.adjustHSB(highlight, -1, HIGHLIGHT_SATURATIONS[grade - 1], HIGHLIGHT_BRIGHTNESSES[grade - 1]);
  }
}
