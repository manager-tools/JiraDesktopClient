package com.almworks.engine.gui;

import com.almworks.util.components.AToolbarButton;
import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRendererCell;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

public class ActionCellDecorator extends TableRendererCell.Decorator {
  public static final int ICON_GAP = 2;
  private final TypedKey<Integer> myIconWidth = TypedKey.create("iconsWidth");
  private final ActionButtonActivity[] myActivities;

  public ActionCellDecorator(TableRendererCell decorated, java.util.List<CellAction> actions) {
    super(decorated);
    myActivities = new ActionButtonActivity[actions.size()];
    for (int i = 0; i < actions.size(); i++) {
      CellAction action = actions.get(i);
      String tooltip = action.getTooltip();
      TypedKey<AToolbarButton> key = TypedKey.create("Button #" + i + " " + tooltip);
      myActivities[i] = new ActionButtonActivity(key, action, tooltip, action.getIcon());
    }
  }

  protected int getAdditionalHeight(RendererContext context, int decoratedHeight) {
    int height = 0;
    for (ActionButtonActivity activity : myActivities) {
      height = Math.max(height, activity.getIconHeight());
    }
    return Math.max(height - decoratedHeight, 0);
  }

  protected int getAdditionalWidth(RendererContext context, int decoratedWidth) {
    return getIconWidth(context);
  }

  private int getIconWidth(RendererContext context) {
    Integer calculated = context.getValue(myIconWidth);
    if (calculated != null)
      return calculated;
    int width = 0;
    for (ActionButtonActivity activity : myActivities)
      width += activity.getIconWidth() + ICON_GAP;
    context.putValue(myIconWidth, width);
    return width;
  }

  protected int translateX(int x, int y, RendererContext context) {
    return x + getIconWidth(context);
  }

  protected int translateY(int x, int y, RendererContext context) {
    return y;
  }

  public void paint(Graphics g, int x, int y, RendererContext context) {
    super.paint(g, x, y, context);
    int height = getHeight(context);
    RendererActivityController controller = context.getController();
    for (int i = 0; i < myActivities.length; i++) {
      ActionButtonActivity activity = myActivities[i];
      if (activity.hasComponent(context))
        continue;
      int iconHeight = activity.getIconHeight();
      int yOffset = y + (height - iconHeight) / 2;
      if (iconHeight < height)
        yOffset++;
      activity.paintIcon(context.getComponent(), g, x, yOffset);
      y += activity.getIconWidth() + ICON_GAP;
    }
  }

  public void invalidateLayout(RendererContext context) {
    super.invalidateLayout(context);
    for (ActionButtonActivity activity : myActivities)
      activity.removeComponent(context);
  }

  @Nullable
  public RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle) {
    if (x >= getIconWidth(context))
      return super.getActivityAt(id, x, y, context, rectangle);
    if (id == MouseEvent.MOUSE_CLICKED || id == MouseEvent.MOUSE_MOVED) {
      int index = findActionIndexAt(x, rectangle);
      if (index < 0)
        return null;
      ActionButtonActivity action = myActivities[index];
      if (id == MouseEvent.MOUSE_CLICKED)
        return action.getPerform();
      else if (id == MouseEvent.MOUSE_MOVED) {
        ActionButtonActivity activity = myActivities[index];
        return activity.hasComponent(context) ? null : activity;
      }
    }
    return null;
  }

  private int findActionIndexAt(int x, Rectangle rectangle) {
    int i = 0;
    int actionStart = 0;
    while (i < myActivities.length) {
      ActionButtonActivity action = myActivities[i];
      int actionEnd = actionStart + action.getIconWidth();
      if (actionStart <= x && x <= actionEnd) {
        rectangle.x += actionStart;
        rectangle.width = actionEnd - actionStart;
        return i;
      }
      i++;
    }
    return -1;
  }
}
