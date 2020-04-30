package com.almworks.util.components.renderer.table;

import com.almworks.util.components.renderer.FontStyle;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TextWithIconCell implements TableRendererCell {
  private final FontStyle myFontStyle;
  private final String myText;
  private final Icon myIcon;

  public TextWithIconCell(FontStyle fontStyle, String text, Icon icon) {
    myFontStyle = fontStyle;
    myText = text;
    myIcon = icon;
  }

  public int getHeight(RendererContext context) {
    int th = myFontStyle.getHeight(context);
    int ih = myIcon == null ? 0 : myIcon.getIconHeight();
    return Math.max(th, ih);
  }

  public int getWidth(RendererContext context) {
    int tw = myFontStyle.getStringWidth(context, myText);
    int iw = myIcon == null ? 0 : myIcon.getIconWidth() + 5;
    return tw + iw;
  }

  public void paint(Graphics g, int x, int y, RendererContext context) {
    if (myIcon != null) {
      int th = myFontStyle.getHeight(context);
      int ih = myIcon.getIconHeight();
      int yy = y + (Math.max(ih, th) - ih) / 2;
      myIcon.paintIcon(context.getComponent(), g, x, yy);
      x += myIcon.getIconWidth() + 5;
      y = y + (Math.max(ih, th) - th) / 2;
    }
    myFontStyle.paint(g, x, y, context, myText);
  }

  public void invalidateLayout(RendererContext context) {
  }

  @Nullable
  public RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle) {
    return null;
  }

  @Nullable
  public JComponent getNextLifeComponent(@NotNull RendererContext context, @Nullable JComponent current, @NotNull Rectangle targetArea, boolean next) {
    return null;
  }
}
