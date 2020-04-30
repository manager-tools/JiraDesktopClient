package com.almworks.util.components.renderer.table;

import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.renderer.Renderer;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.external.BitSet2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author dyoma
 */
public class TableRenderer implements Renderer {
  private static final TypedKey<Dimension> SIZE = TypedKey.create("size");
  private static final TypedKey<Integer> START2 = TypedKey.create("start2");
  private static final TypedKey<Integer> VALID_VERSION = TypedKey.create("validVersion");
  private static final TypedKey<BitSet2> VISIBLE_LINES = TypedKey.create("visibleLines");

  private final List<TableRendererLine> myLines = Collections15.arrayList();
  private final ComponentProperty<Integer> COMPONENT_LINE = ComponentProperty.createProperty("line");
  private final FireEventSupport<Listener> myListeners = FireEventSupport.create(Listener.class);

  private int myVerticalGap = 2;
  private int myLayoutVersion = 0;

  public Dimension getPreferredSize(RendererContext context) {
    ensureValid(context);
    return new Dimension(context.getCachedValue(SIZE));
  }

  private void layout(RendererContext context) {
    Dimension size = new Dimension();
    int maxWidth1 = 0;
    int maxWidth2 = 0;
    BitSet2 visibleLines = new BitSet2(myLines.size());
    for (int i = 0; i < myLines.size(); i++) {
      TableRendererLine line = myLines.get(i);
      if (line.isVisible(context)) {
        visibleLines.set(i);
        maxWidth1 = Math.max(maxWidth1, line.getWidth(0, context));
        maxWidth2 = Math.max(maxWidth2, line.getWidth(1, context));
      }
    }
    size.width = calcWholeWidth(maxWidth1, maxWidth2);
    for (int i = 0; i < myLines.size(); i++) {
      if (visibleLines.get(i)) {
        TableRendererLine line = myLines.get(i);
        size.height += line.getHeight(size.width, context);
      }
    }
    int lineCount = visibleLines.cardinality();
    if (lineCount > 1) {
      size.height += (lineCount - 1) * getVerticalGap();
    }
    context.cacheValue(SIZE, size);
    context.cacheValue(START2, maxWidth1 + getHorizontalGap());
    context.cacheValue(VALID_VERSION, myLayoutVersion);
    context.cacheValue(VISIBLE_LINES, visibleLines);
  }

  private int calcWholeWidth(int width1, int width2) {
    return width1 + width2 + (width1 > 0 && width2 > 0 ? getHorizontalGap() : 0);
  }

  public void paint(Graphics g, RendererContext context) {
    ensureValid(context);
    BitSet2 visibleLines = getVisibleLines(context);
    int y = 0;
    for (int i = 0; i < myLines.size(); i++) {
      if (visibleLines.get(i)) {
        TableRendererLine line = myLines.get(i);
        line.paint(g, y, context);
        y += line.getHeight(context.getWidth(), context) + myVerticalGap;
      }
    }
  }

  private BitSet2 getVisibleLines(RendererContext context) {
    BitSet2 visibleLines = context.getCachedValue(VISIBLE_LINES);
    if (visibleLines == null) {
      assert false : "no visible lines";
      visibleLines = new BitSet2(myLines.size());
      visibleLines.set(0, myLines.size());
    }
    return visibleLines;
  }

  private void ensureValid(RendererContext context) {
    Integer version = context.getCachedValue(VALID_VERSION);
    if (version != null && version == myLayoutVersion)
      return;
    invalidate(context);
    layout(context);
  }

  private void invalidate(RendererContext context) {
    context.cacheValue(SIZE, null);
    context.cacheValue(START2, null);
    context.cacheValue(VISIBLE_LINES, null);
    context.cacheValue(VALID_VERSION, null);
    for (TableRendererLine line : myLines) {
      line.invalidateLayout(context);
    }
  }

  public int getVerticalGap() {
    return myVerticalGap;
  }

  public int getColumnX(int index, RendererContext context) {
    if (index == 0)
      return 0;
    assert index == 1;
    Integer x = context.getCachedValue(START2);
    assert x != null;
    return x == null ? 0 : x;
  }

  private int getColumnEndX(int index, RendererContext context) {
    if (index == 0)
      return getColumnX(1, context) - getHorizontalGap();
    assert index == 1;
    Dimension size = context.getCachedValue(SIZE);
    assert size != null;
    return size.width;
  }

  public int getColumnWidth(int index, RendererContext context) {
    return getColumnEndX(index, context) - getColumnX(index, context);
  }

  public int getHorizontalGap() {
//    return UIUtil.GAP;
    return 20;
  }

  public TwoColumnLine addLine(String label, TableRendererCell valueCell) {
    TwoColumnLine line = TwoColumnLine.labeledCell(label, valueCell, this);
    addLine(line);
    return line;
  }

  public TableRendererLine addLine(TableRendererLine line) {
    myLines.add(line);
    myLayoutVersion++;
    return line;
  }

  public void addAWTListener(Lifespan life, Listener listener) {
    myListeners.addAWTListener(life, listener);
  }

  public boolean updateMouseActivity(int id, int x, int y, RendererContext context, RendererActivityController controller) {
    ensureValid(context);
    int column2X = getColumnX(1, context);
    if (x >= 0 && (x <= column2X - getHorizontalGap() || x >= column2X)) {
      Rectangle rectangle = new Rectangle();
      int lineIndex = findLineIndexAt(y, context, rectangle);
      if (lineIndex >= 0) {
        int columnIndex = x < column2X ? 0 : 1;
        int columnStart = getColumnX(columnIndex, context);
        int cellX = x - columnStart;
        assert cellX >= 0;
        int lineStart = rectangle.y;
        TableRendererLine line = myLines.get(lineIndex);
        if (cellX <= getColumnWidth(columnIndex, context)) {
          rectangle.y = 0;
          rectangle.x = columnStart;
          rectangle.width = getColumnWidth(columnIndex, context);
          RendererActivity activity = line.getActivityAt(id, columnIndex, cellX, y - lineStart, context, rectangle);
          if (activity != null) {
            rectangle.y += lineStart;
            activity.apply(controller, rectangle);
            activity.storeValue(COMPONENT_LINE, lineIndex);
            return true;
          }
        }
      }
    }
    if (id == MouseEvent.MOUSE_CLICKED) {
      controller.removeLiveComponent(true);
    }
    return false;
  }

  public void processMouseEvent(int id, int x, int y, RendererContext context, RendererActivityController controller) {
    ensureValid(context);
    Rectangle rect = new Rectangle();
    int lineIndex = findLineIndexAt(y, context, rect);
    if (lineIndex < 0) {
      myListeners.getDispatcher().onMouseOverRectangle(null);
      return;
    }
    notifyMouseInRectangle(id, x, y, context, rect.y, myLines.get(lineIndex));
  }

  private void notifyMouseInRectangle(int id, int x, int y, RendererContext context, int lineStart, TableRendererLine line) {
    if (id == MouseEvent.MOUSE_EXITED) {
      myListeners.getDispatcher().onMouseOverRectangle(null);
      return;
    }
    if (id != MouseEvent.MOUSE_MOVED && id != MouseEvent.MOUSE_ENTERED)
      return;
    Rectangle lineRectangle = new Rectangle();
    lineRectangle.x = 0;
    lineRectangle.y = lineStart;
    lineRectangle.width = context.getWidth();
    lineRectangle.height = line.getHeight(lineRectangle.width, context);
    if (!line.getLineRectangle(y - lineStart, lineRectangle, context))
      lineRectangle = null;
    else if (lineRectangle.width < x)
      lineRectangle = null;
    myListeners.getDispatcher().onMouseOverRectangle(lineRectangle);
  }

  @Nullable
  public JComponent getNextLiveComponent(@NotNull RendererContext context, @Nullable JComponent current,
    @NotNull Rectangle targetArea, boolean next)
  {
    ensureValid(context);
    Integer firstLine;
    if (current != null) {
      firstLine = COMPONENT_LINE.getClientValue(current);
    } else {
      firstLine = -1;
    }
    if (firstLine == null) {
      assert false : current;
      firstLine = -1;
    }
    BitSet2 visibleLines = getVisibleLines(context);
    int linesCount = myLines.size();
    if (firstLine == -1) {
      firstLine = next ? visibleLines.nextSetBit(0) : visibleLines.prevSetBit(linesCount - 1);
      if (firstLine == -1) {
        assert false : "no visible lines";
        firstLine = 0;
      }
      current = null;
    }
    targetArea.y = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = firstLine; next ? (i < linesCount) : (i >= 0); i += next ? 1 : -1) {
      if (visibleLines.get(i)) {
        TableRendererLine line = myLines.get(i);
        JComponent result = line.getNextLiveComponent(context, this, current, targetArea, next);
        if (result != null) {
          COMPONENT_LINE.putClientValue(result, i);
          targetArea.y += getLineTop(context, i);
          return result;
        }
        current = null;
      }
    }
    return null;
  }

  private int getLineTop(RendererContext context, int index) {
    return getLineTop(myLines, index, context, getVerticalGap(), getVisibleLines(context));
  }

  public static int getLineTop(List<TableRendererLine> lines, int index, RendererContext context, int verticalGap,
    @Nullable BitSet2 visibleLines)
  {
    assert index >= 0 && index < lines.size() : index;
    int result = 0;
    for (int i = 0; i < index; i++) {
      if (visibleLines == null || visibleLines.get(i)) {
        result += lines.get(i).getHeight(context.getWidth(), context) + verticalGap;
      }
    }
    return result;
  }

  public boolean isFocusable() {
    return true;
  }

  private int findLineIndexAt(int y, RendererContext context, Rectangle rectangle) {
    BitSet2 visibleLines = getVisibleLines(context);
    rectangle.y = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myLines.size(); i++) {
      if (y < 0)
        return -1;
      if (!visibleLines.get(i))
        continue;
      TableRendererLine line = myLines.get(i);
      int height = line.getHeight(context.getWidth(), context);
      if (y < height)
        return i;
      y -= height + getVerticalGap();
      rectangle.y += height + getVerticalGap();
    }
    return -1;
  }

  public void clear() {
    myLines.clear();
    myLayoutVersion++;
  }

  /**
   * @return -1 if no width-driven, otherwise returns prefered width
   */
  public int getPreferedWidth(RendererContext context) {
    int maxWidth1 = -1;
    int maxWidth2 = -1;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myLines.size(); i++) {
      TableRendererLine line = myLines.get(i);
      if (line.isVisible(context)) {
        maxWidth1 = Math.max(maxWidth1, line.getPreferedWidth(0, context));
        maxWidth2 = Math.max(maxWidth2, line.getPreferedWidth(1, context));
      }
    }
    return calcWholeWidth(maxWidth1, maxWidth2);
  }

  public int getPreferedHeight(int width, RendererContext context) {
    assert false : "not implemented";
    return 0;
  }
}