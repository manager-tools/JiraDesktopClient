package com.almworks.util.ui.actions.dnd;

import com.almworks.util.Pair;
import com.almworks.util.commons.Factory;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class StringDragImageFactory implements Factory<Image> {
  private final String myString;
  private final Component myComponent;
  private final Pair<Color, Color> myFgBg;

  public StringDragImageFactory(String string, Component component, Pair<Color, Color> fgbg) {
    myString = string;
    myComponent = component;
    myFgBg = fgbg;
  }

  public Image create() {
    String[] lines = myString.split("\\n");
    if (lines.length == 0)
      return null;
    Font font = myComponent.getFont();
    Pair<FontMetrics, Integer> metrics = getMetrics(font, lines);
    int width = metrics.getSecond() + 4;
    FontMetrics fontMetrics = metrics.getFirst();
    int fontHeight = fontMetrics.getHeight();
    int height = fontHeight * lines.length - fontMetrics.getLeading();

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
    Graphics2D g = (Graphics2D) image.getGraphics();
    try {
      Color fg = null;
      Color bg = null;
      if (myFgBg != null) {
        fg = myFgBg.getFirst();
        bg = myFgBg.getSecond();
      }
      if (fg == null) fg = UIManager.getColor("List.selectionForeground");
      if (bg == null) bg = UIManager.getColor("List.selectionBackground");
      int bgt = (bg.getRGB() & 0xFFFFFF) | (((int) (255 * DndUtil.DRAG_IMAGE_OPACITY)) << 24);
      g.setColor(new Color(bgt, true));
      g.fillRect(0, 0, width, height);

      g.setColor(fg);
      setupText(g, font);

      g.setComposite(AlphaComposite.SrcIn);
      int y = fontMetrics.getAscent();
      for (String line : lines) {
        g.drawString(line, 2, y);
        y += fontHeight;
      }
    } finally {
      g.dispose();
    }
    return image;
  }

  private Pair<FontMetrics, Integer> getMetrics(Font font, String[] lines) {
    Graphics2D g = (Graphics2D) DndUtil.DRAG_IMAGE_KICKOFF.getGraphics();
    try {
      setupText(g, font);
      FontMetrics fontMetrics = g.getFontMetrics(font);
      int maxw = 0;
      for (String line : lines) {
        Rectangle2D stringBounds = fontMetrics.getStringBounds(line, g);
        maxw = Math.max(maxw, (int) Math.ceil(stringBounds.getWidth()));
      }
      return Pair.create(fontMetrics, maxw);
    } finally {
      g.dispose();
    }
  }

  private void setupText(Graphics2D g, Font font) {
    AwtUtil.applyRenderingHints(g);
    g.setFont(font);
  }

  public static void ensureContext(DragContext context, TypedKey<Factory<Image>> key, String string,
    Component component, Pair<Color,Color> fgBg)
  {
    Factory<Image> f = context.getValue(key);
    if (f instanceof StringDragImageFactory) {
      StringDragImageFactory factory = (StringDragImageFactory) f;
      if (Util.equals(factory.myFgBg, fgBg) && Util.equals(factory.myString, string) &&
        Util.equals(factory.myComponent, component))
      {
        return;
      }
    }
    context.putValue(key, new StringDragImageFactory(string, component, fgBg));
  }


  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof StringDragImageFactory))
      return false;

    StringDragImageFactory factory = (StringDragImageFactory) o;

    if (myFgBg != null ? !myFgBg.equals(factory.myFgBg) : factory.myFgBg != null)
      return false;
    if (myComponent != null ? !myComponent.equals(factory.myComponent) : factory.myComponent != null)
      return false;
    if (myString != null ? !myString.equals(factory.myString) : factory.myString != null)
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myString != null ? myString.hashCode() : 0);
    result = 31 * result + (myComponent != null ? myComponent.hashCode() : 0);
    result = 31 * result + (myFgBg != null ? myFgBg.hashCode() : 0);
    return result;
  }
}
