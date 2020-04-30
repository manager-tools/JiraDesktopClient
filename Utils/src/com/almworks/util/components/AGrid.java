package com.almworks.util.components;

import com.almworks.util.Env;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ListModelHolder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.ComponentCellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.AbstractDataProvider;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class AGrid<R, C, V> {
  private final ListModelHolder<R> myRowModel = ListModelHolder.create();
  private final ListModelHolder<C> myColumnModel = ListModelHolder.create();
  private final BasicScalarModel<AGridCellFunction<R, C, V>> myCellModel = BasicScalarModel.createWithValue(null, true);
  private final BasicScalarModel<CollectionRenderer<V>> myRenderer = BasicScalarModel.createWithValue(null, true);
  private final BasicScalarModel<CollectionRenderer<C>> myColumnHeaderRenderer =
    BasicScalarModel.createWithValue(null, true);
  private final BasicScalarModel<CollectionRenderer<R>> myRowHeaderRenderer =
    BasicScalarModel.createWithValue(null, true);
  private final MyTableModelAdapter<R, C, V> myModel;

  private final GridHeader<R> myGridRowHeader = new GridHeader<>(myRowModel, myRowHeaderRenderer, true);
  private final GridHeader<C> myGridColumnHeader = new GridHeader<>(myColumnModel, myColumnHeaderRenderer, false);
  private final GridTable myTable = new GridTable(myGridRowHeader, myGridColumnHeader);
  private final JScrollPane myScrollPane = new JScrollPane(myTable);

  private boolean myColumnWidthWithCells;
  private int myMaxColumns;

  private final Bottleneck myGridLayoutBottleneck = new Bottleneck(1000, ThreadGate.AWT, this::layoutGrid);

  @Nullable
  private AGridSelectionModel mySelectionModel;


  public AGrid() {
    myTable.setAutoCreateColumnsFromModel(true);
    myTable.setIntercellSpacing(new Dimension(1, 1));
    myTable.setShowGrid(true);
    myTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    myModel = new MyTableModelAdapter<>(myRowModel, myColumnModel, myCellModel);
    myTable.setModel(myModel);
    myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
    columnModel.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTable.setColumnModel(columnModel);

    myTable.setDefaultRenderer(Object.class, new MyCellRenderer<>(myRenderer));
    myTable.setCellSelectionEnabled(true);
    myModel.attach();
  }

  public void setColumnHeaderPaintsGrid(boolean value) {
    myGridColumnHeader.setPaintGrid(value);
  }

  public static <R, C, V> AGrid<R, C, V> create() {
    return new AGrid<>();
  }

  /**
   * Sets maximum width of row header, in characters
   */
  public void setRowHeaderMaxColumns(int columns) {
    if (columns < 1)
      return;
    myGridRowHeader.setMaxAcrossSize(columns * UIUtil.getColumnWidth(myTable));
  }

  public JComponent getComponent() {
    return myScrollPane;
  }

  public void setRowModel(Lifespan lifespan, AListModel<? extends R> rowModel) {
    if (rowModel == null) {
      myRowModel.setModel(AListModel.empty());
    } else {
      lifespan.add(myRowModel.setModel(rowModel));
    }
  }

  public void setColumnModel(Lifespan lifespan, AListModel<? extends C> columnModel) {
    if (columnModel == null) {
      myColumnModel.setModel(AListModel.empty());
    } else {
      lifespan.add(myColumnModel.setModel(columnModel));
    }
  }

  public AListModel<C> getColumnModel() {
    return myColumnModel;
  }

  public void setAxisModels(Lifespan lifespan, AListModel<? extends R> rowModel, AListModel<? extends C> columnModel) {
    myModel.setInhibitEvents(true);
    try {
      setRowModel(lifespan, rowModel);
      setColumnModel(lifespan, columnModel);
    } finally {
      myModel.setInhibitEvents(false);
    }
  }

  public void setCellModel(Lifespan lifespan, final AGridCellFunction<R, C, V> cellFunction) {
    myCellModel.setValue(cellFunction);
    if (cellFunction != null) {
      lifespan.add(new Detach() {
        protected void doDetach() {
          setCellModel(Lifespan.FOREVER, null);
        }
      });
    }
    myTable.repaint();
  }

  public void setCellRenderer(CollectionRenderer<V> renderer) {
    myRenderer.setValue(renderer);
    myTable.repaint();
  }

  public void setColumnHeaderRenderer(CollectionRenderer<C> renderer) {
    myColumnHeaderRenderer.setValue(renderer);
    myTable.repaint();
  }

  public void setRowHeaderRenderer(CollectionRenderer<R> renderer) {
    myRowHeaderRenderer.setValue(renderer);
    myTable.repaint();
  }

  public void setRowHeight(int rowHeight) {
    myTable.setRowHeight(rowHeight);
  }

  public void setEqualColumnWidthByHeaderRenderer(int maxColumns) {
    myColumnWidthWithCells = false;
    myMaxColumns = maxColumns;
//    myColumnModel.addAWTChangeListener(Lifespan.FOREVER, myGridLayoutBottleneck);
//    myGridLayoutBottleneck.request();
    layoutGrid();
  }

  public void setEqualColumnWidthByHeaderAndCellRenderer(int maxColumns) {
    myColumnWidthWithCells = true;
    myMaxColumns = maxColumns;
    myColumnModel.addAWTChangeListener(Lifespan.FOREVER, myGridLayoutBottleneck);
    myRowModel.addAWTChangeListener(Lifespan.FOREVER, myGridLayoutBottleneck);
    myGridLayoutBottleneck.request();
  }

  private void layoutGrid() {
    int width = getMaximumColumnHeaderWidth();
    if (myColumnWidthWithCells) {
      int cellWidth = getMaximumCellWidth() + 6;
      width = Math.max(width, cellWidth);
    }
    if (myMaxColumns > 0) {
      width = Math.min(width, UIUtil.getColumnWidth(myTable) * myMaxColumns);
    }
    if (width > 0) {
      updateColumnWidths(width);
    }
  }

  private int getMaximumCellWidth() {
    // todo optimize by passing max width here
    TableCellRenderer renderer = myTable.getDefaultRenderer(Object.class);
    TableModel model = myTable.getModel();
    int width = -1;
    for (int i = 0; i < myColumnModel.getSize(); i++) {
      for (int j = 0; j < myRowModel.getSize(); j++) {
        Component c = renderer.getTableCellRendererComponent(myTable, model.getValueAt(j, i), true, true, j, i);
        if (c != null) {
          Dimension sz = c.getPreferredSize();
          if (sz != null) {
            width = Math.max(width, sz.width);
          }
        }
      }
    }
    return width;
  }

  private int getMaximumColumnHeaderWidth() {
    CollectionRenderer<C> renderer = myColumnHeaderRenderer.getValue();
    if (renderer == null)
      return -1;
    int width = -1;
    for (int i = 0; i < myColumnModel.getSize(); i++) {
      C columnData = myColumnModel.getAt(i);
      GridHeader.GridHeaderCellState state = new GridHeader.GridHeaderCellState(i, myTable, false, null);
      JComponent c = renderer.getRendererComponent(state, columnData);
      Dimension pref = c.getPreferredSize();
      if (pref.width > width)
        width = pref.width;
    }
    return width;
  }

  private void updateColumnWidths(int width) {
    TableColumnModel model = myTable.getColumnModel();
    for (int i = 0; i < model.getColumnCount(); i++) {
      TableColumn column = model.getColumn(i);
      column.setPreferredWidth(width);
      column.setWidth(width);
    }
    myTable.repaint();
    myGridColumnHeader.repaint();
  }

  @SuppressWarnings("unused")
  public void setPopupMenu(MenuBuilder builder, final DataRole<Pair<R, C>> dataRole) {
    DataProvider.DATA_PROVIDER.putClientValue(myTable, new AbstractDataProvider(dataRole) {
      @Nullable
      public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
        if (role != dataRole)
          return null;
        int row = myTable.getSelectedRow();
        int column = myTable.getSelectedColumn();
        if (row == -1 || column == -1)
          return Collections15.emptyList();
        R r = myRowModel.getAt(row);
        C c = myColumnModel.getAt(column);
        //noinspection unchecked
        T value = (T) Pair.create(r, c);
        return Collections.singletonList(value);
      }
    });
    builder.addToComponent(Lifespan.FOREVER, myTable);
  }

  public void setTopLeftCorner(JComponent component) {
    myScrollPane.setCorner(JScrollPane.UPPER_LEADING_CORNER, component);
  }

  public Color getGridColor() {
    return myTable.getGridColor();
  }

  public Color getBackground() {
    return myTable.getBackground();
  }

  public void setBackground(Color color) {
    myTable.setBackground(color);
    myScrollPane.getViewport().setBackground(color);
    myGridRowHeader.setBackground(color);
    myGridColumnHeader.setBackground(color);
  }

  public Dimension getGridSpacing() {
    return myTable.getIntercellSpacing();
  }

  public JPanel createCornerPanel() {
    JPanel panel = new JPanel();
    panel.setBorder(
      new CompoundBorder(new BrokenLineBorder(getGridColor(), 1, BrokenLineBorder.SOUTH | BrokenLineBorder.EAST),
        new EmptyBorder(1, 2, 1, 2)));
    panel.setOpaque(true);
    panel.setBackground(getBackground());
    panel.setLayout(new BorderLayout(0, 0));
    return panel;
  }

  public AGridSelectionModel getSelectionModel() {
    if (mySelectionModel == null) {
      mySelectionModel = new AGridSelectionModel();
      final DefaultListSelectionModel rowSelection = new DefaultListSelectionModel();
      final DefaultListSelectionModel columnSelection = new DefaultListSelectionModel();
      myTable.setSelectionModel(rowSelection);
      myTable.getColumnModel().setSelectionModel(columnSelection);
      myTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
      ListSelectionListener listener = e -> {
        AGridSelectionModel sm = mySelectionModel;
        if (sm == null) {
          return;
        }
        int rowFrom = rowSelection.getMinSelectionIndex();
        int rowTo = rowSelection.getMaxSelectionIndex();
        int colFrom = columnSelection.getMinSelectionIndex();
        int colTo = columnSelection.getMaxSelectionIndex();
        int selectionMode =
          rowFrom <= rowTo && colFrom <= colTo && rowFrom >= 0 && colFrom >= 0 ? AGridSelectionModel.CELL_SELECTED :
            AGridSelectionModel.NOTHING_SELECTED;
        sm.update(selectionMode, rowFrom, rowTo, colFrom, colTo);
      };
      rowSelection.addListSelectionListener(listener);
      columnSelection.addListSelectionListener(listener);
    }
    return mySelectionModel;
  }


  private static class MyTableModelAdapter<R, C, V> extends AbstractTableModel {
    private final AListModel<R> myRowModel;
    private final AListModel<C> myColumnModel;
    private final ScalarModel<AGridCellFunction<R, C, V>> myCellModel;
    private boolean myInhibitEvents = false;

    public MyTableModelAdapter(AListModel<R> rowModel, AListModel<C> columnModel,
      ScalarModel<AGridCellFunction<R, C, V>> cellModel)
    {
      myRowModel = rowModel;
      myColumnModel = columnModel;
      myCellModel = cellModel;
    }

    public void attach() {
      myRowModel.addListener(new AListModel.Listener<R>() {
        public void onInsert(int index, int length) {
          if (!myInhibitEvents) {
            fireTableRowsInserted(index, index + length - 1);
          }
        }

        public void onRemove(int index, int length, AListModel.RemovedEvent<R> event) {
          if (!myInhibitEvents) {
            fireTableRowsDeleted(index, index + length - 1);
          }
        }

        public void onListRearranged(AListModel.AListEvent event) {
          update(event);
        }

        public void onItemsUpdated(AListModel.UpdateEvent event) {
          update(event);
        }

        private void update(AListModel.AListEvent event) {
          if (!myInhibitEvents) {
            fireTableRowsUpdated(event.getLowAffectedIndex(), event.getHighAffectedIndex());
          }
        }
      });

      myColumnModel.addListener(new AListModel.Listener<C>() {
        public void onInsert(int index, int length) {
          fire();
        }

        public void onRemove(int index, int length, AListModel.RemovedEvent<C> event) {
          fire();
        }

        public void onListRearranged(AListModel.AListEvent event) {
          fire();
        }

        public void onItemsUpdated(AListModel.UpdateEvent event) {
          fire();
        }

        private void fire() {
          if (!myInhibitEvents) {
            fireTableStructureChanged();
          }
        }
      });
    }

    public int getRowCount() {
      return myRowModel.getSize();
    }

    public int getColumnCount() {
      return myColumnModel.getSize();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      AGridCellFunction<R, C, V> cellFunction = myCellModel.getValue();
      if (cellFunction == null)
        return null;
      if (rowIndex < 0 || rowIndex >= myRowModel.getSize()) {
        return null;
      }
      if (columnIndex < 0 || columnIndex >= myColumnModel.getSize()) {
        return null;
      }
      R row = myRowModel.getAt(rowIndex);
      C column = myColumnModel.getAt(columnIndex);
      return cellFunction.getValue(row, column, rowIndex, columnIndex);
    }

    public void setInhibitEvents(boolean inhibitEvents) {
      boolean oldValue = myInhibitEvents;
      myInhibitEvents = inhibitEvents;
      if (oldValue != myInhibitEvents && !myInhibitEvents) {
        fireTableStructureChanged();
      }
    }
  }


  private static class MyCellRenderer<V> extends BaseRendererComponent implements TableCellRenderer {
    private final BasicScalarModel<CollectionRenderer<V>> myRendererModel;

    public MyCellRenderer(BasicScalarModel<CollectionRenderer<V>> rendererModel) {
      myRendererModel = rendererModel;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column)
    {
      CollectionRenderer<V> renderer = myRendererModel.getValue();
      if (renderer == null)
        return this;
      hasFocus = ListSpeedSearch.fixFocusedState(table, hasFocus, row, column);
      MyCellState state = new MyCellState(table, isSelected, hasFocus, row, column);
      ((GridTable) table).reportNextState(state);
      //noinspection unchecked
      return renderer.getRendererComponent(state, (V) value);
    }
  }


  private static class GridTable extends JTable implements MouseListener, MouseMotionListener, FocusListener {
    private final GridHeader myColumnHeader;
    private final GridHeader myRowHeader;

    private MouseEvent myLastMouseEvent = null;
    private int myLastMouseRow = -1;
    private int myLastMouseColumn = -1;

    private MyCellState myLastCellState;
    private String myPaintTooltip;
    private Cursor myPaintCursor;

    public GridTable(GridHeader rowHeader, GridHeader columnHeader) {
      int h = UIManager.getInt("Table.rowHeight");
      if (h > 0)
        setRowHeight(h);
      myRowHeader = rowHeader;
      myColumnHeader = columnHeader;
      addMouseListener(this);
      addMouseMotionListener(this);
      addFocusListener(this);
    }

    @Override
    public void updateUI() {
      super.updateUI();
      if (Aqua.isAqua()) {
        Color color = getGridColor();
        Color bg = getBackground();
        if (color == null || color.equals(bg)) {
          setGridColor(ColorUtil.between(bg,  getForeground(), 0.5F));
        }
      }
    }

    public void focusGained(FocusEvent e) {
      JTableAdapter.repaintSelection(this);
    }

    public void focusLost(FocusEvent e) {
      JTableAdapter.repaintSelection(this);
    }

    // copied from JTable
    protected void configureEnclosingScrollPane() {
      Container p = getParent();
      if (p instanceof JViewport) {
        Container gp = p.getParent();
        if (gp instanceof JScrollPane) {
          JScrollPane scrollPane = (JScrollPane) gp;
          // Make certain we are the viewPort's view and not, for
          // example, the rowHeaderView of the scrollPane -
          // an implementor of fixed columns might do this.
          JViewport viewport = scrollPane.getViewport();
          if (viewport == null || viewport.getView() != this) {
            return;
          }
          myColumnHeader.setTable(this);
          myRowHeader.setTable(this);
          scrollPane.setColumnHeaderView(myColumnHeader);
          scrollPane.setRowHeaderView(myRowHeader);
          Color bg = getBackground();
          scrollPane.getColumnHeader().setBackground(bg);
          scrollPane.getRowHeader().setBackground(bg);
          //  scrollPane.getViewport().setBackingStoreEnabled(true);
//          Border border = scrollPane.getBorder();
//          if (border == null || border instanceof UIResource) {
//            scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
//          }
        }
      }
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
      if (myLastMouseColumn >= 0 && myLastMouseRow >= 0) {
        getColumnModel().getSelectionModel().setSelectionInterval(myLastMouseColumn, myLastMouseColumn);
        getSelectionModel().setSelectionInterval(myLastMouseRow, myLastMouseRow);
      }
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
      int row = -1;
      int column = -1;
      myLastMouseEvent = e;
      if (myLastMouseEvent != null) {
        Point p = myLastMouseEvent.getPoint();
        row = rowAtPoint(p);
        column = columnAtPoint(p);
      }
      if (myLastMouseRow >= 0 && myLastMouseColumn >= 0 && (myLastMouseRow != row || myLastMouseColumn != column)) {
        repaint(getCellRect(myLastMouseRow, myLastMouseColumn, true));
      }
      if (row >= 0 && column >= 0) {
        repaint(getCellRect(row, column, true));
      }
      myLastMouseRow = row;
      myLastMouseColumn = column;
    }


    public void paint(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      myLastCellState = null;
      myPaintCursor = null;
      myPaintTooltip = null;

      super.paint(g);

      reportNextState(null);
      setToolTipText(myPaintTooltip);
      setCursor(myPaintCursor == null ? Cursor.getDefaultCursor() : myPaintCursor);
    }

    public void reportNextState(MyCellState state) {
      MyCellState paintedState = myLastCellState;
      myLastCellState = state;
      if (paintedState != null) {
        if (paintedState.myCursor != null)
          myPaintCursor = paintedState.myCursor;
        if (paintedState.myTooltip != null)
          myPaintTooltip = paintedState.myTooltip;
      }
    }
  }


  private static class MyCellState extends ComponentCellState {
    private static final boolean NOT_MAC = !Env.isMac();
    private static final Border FOCUSED_BORDER = UIManager.getBorder("Table.focusCellHighlightBorder");
    private static Border ourEmptyBorder;

    private final boolean mySelected;
    private final boolean myHasFocus;
    private final int myRow;
    private final int myColumn;
    private MouseEvent myLastMouseEvent;

    private String myTooltip;
    private Cursor myCursor;

    public MyCellState(JTable table, boolean selected, boolean hasFocus, int row, int column) {
      super(table);
      mySelected = selected;
      myHasFocus = hasFocus;
      myRow = row;
      myColumn = column;
      GridTable gridTable = ((GridTable) table);
      if (row == gridTable.myLastMouseRow && column == gridTable.myLastMouseColumn) {
        myLastMouseEvent = gridTable.myLastMouseEvent;
      }
    }

    protected JTable getComponent() {
      return (JTable) super.getComponent();
    }

    @Override
    public Color getBackground() {
      return getBackground(true);
    }

    public Color getBackground(boolean opaque) {
      if (isSelectionDrawn())
        return getComponent().getSelectionBackground();
      else
        return getDefaultBackground();
    }

    public Color getSelectionBackground() {
      return getComponent().getSelectionBackground();
    }

    @Nullable
    public Border getBorder() {
      return (NOT_MAC && myHasFocus) ? FOCUSED_BORDER : getEmptyBorder();
    }

    private static Border getEmptyBorder() {
      if (ourEmptyBorder == null) {
        ourEmptyBorder = new EmptyBorder(FOCUSED_BORDER.getBorderInsets(new JLabel()));
      }
      return ourEmptyBorder;
    }

    public int getCellColumn() {
      return myColumn;
    }

    public int getCellRow() {
      return myRow;
    }

    public int getComponentCellWidth() {
      TableColumnModel columnModel = getComponent().getColumnModel();
      return columnModel.getColumn(myColumn).getWidth() - columnModel.getColumnMargin();
    }

    public int getComponentCellHeight() {
      return getComponent().getRowHeight(myRow) - getComponent().getRowMargin();
    }

    @NotNull
    public Color getForeground() {
      return isSelectionDrawn() ? getComponent().getSelectionForeground() : getDefaultForeground();
    }

    private boolean isSelectionDrawn() {
      return mySelected &&  ListSpeedSearch.isFocusOwner(getComponent());
    }

    public boolean isExpanded() {
      return false;
    }

    public boolean isFocused() {
      return myHasFocus;
    }

    public boolean isLeaf() {
      return true;
    }

    public boolean isSelected() {
      return mySelected;
    }

    public MouseEvent getMouseEvent() {
      return myLastMouseEvent;
    }

    public void setFeedbackCursor(Cursor cursor) {
      myCursor = cursor;
    }

    public void setFeedbackTooltip(String tooltip) {
      myTooltip = tooltip;
    }
  }
}
