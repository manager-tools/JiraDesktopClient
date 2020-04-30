package com.almworks.util.components.renderer.table;

import com.almworks.util.commons.Function;
import com.almworks.util.components.renderer.FontStyle;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.text.LineTokenizer;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class MultiLineTextCell implements TableRendererCell {
  private final FontStyle myFontStyle;
  private final Function<RendererContext, String> myTextGetter;

  public MultiLineTextCell(FontStyle fontStyle, Function<RendererContext, String> textGetter) {
    myFontStyle = fontStyle;
    myTextGetter = textGetter;
  }

  public int getHeight(RendererContext context) {
    return myFontStyle.getHeight(context) * getLines(context).size();
  }

  public int getWidth(RendererContext context) {
    int max = 0;
    for (String line : getLines(context))
      max = Math.max(max, myFontStyle.getStringWidth(context, line));
    return max;
  }

  public void paint(Graphics g, int x, int y, RendererContext context) {
    int height = myFontStyle.getHeight(context);
    for (String line : getLines(context)) {
      myFontStyle.paint(g, x, y, context, line);
      y += height;
    }
  }

  public void invalidateLayout(RendererContext context) {
  }

  @Nullable
  public RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle) {
    return null;
  }

  @Nullable
  public JComponent getNextLifeComponent(@NotNull RendererContext context, @Nullable JComponent current,
    @NotNull Rectangle targetArea, boolean next)
  {
    return null;
  }

  private java.util.List<String> getLines(RendererContext context) {
    String text = myTextGetter.invoke(context);
    return text != null ? LineTokenizer.getLines(text) : Collections15.<String>emptyList();
  }
}
