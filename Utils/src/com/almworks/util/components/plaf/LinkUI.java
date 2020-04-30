package com.almworks.util.components.plaf;

import com.almworks.util.NoObfuscation;
import com.almworks.util.components.Link;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class LinkUI extends BasicButtonUI implements NoObfuscation {
  private static final LinkUI INSTANCE = new LinkUI();

  public static ComponentUI createUI(JComponent c) {
    return INSTANCE;
  }

  public void installUI(JComponent c) {
    super.installUI(c);
    Link link = (Link) c;
    LookAndFeel.installColorsAndFont(c, "Link.background", "Link.foreground", "Link.font");

    Color color = link.getPressedColor();
    if (color == null || color instanceof UIResource)
      link.setPressedColor(UIManager.getColor("Link.pressed"));

    color = link.getHoverColor();
    if (color == null || color instanceof UIResource)
      link.setHoverColor(UIManager.getColor("Link.hover"));

    if (!link.isUnderlinedSet())
      link.setUnderlined(UIManager.getBoolean("Link.underlined"));
  }

  protected void installDefaults(AbstractButton b) {
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    b.setRolloverEnabled(true);
    b.setFocusable(false);
  }

  protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
    int width = b.getWidth();
    int height = b.getHeight();
    g.setColor(UIManager.getColor("Button.focus")); // todo
    BasicGraphicsUtils.drawDashedRect(g, 0, 0, width - 0, height - 0); // todo insets?
  }

  protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
    Link link = (Link) b;
    ButtonModel model = link.getModel();
    if (model.isEnabled()) {
      boolean underline = model.isRollover() && link.isUnderlined();
      Color color;
      if (model.isArmed()) {
        color = link.getPressedColor();
      } else {
        if (model.isRollover()) {
          color = link.getHoverColor();
        } else {
          color = b.getForeground();
        }
      }
      g.setColor(color);
      FontMetrics fm = g.getFontMetrics();
      int x = textRect.x + getTextShiftOffset();
      int y = textRect.y + fm.getAscent() + getTextShiftOffset();
      BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, -1, x, y);
      if (underline)
        g.drawLine(x, y + 1, x + fm.stringWidth(text), y + 1);
    } else {
      PaintingPolicy disabledLook = link.getDisabledLook();
      if (disabledLook != null)
        disabledLook.paint(g, link, textRect, text);
      else
        super.paintText(g, b, textRect, text);
    }
  }

  public interface PaintingPolicy {
    void paint(Graphics g, Link b, Rectangle textRect, String text);
  }


  public static class NormalPaint implements PaintingPolicy {
    private final String myColorKey;
    private final Color myColor;

    public NormalPaint(String colorKey) {
      myColorKey = colorKey;
      myColor = null;
    }

    public NormalPaint(Color color) {
      myColor = color;
      myColorKey = null;
    }

    public static PaintingPolicy createDefault() {
      return new NormalPaint("Label.foreground");
    }

    public void paint(Graphics g, Link b, Rectangle textRect, String text) {
      assert (myColor != null) ^ (myColorKey != null) : myColor + " " + myColorKey;
      Color color = myColor;
      if (color == null)
        color = UIManager.getDefaults().getColor(myColorKey);
      g.setColor(color);
      LinkUI ui = (LinkUI) b.getUI();
      FontMetrics fm = g.getFontMetrics();
      int x = textRect.x + ui.getTextShiftOffset();
      int y = textRect.y + fm.getAscent() + ui.getTextShiftOffset();
      BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, -1, x, y);
    }
  }
}
