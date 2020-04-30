package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.regex.Pattern;

class GridHeader<T> extends JComponent implements Scrollable, MouseListener, MouseMotionListener {
  private static final Dimension MINUMUM_SIZE = new Dimension(4, 4);
  private static final int MARGIN = 2;

  private JTable myTable;
  private final AListModel<T> myModel;
  private final BasicScalarModel<CollectionRenderer<T>> myRenderer;
  private final boolean myVertical;

  private Dimension myLastPreferredSize = null;
  private CellRendererPane myRendererPane = new CellRendererPane();

  private MouseEvent myLastMouseEvent = null;
  private int myLastMouseIndex = -1;
  private String myTooltip;

  private int myMaxAcrossSize = Integer.MAX_VALUE;
  private boolean myPaintGrid = true;

  public GridHeader(AListModel<T> model, BasicScalarModel<CollectionRenderer<T>> renderer, boolean vertical) {
    myModel = model;
    myRenderer = renderer;
    myVertical = vertical;
    model.addListener(new AListModel.Adapter() {
      public void onChange() {
        myLastPreferredSize = null;
        invalidate();
        repaint();
      }
    });
    renderer.getEventSource().addAWTListener(Lifespan.FOREVER, new ScalarModel.Adapter<CollectionRenderer<T>>() {
      public void onScalarChanged(ScalarModelEvent<CollectionRenderer<T>> event) {
        repaint();
      }
    });
    add(myRendererPane);
    addMouseListener(this);
    addMouseMotionListener(this);
    ImmediateTooltips.installImmediateTooltipManager(Lifespan.FOREVER, this, TooltipLocationProvider.UNDER_MOUSE);
  }

  public void setMaxAcrossSize(int maxAcrossSize) {
    myMaxAcrossSize = maxAcrossSize;
  }

  public Dimension getPreferredScrollableViewportSize() {
    Dimension tableSize = myTable.getPreferredScrollableViewportSize();
    Dimension prefSize = getPreferredSize();
    return myVertical ? new Dimension(prefSize.width, tableSize.height) :
      new Dimension(tableSize.width, prefSize.height);
  }

  public Dimension getPreferredSize() {
    if (myLastPreferredSize != null)
      return myLastPreferredSize;
    CollectionRenderer<T> renderer = myRenderer.getValue();
    if (renderer == null)
      return MINUMUM_SIZE;
    int size = myModel.getSize();
    if (size == 0)
      return MINUMUM_SIZE;
    int maxAcross = 4;
    int totalAlong = 0;
    for (int i = 0; i < size; i++) {
      T element = myModel.getAt(i);
      JComponent c = renderer.getRendererComponent(createState(i), element);
      Dimension preferredSize = c.getPreferredSize();
      maxAcross = Math.max(maxAcross, myVertical ? preferredSize.width : preferredSize.height);
      totalAlong += myVertical ? preferredSize.height : preferredSize.width;
    }
    maxAcross = Math.min(maxAcross, myMaxAcrossSize);
    Dimension tableSize = myTable.getPreferredSize();
    Dimension spacing = myTable.getIntercellSpacing();
    int margin = myPaintGrid ? 2 * MARGIN + (myVertical ? spacing.width : spacing.height) : 0;
    if (myVertical)
      myLastPreferredSize = new Dimension(maxAcross + margin, Math.max(totalAlong, tableSize.height));
    else
      myLastPreferredSize = new Dimension(Math.max(totalAlong, tableSize.width), maxAcross + margin);
    return myLastPreferredSize;
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    Color bg = getBackground();
    Dimension size = getSize();
    g.setColor(bg);
    g.fillRect(0, 0, size.width, size.height);
    int count = myModel.getSize();
    if (count == 0)
      return;
    CollectionRenderer<T> renderer = myRenderer.getValue();
    if (renderer == null)
      return;
    Rectangle clip = g.getClipBounds();
    Dimension spacing = myTable.getIntercellSpacing();
    Color gridColor = myTable.getGridColor();
    Cursor cursor = null;
    String tooltip = null;
    for (int i = 0; i < count; i++) {
      Rectangle r;
      if (myVertical) {
        r = myTable.getCellRect(i, -1, false);
        r.x = 0;
        r.width = size.width - (myPaintGrid ? spacing.width : 0);
      } else {
        r = myTable.getCellRect(-1, i, false);
        r.y = 0;
        r.height = size.height - (myPaintGrid ? spacing.height : 0);
      }
      if (r.intersects(clip)) {
        T element = myModel.getAt(i);
        GridHeaderCellState state = createState(i);
        JComponent c = renderer.getRendererComponent(state, element);
        myRendererPane.paintComponent(g, c, this, r);

        if (myPaintGrid) {
          g.setColor(gridColor);
          if (myVertical) {
            g.fillRect(0, r.y + r.height - 1, size.width, spacing.height);
            g.fillRect(r.x + r.width, r.y, spacing.width, r.height + spacing.height);
          } else {
            g.fillRect(r.x + r.width - 1, 0, spacing.width, size.height);
            g.fillRect(r.x, r.y + r.height, r.width + spacing.width, spacing.height);
          }
        }

        if (myLastMouseIndex == i) {
          if (state.myCursor != null) {
            cursor = state.myCursor;
          }
          if (state.myTooltip != null) {
            tooltip = state.myTooltip;
          }
        }
      }
    }
    myRendererPane.removeAll();
    myTooltip = tooltip;
    ImmediateTooltips.tooltipChanged(this);
    setCursor(cursor == null ? Cursor.getDefaultCursor() : cursor);
  }

  public String getToolTipText() {
    return myTooltip;
  }

  private GridHeaderCellState createState(int i) {
    return new GridHeaderCellState(i, myTable, myVertical, myLastMouseEvent);
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return myTable.getScrollableBlockIncrement(visibleRect, orientation, direction);
  }

  public boolean getScrollableTracksViewportHeight() {
    return myTable.getScrollableTracksViewportHeight();
  }

  public boolean getScrollableTracksViewportWidth() {
    return myTable.getScrollableTracksViewportWidth();
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return myTable.getScrollableUnitIncrement(visibleRect, orientation, direction);
  }

  public void setTable(JTable table) {
    myTable = table;
    setOpaque(true);
    setBackground(myTable.getBackground());
    setForeground(myTable.getForeground());
    setFont(myTable.getFont());
  }

  public void mouseClicked(MouseEvent e) {
    onMouse(e);
  }

  public void mouseEntered(MouseEvent e) {
    onMouse(e);
  }

  public void mouseExited(MouseEvent e) {
    onMouse(e);
  }

  public void mousePressed(MouseEvent e) {
    onMouse(e);
  }

  public void mouseReleased(MouseEvent e) {
    onMouse(e);
  }

  public void mouseDragged(MouseEvent e) {
    onMouse(e);
  }

  public void mouseMoved(MouseEvent e) {
    onMouse(e);
  }

  private void onMouse(MouseEvent e) {
    if (myTable == null)
      return;
    int index = -1;
    myLastMouseEvent = e;
    if (myLastMouseEvent != null) {
      Point p = myLastMouseEvent.getPoint();
      if (myVertical) {
        index = myTable.rowAtPoint(p);
      } else {
        index = myTable.columnAtPoint(p);
      }
    }
    if (myLastMouseIndex >= 0 && myLastMouseIndex != index) {
      repaint(getElementRect(myLastMouseIndex));
    }
    if (index >= 0) {
      repaint(getElementRect(index));
    }
    myLastMouseIndex = index;
  }

  private Rectangle getElementRect(int index) {
    if (myTable == null)
      return UIUtil.RECT_0000;
    Rectangle r;
    if (myVertical) {
      r = myTable.getCellRect(index, -1, true);
      r.x = 0;
      r.width = getWidth();
    } else {
      r = myTable.getCellRect(-1, index, true);
      r.y = 0;
      r.height = getHeight();
    }
    return r;
  }

  public void setPaintGrid(boolean value) {
    if (value != myPaintGrid) {
      myPaintGrid = value;
      repaint();
    }
  }

  static class GridHeaderCellState extends CellState {
    private final int myIndex;
    private final JTable myTable;
    private final boolean myVertical;
    private final MouseEvent myLastMouseEvent;

    private String myTooltip;
    private Cursor myCursor;

    public GridHeaderCellState(int index, JTable table, boolean vertical, MouseEvent lastMouseEvent) {
      myIndex = index;
      myTable = table;
      myVertical = vertical;
      myLastMouseEvent = lastMouseEvent;
    }

    @Override
    public Color getBackground() {
      return getBackground(true);
    }

    public Color getBackground(boolean opaque) {
      return getDefaultBackground();
    }

    @Nullable
    public Border getBorder() {
      return null;
    }

    public Color getDefaultBackground() {
      return myTable == null ? Color.WHITE : myTable.getBackground();
    }

    public Color getSelectionBackground() {
      return myTable == null ? Color.WHITE : myTable.getSelectionBackground();
    }

    public Color getDefaultForeground() {
      return myTable == null ? Color.WHITE : myTable.getForeground();
    }

    public Font getFont() {
      if (myTable == null) {
        assert false;
        return null;
      } else {
      }
      return myTable.getFont();
    }

    public Color getForeground() {
      return getDefaultForeground();
    }

    public boolean isEnabled() {
      return true;
    }

    public boolean isExpanded() {
      return true;
    }

    public boolean isFocused() {
      return false;
    }

    public boolean isLeaf() {
      return true;
    }

    public boolean isSelected() {
      return false;
    }

    public int getCellColumn() {
      return myVertical ? -1 : myIndex;
    }

    public boolean isExtracted() {
      return false;
    }

    public int getCellRow() {
      return myVertical ? myIndex : -1;
    }

    public int getComponentCellWidth() {
      return 0;
    }

    public int getComponentCellHeight() {
      return 0;
    }

    public void setFeedbackTooltip(String tooltip) {
      myTooltip = tooltip;
    }

    public void setFeedbackCursor(Cursor cursor) {
      myCursor = cursor;
    }

    @Nullable
    public MouseEvent getMouseEvent() {
      return myLastMouseEvent;
    }

    public Pattern getHighlightPattern() {
      return null;
    }
  }
}
