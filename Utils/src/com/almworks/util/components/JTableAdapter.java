package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Lazy;
import com.almworks.util.components.layout.WidthDrivenCollectionRenderer;
import com.almworks.util.components.plaf.macosx.MacKeyboardSelectionPatch;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.model.LightScalarModel;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.MegaMouseAdapter;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.dnd.DndComponentAdapter;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author : Dyoma
 */
public class JTableAdapter<T> extends JTable implements DndComponentAdapter<TableDropHint> {
  private static final int MINIMUM_COLUMN_WIDTH = 16;

  private static final int MINIMUM_PAINT_EXCEPTION_LOG_PERIOD = 60000;
  private static long ourLastPaintExceptionTime;

  private static final CellRendererPane CELL_RENDERER_PANE = new CellRendererPane();

  private boolean myRowHeightByRenderer = false;
  private boolean myDefaultFocusTraversalKeys = false;

  private Point myLastMousePoint;
  private MegaMouseAdapter myMouseAdapter;
  private FocusListener myFocusListener;

  private final Lifecycle myInsertTargetHintLifecycle = new Lifecycle();
  private final TableModelListener myInsertHintModelListener = new DropHintInvalidator();
  private boolean myDndActive;
  private boolean myDndEnabled;
  private final LightScalarModel<Boolean> myInhibitSelectionChange = LightScalarModel.create(false);
  private TableDropHint myDropHint;
  private int[] mySelectionAfterDnd;
  private Pattern myHighlightPattern;
  private boolean myStriped;
  private boolean myColumnLinesPainted;
  private boolean myColumnBackgroundsPainted;
  private Color myStripeBackground;

  public JTableAdapter(TableModelAdapter<T> tableModel) {
    super(tableModel);
    int h = UIManager.getInt("Table.rowHeight");
    if (h > 0)
      setRowHeight(h);
    setupSelectionModel();
    updatePreferredScrollableViewportSize();
  }

  private void setupSelectionModel() {
    try {
      setSelectionModel(new PatchedListSelectionModel(myInhibitSelectionChange));
    } catch(CantPerformException e) {
      Log.warn("PLSM installation failed: " + e.getCause().getClass().getSimpleName());
      setSelectionModel(new VetoableListSelectionModel(myInhibitSelectionChange));
    }
  }

  public void setStriped(boolean striped) {
    if (myStriped != striped) {
      myStriped = striped;
      setOpaque(!striped);
      repaint();
    }
  }

  public boolean isStriped() {
    return myStriped;
  }

  /**
   * Turns column lines painting on or off.
   * If turned on, the values returned by {@link TableColumnAccessor}s
   * for the {@link TableColumnAccessor#LINE_EAST_HINT} are respected.
   * @param painted Whether to paint column lines or not.
   * todo Most cell renderers are opaque, so the lines won't show through.
   */
  public void setColumnLinesPainted(boolean painted) {
    if(myColumnLinesPainted != painted) {
      myColumnLinesPainted = painted;
      repaint();
    }
  }

  /**
   * Turns column backgrounds painting on or off.
   * If turned on, the values returned by {@link TableColumnAccessor}s
   * for the {@link TableColumnAccessor#BACKGROUND_COLOR_HINT} and
   * {@link TableColumnAccessor#BACKGROUND_ALPHA_HINT} are respected.
   * @param painted Whether to paint column backgrounds or not.
   * todo Most cell renderers are opaque, so the background won't show through.
   */
  public void setColumnBackgroundsPainted(boolean painted) {
    if(myColumnBackgroundsPainted != painted) {
      myColumnBackgroundsPainted = painted;
      repaint();
    }
  }

  private void updatePreferredScrollableViewportSize() {
    Dimension ps = getPreferredSize();
    Dimension vs = getPreferredScrollableViewportSize();
    if (!Util.equals(ps, vs)) {
      setPreferredScrollableViewportSize(ps);
    }
  }

  public void updateUI() {
    MegaMouseAdapter mouseAdapter = getMouseAdapter();
    FocusListener focusListener = getFocusListener();

    removeMouseListener(mouseAdapter);
    removeMouseMotionListener(mouseAdapter);
    removeFocusListener(focusListener);

    super.updateUI();

    addMouseListener(mouseAdapter);
    addMouseMotionListener(mouseAdapter);
    addFocusListener(focusListener);

    // todo #1434
//    setFocusCycleRoot(false);
//    InputMap map1 = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//    map1.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "noAction");
//    map1.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "noAction");
    MacKeyboardSelectionPatch.install(this);
  }

  public ATable<T> getATable() {
    Container parent = getParent();
    assert parent instanceof ATable : parent;
    //noinspection unchecked
    return (ATable<T>) parent;
  }

  private MegaMouseAdapter getMouseAdapter() {
    if (myMouseAdapter == null) {
      myMouseAdapter = new MegaMouseAdapter() {
        protected void onMouseEvent(MouseEvent e) {
          myLastMousePoint = e.getPoint();
        }
      };
    }
    return myMouseAdapter;
  }

  private FocusListener getFocusListener() {
    if (myFocusListener == null) {
      myFocusListener = new FocusListener() {
        public void focusGained(FocusEvent e) {
          repaintSelection(JTableAdapter.this);
        }

        public void focusLost(FocusEvent e) {
          repaintSelection(JTableAdapter.this);
        }
      };
    }
    return myFocusListener;
  }

  @Override
  public void doLayout() {
    super.doLayout();
    updatePreferredScrollableViewportSize();
  }

  static void repaintSelection(JTable table) {
    ListSelectionModel model = table.getSelectionModel();
    int min = model.getMinSelectionIndex();
    if (min < 0)
      return;
    int max = model.getMaxSelectionIndex();
    if (max < min)
      return;
    if (table.getColumnCount() < 1)
      return;
    Dimension size = table.getSize();
    for (int i = min; i <= Math.min(max, table.getRowCount() - 1); i++) {
      if (model.isSelectedIndex(i)) {
        Rectangle rect = table.getCellRect(i, 0, true);
        if (rect != null) {
          rect.x = 0;
          rect.width = size.width;
          table.repaint(rect);
        }
      }
    }
  }


  public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
    if (myDefaultFocusTraversalKeys) {
      Container parent = getParent();
      if (parent != null)
        return parent.getFocusTraversalKeys(id);
    }
    return super.getFocusTraversalKeys(id);
  }

  public void setDefaultFocusTraversalKeys(boolean isDefault) {
    myDefaultFocusTraversalKeys = isDefault;
  }

  public synchronized void addMouseListener(MouseListener l) {
    super.addMouseListener(l);
  }

  @Nullable
  public String getToolTipText() {
    return getTooltip(myLastMousePoint);
  }

  @Nullable
  public String getToolTipText(MouseEvent event) {
    return getTooltip(event.getPoint());
  }

  @Nullable
  private String getTooltip(Point point) {
    if (point == null) return null;
    int row = rowAtPoint(point);
    int column = columnAtPoint(point);
    if (column >= 0 && row >= 0) {
      TableColumnModel columnModel = getColumnModel();
      if (column < columnModel.getColumnCount()) {
        TableColumn tableColumn = columnModel.getColumn(column);
        int modelIndex = tableColumn.getModelIndex();
        AListModel<TableColumnAccessor<? super T, ?>> columns = getColumns();
        if (modelIndex >= 0 && modelIndex < columns.getSize()) {
          TableColumnAccessor<? super T, ?> accessor = columns.getAt(modelIndex);
          AListModel<? extends T> model = getModel().getData();
          if (row < model.getSize()) {
            T element = model.getAt(row);
            Rectangle cellRect = getCellRect(row, column, false);
            Point cellPoint = new Point(point.x - cellRect.x, point.y - cellRect.y);
            ColumnTooltipProvider<? super T> provider = accessor.getTooltipProvider();
            if (provider != null) {
              String tip =
                provider.getTooltip(getCellState(row, column), element, cellPoint, cellRect);
              if (tip != null) {
                return tip;
              }
            }
          }
        }
      }
    }

    Container p = getParent();
    if (p instanceof ATable) {
      TableTooltipProvider provider = ((ATable<?>) p).getTooltipProvider();
      if (provider != null) {
        String tip = provider.getTooltip(row, column, point);
        if (tip != null) {
          return tip;
        }
      }
    } else {
      assert false : this;
    }

    return null;
  }

  public void tableChanged(TableModelEvent e) {
    if (!getModel().isInitialized())
      return;
    if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
      if (e instanceof TableModelAdapter.HeaderTableModelEvent) {
        int type = e.getType();
        TableModelAdapter.HeaderTableModelEvent headerEvent = (TableModelAdapter.HeaderTableModelEvent) e;
        int firstIndex = headerEvent.getIndex();
        if (type == TableModelEvent.INSERT) {
          for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            int modelIndex = column.getModelIndex();
            if (modelIndex >= firstIndex)
              column.setModelIndex(modelIndex + headerEvent.getLength());
          }
          TableColumnModel columnModel = getColumnModel();
          for (int i = firstIndex; i <= headerEvent.getLastIndex(); i++) {
            TableColumn column = new MyTableColumn(i, getColumns().getAt(i));
            addColumn(column);
            int addedIndex = columnModel.getColumnCount() - 1;
            updateColumnPreferredWidths(addedIndex, addedIndex);
            assert i <= addedIndex : "i=" + i + " addedIndex=" + addedIndex;
            if (addedIndex != i)
              columnModel.moveColumn(addedIndex, i);
          }
        } else if (type == TableModelEvent.DELETE) {
          removeColumnsWithModelIndices(firstIndex, headerEvent.getLength());
        } else if (type == TableModelEvent.UPDATE) {
          for (int i = firstIndex; i <= headerEvent.getLastIndex(); i++) updateColumnWithModelIndex(i);
        }
      } else {
        resizeAndRepaint();
      }
      resetTableHeights();
    } else {
//      System.out.println(e.getFirstRow() + " -- " + e.getLastRow() + " : " + e.getType());
      super.tableChanged(e);
      updateRowHeights(e.getFirstRow(), e.getLastRow());
      if (isEditing()) {
        TableCellEditor editor = getCellEditor();
        if (editor != null)
          editor.cancelCellEditing();
      }
    }
  }

  private void updateColumnWithModelIndex(int modelIndex) {
    TableColumnModel columnModel = getColumnModel();
    for (int i = 0; i < columnModel.getColumnCount(); i++) {
      MyTableColumn column = Util.castNullable(MyTableColumn.class, columnModel.getColumn(i));
      if (column == null || column.getModelIndex() != modelIndex) continue;
      TableColumnAccessor<? super T, ?> accessor = getColumns().getAt(modelIndex);
      if (accessor == column.myAccessor) break;
      int preferredWidth = column.getPreferredWidth();
      columnModel.removeColumn(column);
      column = new MyTableColumn(modelIndex, accessor);
      column.setPreferredWidth(preferredWidth);
      columnModel.addColumn(column);
      int lastIndex = columnModel.getColumnCount() - 1;
      if (i != lastIndex) columnModel.moveColumn(lastIndex, i);
    }
  }

  public void repaint(long tm, int x, int y, int width, int height) {
//    if (width > 0 && height > 0 && y == 0)
//      System.out.println("x=" + x + "; y=" + y + "; width=" + width + "; height=" + height);
    super.repaint(tm, x, y, width, height);
  }

  protected void paintComponent(Graphics g) {
//    boolean debug = "TCI.Table".equals(getName());
//    if (debug) {
//      __time.start();
//      Rectangle clip = g.getClipBounds();
//      if (clip.height >= 50) {
//        __kind.b();
//      } else {
//        __kind.a();
//      }
//    }
    AwtUtil.applyRenderingHints(g);
    paintTable(g);
//    if (debug) {
//      __time.stop();
//    }
    TableDropHint dropHint = myDropHint;
    if (dropHint != null) {
      dropHint.paint(g, this);
    }
  }

  private void paintTable(Graphics g) {
    try {
      Rectangle clip = g.getClipBounds();
      if (clip == null) {
        clip = new Rectangle(0, 0, getWidth(), getHeight());
      }

      if(myStriped) {
        paintStripes(g, clip);
      }

      if(myColumnBackgroundsPainted) {
        paintBackgrounds(g, clip);
      }

      if(myColumnLinesPainted) {
        paintLines(g, clip);
      }
      super.paintComponent(g);
    } catch (Exception e) {
      long now = System.currentTimeMillis();
      if (now > ourLastPaintExceptionTime + MINIMUM_PAINT_EXCEPTION_LOG_PERIOD) {
        ourLastPaintExceptionTime = now;
        Log.warn("exception when painting table", e);
      }
    }
  }

  private void paintBackgrounds(Graphics g, Rectangle clip) {
    if(!(g instanceof Graphics2D)) {
      return;
    }

    final TableColumnModel swingColumnModel = getColumnModel();

    Graphics2D g2 = null;
    int x = 0;
    final int y = clip.y;
    final int h = clip.height;

    final int nc = swingColumnModel.getColumnCount();
    for(int i = 0; i < nc; i++) {
      final TableColumn swingColumn = swingColumnModel.getColumn(i);
      final int w = swingColumn.getWidth();

      final TableColumnAccessor<?, ?> accessor = ATable.getColumnAccessor(swingColumn);
      if(accessor != null) {
        final Color color = accessor.getHint(TableColumnAccessor.BACKGROUND_COLOR_HINT);
        if(color != null) {
          if(g2 == null) {
            g2 = (Graphics2D) g.create();
          }
          final float alpha = Util.NN(accessor.getHint(TableColumnAccessor.BACKGROUND_ALPHA_HINT), 1.0f);

          g2.setColor(color);
          g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
          g2.fillRect(x, y, w, h);
        }
      }

      x += w;
    }

    if(g2 != null) {
      g2.dispose();
    }
  }

  private void paintLines(Graphics g, Rectangle clip) {
    final TableColumnModel swingColumnModel = getColumnModel();

    g.setColor(AwtUtil.getPanelBackground());

    int x = -1;
    final int y1 = clip.y;
    final int y2 = y1 + clip.height;

    final int nc = swingColumnModel.getColumnCount();
    for(int i = 0; i < nc; i++) {
      final TableColumn swingColumn = swingColumnModel.getColumn(i);
      x += swingColumn.getWidth();

      final TableColumnAccessor<?, ?> accessor = ATable.getColumnAccessor(swingColumn);
      if(accessor != null && Boolean.TRUE.equals(accessor.getHint(TableColumnAccessor.LINE_EAST_HINT))) {
        g.drawLine(x, y1, x, y2);
      }
    }
  }

  private void paintStripes(Graphics g, Rectangle clip) {
    final int height = clip.y + clip.height;
    for(int i = 0; i <= height / rowHeight; i++) {
      g.setColor(i % 2 == 0 ? getBackground() : getStripeBackground());
      g.fillRect(clip.x, i * rowHeight, clip.width, rowHeight);
    }
  }

  public Color getStripeBackground() {
    Color r = myStripeBackground;
    if (r == null) {
      myStripeBackground = r = ColorUtil.getStripeBackground(getBackground());
    }
    return r;
  }

  /**
   * Paints the given JTable's table default header background at given
   * x for the given width.
   */
  private static void paintHeader(Graphics g, JTable table, int x, int width) {
    if(table.getColumnCount() > 0) {
      AwtUtil.applyRenderingHints(g);
      final TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
      final Component component = renderer.getTableCellRendererComponent(table, "", false, false, -1, 0);
      component.setBounds(0, 0, width, table.getTableHeader().getHeight());
      ((JComponent) component).setOpaque(false);
      CELL_RENDERER_PANE.paintComponent(g, component, null, x, 0, width, table.getTableHeader().getHeight(), true);
    }
  }

  /**
   * Creates a component that paints the header background for use in a
   * JScrollPane corner.
   */
  public static JComponent createCornerComponent(final JTable table) {
    return new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
        paintHeader(g, table, 0, getWidth());
      }
    };
  }

  @Override
  protected void configureEnclosingScrollPane() {
    super.configureEnclosingScrollPane();

    // Installing a header-like upper right corner.
    final JScrollPane jsp = SwingTreeUtil.findAncestorOfType(this, JScrollPane.class);
    if(jsp != null && jsp.getCorner(JScrollPane.UPPER_RIGHT_CORNER) == null) {
      jsp.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createCornerComponent(this));
    }
  }

  private void resetTableHeights() {
    if (myRowHeightByRenderer) {
      updateRowHeights();
    } else {
      int rowHeight = getRowHeight();
      if (rowHeight > 0)
        setRowHeight(rowHeight);
    }
  }

  private void removeColumnsWithModelIndices(int firstIndex, int length) {
    TableColumnModel columnModel = getColumnModel();
    int i = 0;
    while (i < columnModel.getColumnCount()) {
      TableColumn column = columnModel.getColumn(i);
      int modelIndex = column.getModelIndex();
      if (firstIndex <= modelIndex && modelIndex < firstIndex + length) {
        columnModel.removeColumn(column);
        continue;
      } else if (modelIndex >= firstIndex + length)
        column.setModelIndex(modelIndex - length);
      i++;
    }
  }

  public TableModelAdapter<T> getModel() {
    return (TableModelAdapter<T>) super.getModel();
  }

  public void columnAdded(TableColumnModelEvent e) {
    super.columnAdded(e);
//    updateColumnPreferredWidths(e.getFromIndex(), e.getToIndex());
    updateRowHeights();
  }

  /**
   * @deprecated do not call directly
   */
  public void setModel(TableModel dataModel) {
    assert dataModel instanceof TableModelAdapter;
    setTableModel((TableModelAdapter<T>) dataModel);
  }

  void setTableModel(TableModelAdapter<T> model) {
    super.setModel(model);
    if (getTableHeader() == null)
      return;
    updateColumnPreferredWidths(0, columnModel.getColumnCount() - 1);
    updateRowHeights();
    updatePreferredScrollableViewportSize();
  }

  private void updateRowHeights() {
    if (!myRowHeightByRenderer)
      return;
    TableModelAdapter<T> model = getModel();
    if (model == null)
      return;
    int count = model.getRowCount();
    if (count == 0)
      return;
    updateRowHeights(0, count - 1);
  }

  public void addNotify() {
    super.addNotify();
    updateRowHeights();
  }

  @SuppressWarnings({"Deprecation"})
  @Deprecated
  public void reshape(int x, int y, int w, int h) {
    int oldWidth = getWidth();
    super.reshape(x, y, w, h);
    if (oldWidth != w)
      updateRowHeights();
  }

  private void updateRowHeights(int fromIndex, int toIndex) {
    if (!myRowHeightByRenderer)
      return;
    if (!isDisplayable())
      return;
    if (toIndex < fromIndex)
      return;
    AListModel<TableColumnAccessor<? super T, ?>> columns = getColumns();
    if (columns.getSize() <= 0)
      return;
    List<? extends T> data = getModel().getData().toList();

    if (fromIndex < 0)
      fromIndex = 0;
    if (toIndex >= data.size())
      toIndex = data.size() - 1;
    if (toIndex < fromIndex)
      return;

    if (getSize().width <= 0)
      return;

    // make sure column sizes are correct
    doLayout();

    for (int ri = fromIndex; ri <= toIndex; ri++) {
      T element = data.get(ri);
      int maxHeight = getRowHeight();

      int columnIndex = 0;
      for (int ci = 0; ci < columns.getSize(); ci++) {
        TableColumnAccessor<? super T, ?> column = columns.getAt(ci);
        assert columnIndex < columnModel.getColumnCount() : columnIndex + " " + columnModel.getColumnCount();
        int columnWidth = columnModel.getColumn(columnIndex++).getWidth() - columnModel.getColumnMargin();
        CollectionRenderer<? super T> renderer = column.getDataRenderer();
        int height = -1;
        TableCellState state = getCellState(ri, ci);
        if (renderer instanceof WidthDrivenCollectionRenderer) {
          height = ((WidthDrivenCollectionRenderer<T>) renderer).getPreferredHeight(state, element, columnWidth);
        } else {
          JComponent c = renderer.getRendererComponent(state, element);
          if (c != null) {
            if (c instanceof WidthFriendly) {
              ((WidthFriendly) c).setPreferredWidth(columnWidth);
            }
            Dimension pref = c.getPreferredSize();
            height = pref != null ? pref.height : 0;
          }
        }
        maxHeight = Math.max(maxHeight, height);
      }
      maxHeight += getRowMargin();
      int h = getRowHeight(ri);
      if (h != maxHeight) {
        setRowHeight(ri, maxHeight);
      }
    }
/*


    if (affected) {
      revalidate();
      repaint();
    }
*/
  }

  public TableCellState getCellState(int row, int column) {
    boolean selected = isCellSelected(row, column);
    boolean rowIsLead =
        (getSelectionModel().getLeadSelectionIndex() == row);
    boolean colIsLead =
        (getColumnModel().getSelectionModel().getLeadSelectionIndex() == column);

    boolean hasFocus = (rowIsLead && colIsLead) && ListSpeedSearch.isFocusOwner(this);
    return new TableCellState(this, selected, hasFocus, row, column);
  }

  public void createDefaultColumnsFromModel() {
    TableModel m = this.getModel();
    if (m != null) {
      // Remove any current columns
      TableColumnModel cm = this.getColumnModel();
      while (cm.getColumnCount() > 0) {
        cm.removeColumn(cm.getColumn(0));
      }

      // Create new columns from the data model info
      int initialCount = getColumnCount();
      for (int i = 0; i < m.getColumnCount(); i++) addColumn(new MyTableColumn(i, getColumns().getAt(i)));
      updateColumnPreferredWidths(initialCount, getColumnCount() - 1);
    }
  }
  
  public TableColumnAccessor<?, ?> getColumnAccessorAtVisual(int index) {
    if (index < 0) return null;
    TableColumnModel columns = getColumnModel();
    if (columns == null || columns.getColumnCount() <= index) return null;
    MyTableColumn column = Util.castNullable(MyTableColumn.class, columns.getColumn(index));
    if (column == null) return null;
    return column.myAccessor;
  }

  private void updateColumnPreferredWidths(int fromIndex, int toIndex) {
    TableColumnModel columnModel = getColumnModel();
    AListModel<TableColumnAccessor<? super T, ?>> columns = getColumns();
    for (int i = fromIndex; i <= toIndex; i++) {
      TableColumn column = columnModel.getColumn(i);
      int modelIndex = column.getModelIndex();
      if (modelIndex >= 0 && modelIndex < columns.getSize()) {
        final TableColumnAccessor<? super T, ?> modelColumn = columns.getAt(modelIndex);
        updateColumnPreferredWidth((TableColumnAccessor<T, ?>) modelColumn, column, i);
      }
    }
  }

  private void updateColumnPreferredWidth(TableColumnAccessor<T, ?> modelColumn, TableColumn column, int columnIndex) {
    int columnWidth = modelColumn.getPreferredWidth(this, getModel().getTableModel(), modelColumn, columnIndex);
    if (columnWidth > 0) {
      ColumnSizePolicy policy = modelColumn.getSizePolicy();
      policy.setWidthParameters(columnWidth, column);
    }
  }

  public void forcePreferredColumnWidth(int columnIndex) {
    TableColumnModel columnModel = getColumnModel();
    int columnCount = columnModel.getColumnCount();
    if (columnCount < 2) {
      return;
    }
    if (columnIndex < 0 || columnIndex >= columnCount) {
      assert false : columnIndex + " " + columnModel + " " + this;
      return;
    }
    TableColumn column = columnModel.getColumn(columnIndex);
    AListModel<TableColumnAccessor<? super T, ?>> columns = getColumns();
    int modelSize = columns.getSize();
    int modelIndex = column.getModelIndex();
    if (modelIndex >= 0 && modelIndex < modelSize) {
      final TableColumnAccessor<T, ?> modelColumn = (TableColumnAccessor<T, ?>) columns.getAt(modelIndex);
      int preferred = modelColumn.getPreferredWidth(this, getModel().getTableModel(), modelColumn, columnIndex);
      preferred = modelColumn.getSizePolicy().validateForcedWidth(column, preferred);
      if (preferred > 0) {
        int startingWidth = column.getWidth();
        int delta = preferred - startingWidth;
        if (delta != 0) {
          int compensated = compensateWidthChange(columnModel, columnIndex, columns, delta);
          if (compensated != 0) {
            int newWidth = startingWidth + compensated;
            column.setPreferredWidth(newWidth);
            column.setWidth(newWidth);
          }
        }
      }
    }
  }

  public void forcePreferredColumnWidths() {
    TableColumnModel columnModel = getColumnModel();
    int count = columnModel.getColumnCount();
    if (count < 2) {
      return;
    }

    AListModel<TableColumnAccessor<? super T, ?>> columns = getColumns();
    int totalWidth = getWidth();
    Insets insets = getInsets();
    Dimension dimension = getIntercellSpacing();
    totalWidth -= insets.left + insets.right + dimension.width * (count - 1);

    int wantedWidth = 0;
    final int[] widths = new int[count];
    Integer[] order = new Integer[count];
    for (int i = 0; i < count; i++) {
      order[i] = i;
      TableColumn column = columnModel.getColumn(i);
      int modelIndex = column.getModelIndex();
      if (modelIndex < 0 || modelIndex >= columns.getSize()) {
        // ??
        return;
      }
      final TableColumnAccessor<T, ?> modelColumn = (TableColumnAccessor<T, ?>) columns.getAt(modelIndex);
      int preferred = modelColumn.getPreferredWidth(this, getModel().getTableModel(), modelColumn, i);
      if (preferred > 0) {
        preferred = modelColumn.getSizePolicy().validateForcedWidth(column, preferred);
      }
      if (preferred <= 0) {
        preferred = column.getWidth();
      }
      preferred = Math.max(MINIMUM_COLUMN_WIDTH, preferred);

      widths[i] = preferred;
      wantedWidth += preferred;
    }
    Arrays.sort(order, new Comparator<Integer>() {
      public int compare(Integer o1, Integer o2) {
        return Containers.compareInts(widths[o1], widths[o2]);
      }
    });

    // use preferred size for all small columns up to threshold total width, then distribute extra 
    int threshold = totalWidth >> 1;

    int distributed = 0;
    int ci;
    for (ci = 0; ci < count; ci++) {
      int index = order[ci];
      int w = widths[index];
      if (distributed + w > threshold) {
        break;
      }
      TableColumn column = columnModel.getColumn(index);
      column.setPreferredWidth(w);
      column.setWidth(w);
      distributed += w;
    }

    if (ci == count) {
      // all fit into threshold
      int left = totalWidth - distributed;
      for (int i = count - 1; i >= 0; i--) {
        int index = order[i];
        TableColumn column = columnModel.getColumn(index);
        TableColumnAccessor<T, ?> modelColumn = (TableColumnAccessor<T, ?>) columns.getAt(column.getModelIndex());
        int w = column.getWidth() + left;
        int validated = modelColumn.getSizePolicy().validateForcedWidth(column, w);
        if (validated == w) {
          column.setPreferredWidth(w);
          column.setWidth(w);
          break;
        }
      }
      return;
    }

    int wanted = 0;
    for (int i = ci; i < count; i++) {
      wanted += widths[order[i]];
    }
    if (wanted <= 0) {
      assert false : wanted + " " + this;
      wanted = 1;
    }
    double rate = 1D * (totalWidth - distributed) / wanted;
    for (; ci < count; ci++) {
      int index = order[ci];
      int w = ci == count - 1 ? totalWidth - distributed : (int) Math.round(widths[index] * rate);
      // todo w is not validated with size policy here
      TableColumn column = columnModel.getColumn(index);
      column.setPreferredWidth(w);
      column.setWidth(w);
      distributed += w;
    }
  }

  private int compensateWidthChange(final TableColumnModel columnModel, final int columnIndex,
    AListModel<TableColumnAccessor<? super T, ?>> columns, int delta)
  {
    int remaining = -delta;
    int count = columnModel.getColumnCount();
    for (int i = columnIndex + 1; remaining != 0 && i < count; i++) {
      remaining = compensateWidthChangeOnColumn(columnModel, i, columns, remaining, true);
    }
    for (int i = columnIndex - 1; remaining != 0 && i >= 0; i--) {
      remaining = compensateWidthChangeOnColumn(columnModel, i, columns, remaining, true);
    }
    // if preferred sizes do not accomodate for delta, check columns by the order of their widths
    if (remaining != 0) {
      Integer[] indices = new Integer[count];
      for (int i = 0; i < count; i++) {
        indices[i] = i;
      }
      Arrays.sort(indices, new Comparator<Integer>() {
        public int compare(Integer o1, Integer o2) {
          int w1 = columnModel.getColumn(o1).getWidth();
          int w2 = columnModel.getColumn(o2).getWidth();
          if (w1 != w2) {
            return -Containers.compareInts(w1, w2);
          } else {
            return Containers.compareInts(Math.abs(o1 - columnIndex), Math.abs(o2 - columnIndex));
          }
        }
      });
      for (int i = 0; remaining != 0 && i < count; i++) {
        int index = indices[i];
        if (index != columnIndex) {
          remaining = compensateWidthChangeOnColumn(columnModel, index, columns, remaining, false);
        }
      }
    }
    return delta + remaining;
  }

  private int compensateWidthChangeOnColumn(TableColumnModel columnModel, int targetIndex,
    AListModel<TableColumnAccessor<? super T, ?>> columns, int remaining, boolean usePreferred)
  {
    TableColumn c = columnModel.getColumn(targetIndex);
    int mi = c.getModelIndex();
    if (mi >= 0 && mi < columns.getSize()) {
      TableColumnAccessor<T, ?> mc = (TableColumnAccessor<T, ?>) columns.getAt(mi);
      int currentWidth = c.getWidth();
      int newWidth;
      if (usePreferred) {
        newWidth = mc.getPreferredWidth(this, getModel().getTableModel(), mc, targetIndex);
        int change = newWidth - currentWidth;
        if ((change ^ remaining) < 0) {
          // suggested width change and space-to-be-distributed are of different signs
          newWidth = 0;
        } else {
          if ((remaining < 0 && change < remaining) || (remaining > 0 && change > remaining)) {
            // preferred size is overly allowing
            newWidth = currentWidth + remaining;
          }
        }
      } else {
        newWidth = Math.max(MINIMUM_COLUMN_WIDTH, currentWidth + remaining);
      }
      if (newWidth > 0) {
        int validWidth = mc.getSizePolicy().validateForcedWidth(c, newWidth);
        if (validWidth > 0) {
          int newRemaining = remaining - (validWidth - currentWidth);
          if (newRemaining != 0 && ((newRemaining ^ remaining) < 0)) {
            // sign has changed??
            assert false : remaining + " " + newRemaining;
            validWidth = newWidth;
            newRemaining = 0;
          }
          c.setPreferredWidth(validWidth);
          c.setWidth(validWidth);
          remaining = newRemaining;
        }
      }
    }
    return remaining;
  }


  private AListModel<TableColumnAccessor<? super T, ?>> getColumns() {
    return getModel().getColumns();
  }

  public void setRowHeightByRenderer(boolean rowHeightByRenderer) {
    if (myRowHeightByRenderer != rowHeightByRenderer) {
      myRowHeightByRenderer = rowHeightByRenderer;
      if (myRowHeightByRenderer)
        updateRowHeights();
    }
  }

  public void resizeColumns() {
    int count = getColumnCount();
    if (count > 0)
      updateColumnPreferredWidths(0, count - 1);
  }

  public void resizeColumn(TableColumnAccessor<?, ?> columnAccessor) {
    TableColumnModel columnModel = getColumnModel();
    int columnCount = columnModel.getColumnCount();
    if (columnCount == 0)
      return;
    AListModel<TableColumnAccessor<? super T, ?>> columns = getColumns();
    int modelIndex = -1;
    TableColumnAccessor<T, ?> modelColumn = null;
    for (int i = 0; i < columns.getSize(); i++) {
      TableColumnAccessor<? super T, ?> accessor = columns.getAt(i);
      if (accessor != null && accessor.getId().equals(columnAccessor.getId())) {
        modelIndex = i;
        modelColumn = (TableColumnAccessor<T, ?>) accessor;
        break;
      }
    }
    if (modelIndex < 0 || modelColumn == null)
      return;
    for (int i = 0; i < columnCount; i++) {
      TableColumn column = columnModel.getColumn(i);
      if (modelIndex == column.getModelIndex()) {
        updateColumnPreferredWidth(modelColumn, column, i);
        break;
      }
    }
  }

  @Nullable
  public Component prepareEditor(TableCellEditor editor, int row, int column) {
    Component component = super.prepareEditor(editor, row, column);
    if (component == null)
      return null;
    if (editor instanceof TableCellEditorAdapter) {
      if (!((TableCellEditorAdapter<?>) editor).startEditing(this, row))
        return null;
    } else
      assert false;
    return component;
  }

  public void setDndActive(boolean dndActive, boolean dndEnabled) {
    myInhibitSelectionChange.setValue(dndActive);
    if (myDndActive != dndActive) {
      myDndActive = dndActive;
      myDndEnabled = dndEnabled;
      if (!dndActive || !dndEnabled) {
        setDropHint(null);
      }
      JScrollPane scrollPane = SwingTreeUtil.findAncestorOfType(this, JScrollPane.class);
      if (scrollPane instanceof AScrollPane) {
        ((AScrollPane) scrollPane).setDndActive(dndActive && dndEnabled);
      }
    }
    if (!dndActive && mySelectionAfterDnd != null) {
      ListSelectionModel model = getSelectionModel();
      model.clearSelection();
      for (int index : mySelectionAfterDnd)
        model.addSelectionInterval(index, index);
      mySelectionAfterDnd = null;
    }
  }

  public void setSelectionWhenDndDone(int[] indeces) {
    assert myDndActive;
    mySelectionAfterDnd = indeces;
  }

  public boolean isDndWorking() {
    return myDndActive && myDndEnabled;
  }

  public void setDropHint(TableDropHint hint) {
    if (!Util.equals(myDropHint, hint)) {
      if (myDropHint != null)
        myDropHint.repaint(this);
      myDropHint = hint;
      myInsertTargetHintLifecycle.cycle();
      if (myDropHint != null) {
        assert hint.isValid() : hint;
        myDropHint.repaint(this);
        UIUtil.addTableModelListener(myInsertTargetHintLifecycle.lifespan(), getModel(), myInsertHintModelListener);
      }
    }
  }

  public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
    // do not touch selection if dnd is in progress
    if (!myInhibitSelectionChange.getValue()) {
      super.changeSelection(rowIndex, columnIndex, toggle, extend);
    }
  }

  public void setHighlightPattern(Pattern highlightPattern) {
    myHighlightPattern = highlightPattern;
    repaint();
  }

  private Pattern getHighlightPattern() {
    return myHighlightPattern;
  }

  static class TableCellRendererAdapter<T> implements TableCellRenderer {
    private final CollectionRenderer<T> myRenderer;

    public TableCellRendererAdapter(CollectionRenderer<T> renderer) {
      myRenderer = renderer;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column)
    {
      JTableAdapter<T> tjTableAdapter = (JTableAdapter<T>) table;
      T rowItem = tjTableAdapter.getModel().getData().getAt(row);
      hasFocus = ListSpeedSearch.fixFocusedState(table, hasFocus, row, column);
      TableCellState state = new TableCellState(table, isSelected, hasFocus, row, column);
      state.setHighlightPattern(tjTableAdapter.getHighlightPattern());
      return myRenderer.getRendererComponent(state, rowItem);
    }

    public CollectionRenderer<T> getRenderer() {
      return myRenderer;
    }
  }


  static class TableCellEditorAdapter<T> implements TableCellEditor {
    private final CollectionEditor<? super T> myEditor;
    private final Lazy<FireEventSupport<CellEditorListener>> myEvents =
      new Lazy<FireEventSupport<CellEditorListener>>() {
        @NotNull
        public FireEventSupport<CellEditorListener> instantiate() {
          return FireEventSupport.create(CellEditorListener.class);
        }
      };
    private static final int NO_ROW = -2;
    private int myEditingRow = NO_ROW;
    private JTable myEditingTable = null;

    private boolean mySelectEditorCell;

    public TableCellEditorAdapter(CollectionEditor<T> editor) {
      myEditor = editor;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      Object rowItem = getData(table, row);
      mySelectEditorCell = myEditor.shouldSelect(table, row, column, (T) rowItem);

      // if cell should be selected, provide selected cell state. selection will happen later
      boolean selected = isSelected || mySelectEditorCell;

      return myEditor.getEditorComponent(new TableCellState(table, selected, true, row, column), (T) rowItem);
    }

    @Nullable
    private T getData(JTable table, int row) {
      if (table == null)
        return null;
      AListModel<? extends T> model = ((JTableAdapter<T>) table).getModel().getData();
      if (row < 0 || row >= model.getSize())
        return null;
      else
        return model.getAt(row);
    }

    public void addCellEditorListener(CellEditorListener listener) {
      myEvents.get().addAWTListener(Lifespan.FOREVER, listener);
    }

    public void cancelCellEditing() {
      T data = getData(myEditingTable, myEditingRow);
      myEditor.stopEditing(data, false);
      myEditingTable = null;
      myEditingRow = NO_ROW;
      if (myEvents.isInitialized())
        myEvents.get().getDispatcher().editingCanceled(new ChangeEvent(this));
    }

    @Nullable
    public Object getCellEditorValue() {
      // we can return null because TableModel.setValueAt() is empty
      return null;
    }

    public boolean isCellEditable(EventObject event) {
      return myEditor.shouldEdit(event);
    }

    public void removeCellEditorListener(CellEditorListener listener) {
      if (myEvents.isInitialized())
        myEvents.get().removeListener(listener);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
      return mySelectEditorCell;
    }

    public boolean stopCellEditing() {
      T data = getData(myEditingTable, myEditingRow);
      boolean accept = myEditor.stopEditing(data, true);
      if (accept) {
        myEditingTable = null;
        myEditingRow = NO_ROW;
        if (myEvents.isInitialized())
          myEvents.get().getDispatcher().editingStopped(new ChangeEvent(this));
      }
      return accept;
    }

    public boolean startEditing(JTable table, int row) {
      if (myEditingTable != null || myEditingRow != NO_ROW) {
        if (myEditingRow != row || myEditingTable != table) {
          cancelCellEditing();
        }
      }
      myEditingTable = table;
      myEditingRow = row;
      T data = getData(table, row);
      return myEditor.startEditing(data);
    }
  }


  private static class MyTableColumn extends TableColumn {
    private final TableColumnAccessor<?, ?> myAccessor;

    public MyTableColumn(int modelIndex, TableColumnAccessor<?, ?> accessor) {
      super(modelIndex);
      myAccessor = accessor;
      setIdentifier(myAccessor);
    }

    public void setCellRenderer(TableCellRenderer cellRenderer) {
      assert false;
      super.setCellRenderer(cellRenderer);
    }

    public void setCellEditor(TableCellEditor cellEditor) {
      assert false;
      super.setCellEditor(cellEditor);
    }

    public Object getHeaderValue() {
      return myAccessor.getColumnHeaderText();
    }

    @Nullable
    public TableCellRenderer getCellRenderer() {
      CollectionRenderer<?> renderer = myAccessor.getDataRenderer();
      return renderer != null ? new TableCellRendererAdapter(renderer) : null;
    }

    @Nullable
    public TableCellEditor getCellEditor() {
      CollectionEditor<?> editor = myAccessor.getDataEditor();
      return editor != null ? new TableCellEditorAdapter(editor) : null;
    }
  }


  private class DropHintInvalidator implements TableModelListener {
    public void tableChanged(TableModelEvent e) {
      TableDropHint hint = myDropHint;
      if (hint != null) {
        hint.setValid(false);
        setDropHint(null);
      }
    }
  }
}
