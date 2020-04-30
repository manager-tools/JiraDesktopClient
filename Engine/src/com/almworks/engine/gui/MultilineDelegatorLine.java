package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.field.ItemField;
import com.almworks.util.commons.Function2;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererLine;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public abstract class MultilineDelegatorLine implements TableRendererLine {
  private final TypedKey<List<TableRendererLine>> LINES = TypedKey.create("lines");
  private final ComponentProperty<Integer> LINE = ComponentProperty.createProperty("cfLine");
  protected final ModelKey<List<ModelKey<?>>> myKey;
  protected Function2<ModelKey, ModelMap, ItemField> myFieldGetter;
  protected final TableRenderer myRenderer;

  public MultilineDelegatorLine(TableRenderer renderer, ModelKey<List<ModelKey<?>>> key,
    Function2<ModelKey, ModelMap, ItemField> fieldGetter)
  {
    myRenderer = renderer;
    myKey = key;
    myFieldGetter = fieldGetter;
  }

  public boolean isVisible(RendererContext context) {
    return true;
  }

  public int getHeight(int width, RendererContext context) {
    List<TableRendererLine> lines = getLines(context);
    int height = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < lines.size(); i++) {
      TableRendererLine line = lines.get(i);
      height += line.getHeight(width, context);
    }
    if (!lines.isEmpty())
      height += myRenderer.getVerticalGap() * (lines.size() - 1);
    return height;
  }

  public boolean getLineRectangle(int y, Rectangle lineRectangle, RendererContext context) {
    List<TableRendererLine> lines = getLines(context);
    int yOffset = 0;
    for (TableRendererLine line : lines) {
      int lineHeight = line.getHeight(lineRectangle.width, context);
      if (y < lineHeight) {
        lineRectangle.y += yOffset;
        lineRectangle.height = lineHeight;
        return line.getLineRectangle(y, lineRectangle, context);
      }
      int yDelta = lineHeight + myRenderer.getVerticalGap();
      y -= yDelta;
      yOffset += yDelta;
      if (y < 0)
        return false;
    }
    return false;
  }

  public int getWidth(int columnIndex, RendererContext context) {
    List<TableRendererLine> lines = getLines(context);
    int width = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < lines.size(); i++) {
      TableRendererLine line = lines.get(i);
      width = Math.max(width, line.getWidth(columnIndex, context));
    }
    return width;
  }

  public void paint(Graphics g, int y, RendererContext context) {
    List<TableRendererLine> lines = getLines(context);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < lines.size(); i++) {
      TableRendererLine line = lines.get(i);
      line.paint(g, y, context);
      y += line.getHeight(context.getWidth(), context) + myRenderer.getVerticalGap();
    }
  }

  public void invalidateLayout(RendererContext context) {
  }

  @Nullable
  public RendererActivity getActivityAt(int id, int columnIndex, int x, int y, RendererContext context,
    Rectangle rectangle)
  {
    List<TableRendererLine> lines = getLines(context);
    int yOffset = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < lines.size(); i++) {
      if (y < 0)
        return null;
      TableRendererLine line = lines.get(i);
      int height = line.getHeight(context.getWidth(), context);
      if (y < height) {
        RendererActivity activity = line.getActivityAt(id, columnIndex, x, y, context, rectangle);
        if (activity != null) {
          rectangle.y += yOffset;
          activity.storeValue(LINE, i);
        }
        return activity;
      }
      y -= height + myRenderer.getVerticalGap();
      yOffset += height + myRenderer.getVerticalGap();
    }
    return null;
  }

  @Nullable
  public JComponent getNextLiveComponent(@NotNull RendererContext context, TableRenderer tableRenderer,
    @Nullable JComponent current, @NotNull Rectangle targetArea, boolean next)
  {
    Integer firstLine;
    if (current != null)
      firstLine = LINE.getClientValue(current);
    else
      firstLine = -1;
    if (firstLine == null) {
      assert false : current;
      firstLine = -1;
    }
    List<TableRendererLine> lines = getLines(context);
    if (firstLine == -1) {
      firstLine = next ? 0 : lines.size() - 1;
      current = null;
    }
    for (int i = firstLine; next ? (i < lines.size()) : i >= 0; i += next ? 1 : -1) {
      TableRendererLine line = lines.get(i);
      JComponent result = line.getNextLiveComponent(context, myRenderer, current, targetArea, next);
      if (result != null) {
        targetArea.y += TableRenderer.getLineTop(lines, i, context, myRenderer.getVerticalGap(), null);
        LINE.putClientValue(result, i);
        return result;
      }
      current = null;
    }
    return null;
  }

  public int getPreferedWidth(int column, RendererContext context) {
    return -1;
  }

  protected abstract List<TableRendererLine> createLines(RendererContext context);

  private List<TableRendererLine> getLines(RendererContext context) {
    List<TableRendererLine> lines = context.getCachedValue(LINES);
    if (lines == null) {
      lines = createLines(context);
      context.cacheValue(LINES, lines);
    }
    return lines;
  }
}
