package com.almworks.util.components.plaf.macosx.combobox;

import com.almworks.util.Env;
import com.almworks.util.components.plaf.macosx.Aqua;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

public class MacComboBoxFocusRing {
  public static final boolean USE_QUARTZ = "true".equals(System.getProperty("apple.awt.graphics.UseQuartz"));
  public static final int MAC_COMBO_BORDER_V_OFFSET = Env.isMacLionOrNewer() ? 1 : 0;
  public static final int MAC_COMBO_BORDER_W_OFFSET = Env.isMacLionOrNewer() ? 1 : 0;

  public static void paintComboboxFocusRing(@NotNull final JComboBox combobox, @NotNull final Graphics g, @Nullable Insets insets) {
    assert Aqua.isAqua();
    if (combobox.isEnabled() && combobox.isEditable()) {
      final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      if (focusOwner != null) {
        final Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, focusOwner);
        if (ancestor == combobox) {
          paintComboboxFocusRing((Graphics2D)g, combobox.getBounds(), insets);
        }
      }
    }
  }

  private static void paintComboboxFocusRing(@NotNull final Graphics2D g2d, @NotNull final Rectangle bounds, @Nullable Insets insets) {
    final Color color = getFocusRingColor();
    final Color[] colors = new Color[] {
      new Color(color.getRed(), color.getGreen(), color.getBlue(), 180),
      new Color(color.getRed(), color.getGreen(), color.getBlue(), 130),
      new Color(color.getRed(), color.getGreen(), color.getBlue(), 80)
    };

    final Object oldAntialiasingValue = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    final Object oldStrokeControlValue = g2d.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
      USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE);

    int _y = MAC_COMBO_BORDER_V_OFFSET;
    int width = bounds.width + MAC_COMBO_BORDER_W_OFFSET;
    int height = bounds.height;
    if (insets != null) {
      width -= (insets.left + insets.right);
      height -= (insets.top + insets.bottom);
      g2d.translate(insets.left, insets.top);
    }

    final GeneralPath path1 = new GeneralPath();
    path1.moveTo(2, _y + 4);
    path1.quadTo(2, _y + 2, 4, _y + 2);
    path1.lineTo(width - 7, _y + 2);
    path1.quadTo(width - 5, _y + 3, width - 4, _y + 5);
    path1.lineTo(width - 4, height - 7 + _y);
    path1.quadTo(width - 5, height - 5 + _y, width - 7, height - 4 + _y);
    path1.lineTo(4, height - 4 + _y);
    path1.quadTo(2, height - 4 + _y, 2, height - 6 + _y);
    path1.closePath();

    g2d.setColor(colors[0]);
    g2d.draw(path1);

    final GeneralPath path2 = new GeneralPath();
    path2.moveTo(1, 5 + _y);
    path2.quadTo(1, 1 + _y, 5, 1 + _y);
    path2.lineTo(width - 8, 1 + _y);
    path2.quadTo(width - 4, 2 + _y, width - 3, 6 + _y);
    path2.lineTo(width - 3, height - 7 + _y);
    path2.quadTo(width - 4, height - 4 + _y, width - 8, height - 3 + _y);
    path2.lineTo(4, height - 3 + _y);
    path2.quadTo(1, height - 3 + _y, 1, height - 6 + _y);
    path2.closePath();

    g2d.setColor(colors[1]);
    g2d.draw(path2);

    final GeneralPath path3 = new GeneralPath();
    path3.moveTo(0, 4 + _y);
    path3.quadTo(0, _y, 7, _y);
    path3.lineTo(width - 9, _y);
    path3.quadTo(width - 2, 1 + _y, width - 2, 7 + _y);
    path3.lineTo(width - 2, height - 8 + _y);
    path3.quadTo(width - 3, height - 1 + _y, width - 12, height - 2 + _y);
    path3.lineTo(7, height - 2 + _y);
    path3.quadTo(0, height - 1 + _y, 0, height - 7 + _y);
    path3.closePath();

    g2d.setColor(colors[2]);
    g2d.draw(path3);

    // restore rendering hints
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasingValue);
    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStrokeControlValue);
  }

  private static Color getFocusRingColor() {
    final Object o = UIManager.get("Focus.color");
    if (o instanceof Color) {
      return (Color)o;
    }
    return new Color(64, 113, 167);
  }
}
