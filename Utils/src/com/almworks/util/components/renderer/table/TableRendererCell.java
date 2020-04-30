package com.almworks.util.components.renderer.table;

import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public interface TableRendererCell {
  int getHeight(RendererContext context);

  int getWidth(RendererContext context);

  void paint(Graphics g, int x, int y, RendererContext context);

  void invalidateLayout(RendererContext context);

  @Nullable
  RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle);

  @Nullable
  JComponent getNextLifeComponent(@NotNull RendererContext context, @Nullable JComponent current,
    @NotNull Rectangle targetArea, boolean next);

  abstract class Decorator implements TableRendererCell {
    private final TableRendererCell myDecorated;

    public Decorator(TableRendererCell decorated) {
      myDecorated = decorated;
    }

    protected abstract int getAdditionalHeight(RendererContext context, int decoratedHeight);

    protected abstract int getAdditionalWidth(RendererContext context, int decoratedWidth);

    protected abstract int translateX(int x, int y, RendererContext context);

    protected abstract int translateY(int x, int y, RendererContext context);

    public int getHeight(RendererContext context) {
      int decoratedHeight = myDecorated.getHeight(context);
      return decoratedHeight + getAdditionalHeight(context, decoratedHeight);
    }

    public int getWidth(RendererContext context) {
      int decoratedWidth = myDecorated.getWidth(context);
      return decoratedWidth + getAdditionalWidth(context, decoratedWidth);
    }

    public void paint(Graphics g, int x, int y, RendererContext context) {
      myDecorated.paint(g, translateX(x, y, context), translateY(x, y, context), context);
    }

    public void invalidateLayout(RendererContext context) {
      myDecorated.invalidateLayout(context);
    }

    @Nullable
    public RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle) {
      int translatedX = translateX(x, y, context);
      int translatedY = translateY(x, y, context);
      RendererActivity activity = myDecorated.getActivityAt(id, translatedX, translatedY, context, rectangle);
      if (activity != null)
        translateRectangle(context, rectangle);
      return activity;
    }

    private void translateRectangle(RendererContext context, Rectangle rectangle) {
      int rectX = rectangle.x;
      int rectY = rectangle.y;
      rectangle.x = translateX(rectX, rectY, context);
      rectangle.y = translateY(rectX, rectY, context);
      rectangle.width -= getAdditionalWidth(context, rectangle.width);
      rectangle.height -= getAdditionalHeight(context, rectangle.height);
    }

    @Nullable
    public JComponent getNextLifeComponent(@NotNull RendererContext context, @Nullable JComponent current,
      @NotNull Rectangle targetArea, boolean next)
    {
      Rectangle rect = new Rectangle(targetArea);
      JComponent component = myDecorated.getNextLifeComponent(context, current, targetArea, next);
      if (component != null) {
        translateRectangle(context, rect);
        targetArea.setBounds(rect);
      }
      return component;
    }
  }
}
