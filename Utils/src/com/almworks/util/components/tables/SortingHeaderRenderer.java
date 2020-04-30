package com.almworks.util.components.tables;

import com.almworks.util.Env;
import com.almworks.util.components.DelegatingGraphics;
import com.almworks.util.images.Icons;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.AttributedCharacterIterator;

class SortingHeaderRenderer<T> extends BaseRendererComponent implements TableCellRenderer {
  private static final int LEFT_GAP = 4;
  private static final int TEXT_ICON_GAP = 4;
  private final TableCellRenderer myRenderer;
  private Icon myIcon = null;
  private JComponent myLastComponent = null;
  private final CellRendererPane myRendererPane = new CellRendererPane();
  private SortingTableHeaderController<T> myHeaderController;
  private JLabel myFallbackComponent;

  public SortingHeaderRenderer(SortingTableHeaderController<T> headerController, TableCellRenderer renderer) {
    super();
    myHeaderController = headerController;
    myRenderer = renderer;
    add(myRendererPane);
    setOpaque(true);
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
    int row, int column)
  {
    TableColumnAccessor<T, ?> accessor = null;
    try {
      TableColumn tableColumn = myHeaderController.getSwingColumnModel().getColumn(column);
      accessor = myHeaderController.getColumnAccessor(tableColumn);
    } catch (IndexOutOfBoundsException e) {
      Log.warn("rendering problem", e);
    }
    int sortState;
    if (accessor != null) {
      sortState = myHeaderController.getColumnSortState(accessor);
      setToolTipText(accessor.getHeaderTooltip());
    } else {
      sortState = 0;
      setToolTipText(null);
    }
    setIcon(sortState);
    try {
      myLastComponent =
        (JComponent) myRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    } catch (Exception e) {
      Log.debug("rendering problem", e);
      myLastComponent = getFallbackComponent(value);
    }
    return this;
  }

  private JComponent getFallbackComponent(Object value) {
    if (myFallbackComponent == null)
      myFallbackComponent = new JLabel();
    myFallbackComponent.setText(value == null ? "" : String.valueOf(value));
    return myFallbackComponent;
  }

  private void setIcon(int sortState) {
    Icon icon;
    if (sortState > 0)
      icon = Icons.TABLE_COLUMN_SORTED_ASCENDING;
    else if (sortState < 0)
      icon = Icons.TABLE_COLUMN_SORTED_DESCENDING;
    else
      icon = null;
    myIcon = icon;
  }

  public Dimension getPreferredSize() {
    Dimension size = myLastComponent.getPreferredSize();
    Insets insets = myLastComponent.getInsets();
    int height = Icons.TABLE_COLUMN_SORTED_ASCENDING.getIconHeight() + insets.top + insets.bottom;
    size.height = Math.max(size.height, height);
    if (myIcon == null)
      return size;
    size.width += myIcon.getIconWidth() + TEXT_ICON_GAP + LEFT_GAP;
    return size;
  }

  public void paint(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    if (Env.isMac()) {
      paintMac(g);
    } else {
      paintNonMac(g);
    }
  }

  private void paintMac(Graphics g) {
    CellRendererPane rendererPane = myRendererPane;
    JComponent c = myLastComponent;
    if (rendererPane != null && c != null) {
      try {
        rendererPane.paintComponent(g, c, this, 0, 0, getWidth(), getHeight(), true);
      } catch (NullPointerException e) {
        // see #1353 - and sun bug 6346886
        Log.warn("swing bug 6346886 manifested, ignoring");
        // maybe call full repaint?
      }
      if (myIcon != null) {
        int y = (getHeight() - myIcon.getIconHeight()) >> 1;
        int x = getWidth() - myIcon.getIconWidth() - 4;
        if (x > 4) {
          Graphics g2 = g.create();
          try {
            myIcon.paintIcon(this, g2, x, y);
          } finally {
            g2.dispose();
          }
        }
      }
    }
  }

  private void paintNonMac(Graphics g) {
    CellRendererPane rendererPane = myRendererPane;
    JComponent c = myLastComponent;
    if (rendererPane != null && c != null) {
      DelegatingGraphics dg = new MyDelegatingGraphics(g);
      try {
        rendererPane.paintComponent(dg, c, this, 0, 0, getWidth(), getHeight(), true);
      } catch (NullPointerException e) {
        // see #1353 - and sun bug 6346886
        Log.warn("swing bug 6346886 manifested, ignoring");
        // maybe call full repaint?
      }
    }
  }


  private class MyDelegatingGraphics extends DelegatingGraphics {
    public MyDelegatingGraphics(Graphics g) {
      super((Graphics2D) g);
    }

    protected DelegatingGraphics wrap(Graphics2D graphics2D) {
      return new MyDelegatingGraphics(graphics2D);
    }

    public void drawString(String str, float x, float y) {
      if (myIcon == null) {
        super.drawString(str, x, y);
        return;
      }
      String text = correctText(str, (int) x);
      if (text != null) {
        super.drawString(text, x, y);
        x += getFontMetrics().stringWidth(text);
      }
      paintIcon((int) x);
    }

    public void drawString(String str, int x, int y) {
      if (myIcon == null) {
        super.drawString(str, x, y);
        return;
      }
      String text = correctText(str, x);
      if (text != null) {
        super.drawString(text, x, y);
        x += getFontMetrics().stringWidth(text);
      }
      paintIcon(x);
    }

    private void paintIcon(int x) {
      assert myIcon != null;
      int y = (myLastComponent.getHeight() - myIcon.getIconHeight()) / 2;
      int xLimit = myLastComponent.getWidth() - LEFT_GAP - myIcon.getIconWidth();
      if (x + TEXT_ICON_GAP > xLimit)
        x = xLimit;
      else
        x += TEXT_ICON_GAP;
      myIcon.paintIcon(SortingHeaderRenderer.this, this, (int) x, y);
    }

    @Nullable
    private String correctText(String s, int x) {
      int maxWidth = myLastComponent.getWidth() - x - myIcon.getIconWidth() - TEXT_ICON_GAP - LEFT_GAP;
      FontMetrics fontMetrics = getFontMetrics();
      if (fontMetrics.stringWidth(s) <= maxWidth)
        return s;
      boolean endsWithDots = s.endsWith("...");
      String text;
      if (endsWithDots)
        text = s.substring(0, s.length() - 3);
      else
        text = s;
      maxWidth -= fontMetrics.stringWidth("...");
      if (maxWidth >= 0)
        return TextUtil.truncate(text, fontMetrics, maxWidth) + "...";
      else
        return null;
    }

    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
      //TODO implement
      super.drawString(iterator, x, y);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
      //TODO implement
      super.drawString(iterator, x, y);
    }
  }
}
