package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.util.SingleChildWidget;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class FocusBorderWidget<T> extends SingleChildWidget<T> {
  private static final Color NOT_FOCUCED_COLOR = new Color(64, 0, 0);

  @Override
  public int getPreferedWidth(@NotNull CellContext context, T value) {
    return super.getPreferedWidth(context, value) + 2;
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, T value) {
    return super.getPreferedHeight(context, width - 2, value) + 2;
  }

  @Override
  protected void layout(LayoutContext context, T value) {
    int width = context.getWidth() - 2;
    int height = context.getHeight() - 2;
    if (width < 0 || height < 0) return;
    setChildBounds(context, 1, 1, width, height);
  }

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
    if (reason == EventContext.DESCENDANT_GAINED_FOCUS || reason == EventContext.DESCENDANT_LOST_FOCUS) context.repaint();
    super.processEvent(context, value, reason);
  }

  @Override
  public void paint(@NotNull GraphContext context, T value) {
    HostCell child = context.findChild(0);
    if (child != null && child.isFocused())context.setColor(Color.RED);
    else if (child != null) context.setColor(NOT_FOCUCED_COLOR);
    else context.setColor(Color.BLACK);
    context.drawRect(0, 0, context.getWidth(), context.getHeight());
  }

  public static <T> Widget<T> wrap(Widget<T> widget) {
    FocusBorderWidget<T> border = new FocusBorderWidget<T>();
    border.setChildWidget(widget);
    return border;
  }
}
