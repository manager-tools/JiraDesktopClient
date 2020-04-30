package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ListModelHolder;
import com.almworks.util.advmodel.ListSelectionModelAdapter;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.*;
import com.almworks.util.ui.actions.globals.GlobalData;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;

/**
 * @author : Dyoma
 */
public class ATable<T> extends ScrollableWrapper<JTableAdapter<T>>
  implements FlatCollectionComponent<T>, DndTarget, Highlightable
{
  private final SelectionAccessor<T> mySelection;
  private final CompositeTooltipProvider myTooltipProvider = new CompositeTooltipProvider();
  private final InputEventController myInputController = new InputEventController(getSwingComponent());

  private final Lifecycle mySwingLife = new Lifecycle(false);
  private final DndHelper<TableDropHint, JTableAdapter<?>> myDndHelper;


  public ATable() {
    this(new TableModelAdapter<T>(), TableDropHintProvider.DEFAULT);
  }

  public ATable(TableDropHintProvider dropHintProvider) {
    this(new TableModelAdapter<T>(), dropHintProvider);
  }


  ATable(TableModelAdapter<T> tableModel, TableDropHintProvider dropHintProvider) {
    super(new JTableAdapter<T>(tableModel));
    myDndHelper = new DndHelper<TableDropHint, JTableAdapter<?>>(dropHintProvider);
    TableColumnModel columnModel = new ATableColumnModel();
    getScrollable().setColumnModel(columnModel);
    ATableHeader tableHeader = new ATableHeader(columnModel);
    getScrollable().setTableHeader(tableHeader);
    getScrollable().getModel().init(columnModel);
    mySelection = new ListSelectionAccessor<T>(this);
    getScrollable().getModel().getTableModel().setSelection(mySelection);
    ListSelectionModelAdapter.createListening(getDataModel(), getSelectionModel(), false);
    ListSelectionModelAdapter.createListening(getColumnModel(), columnModel.getSelectionModel(), true);
    SelectionDataProvider.installTo(this);
    UIUtil.addPopupTriggerListener(this);
    getScrollable().addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getModifiers() != 0)
          return;
        JTableAdapter<T> jtable = getScrollable();
        if (jtable.getColumnCount() == 0)
          return;
        Rectangle visibleRect = getVisibleRect();
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_PAGE_DOWN) {
          int newFirstRow = getElementIndexAt(1, visibleRect.y + visibleRect.height - 2);
          if (getSelectionAccessor().isSelectedAt(newFirstRow))
            newFirstRow = getElementIndexAt(1, visibleRect.y + 2 * visibleRect.height - 2);
          getSelectionAccessor().setSelectedIndex(newFirstRow);
          jtable.scrollRectToVisible(jtable.getCellRect(newFirstRow, -1, false));
        } else if (keyCode == KeyEvent.VK_PAGE_UP) {
          int newFirstRow = getElementIndexAt(1, visibleRect.y + 2);
          if (getSelectionAccessor().isSelectedAt(newFirstRow))
            newFirstRow = Math.max(0, getElementIndexAt(1, visibleRect.y - visibleRect.height + 2));
          getSelectionAccessor().setSelectedIndex(newFirstRow);
          scrollSelectionToView();
        }
      }
    });
    setupFocusedRowUpdateListener();
  }

  public void setStriped(boolean striped) {
    getScrollable().setStriped(striped);
  }

  public boolean isStriped() {
    return getScrollable().isStriped();
  }

  public Color getBackground() {
    return getScrollable().getBackground();
  }

  public Color getStripeBackground() {
    return getScrollable().getStripeBackground();
  }

  public int getRowHeight() {
    return getScrollable().getRowHeight();
  }

  public void setColumnLinesPainted(boolean painted) {
    getScrollable().setColumnLinesPainted(painted);
  }

  public void setColumnBackgroundsPainted(boolean painted) {
    getScrollable().setColumnBackgroundsPainted(painted);
  }

  public void addDoubleClickListener(Lifespan life, final CollectionCommandListener<T> listener) {
    if (life.isEnded())
      return;
    final JTableAdapter table = (JTableAdapter) getSwingComponent();
    final MouseAdapter mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
          Point point = e.getPoint();
          int row = table.rowAtPoint(point);
          if (row >= 0 && row < table.getModel().getRowCount()) {
            T element = getCollectionModel().getAt(row);
            listener.onCollectionCommand(ATable.this, row, element);
          }
        }
      }
    };
    table.addMouseListener(mouseListener);
    life.add(new Detach() {
      protected void doDetach() {
        table.removeMouseListener(mouseListener);
      }
    });
  }

  private void setupFocusedRowUpdateListener() {
    getScrollable().addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        repaintFocusedRow();
      }

      public void focusLost(FocusEvent e) {
        repaintFocusedRow();
      }

      private void repaintFocusedRow() {
        JTable table = getScrollable();
        int index = getSelectionModel().getLeadSelectionIndex();
        if (index < 0)
          return;
        if (index >= table.getRowCount())
          return;
        table.repaint(getElementRect(index));
      }
    });
  }

  public InputEventController getInputController() {
    return myInputController;
  }

  public Detach setDataModel(AListModel<? extends T> dataModel) {
    return getScrollable().getModel().setData(dataModel);
  }

  public void setColumnModel(AListModel<? extends TableColumnAccessor<? super T, ?>> columns) {
    getScrollable().getModel().setColumns(columns);
  }

  public TableColumnAccessor<?, ?> getColumnAccessorAtVisual(int index) {
    return getScrollable().getColumnAccessorAtVisual(index);
  }

  public ListSelectionModel getSelectionModel() {
    return getScrollable().getSelectionModel();
  }

  public AListModel<? extends T> getCollectionModel() {
    return getDataModel();
  }

  public Detach setCollectionModel(AListModel<? extends T> model) {
    return setDataModel(model);
  }

  public int getElementIndexAt(int x, int y) {
    int index = getScrollable().rowAtPoint(new Point(0, y));
    if (index == -1) {
      int rowCount = getScrollable().getRowCount();
      if (rowCount == 0 || getScrollable().getColumnCount() == 0)
        return -1;
      Rectangle cellRect = getScrollable().getCellRect(rowCount - 1, 0, true);
      if (y > cellRect.y)
        return rowCount - 1;
    }
    return index;
  }

  public T getElementAt(Point point) {
    return AComponentUtil.getElementAtPoint(this, point);
  }

  public int getScrollingElementAt(int x, int y) {
    return getElementIndexAt(x, y);
  }

  @NotNull
  public Rectangle getElementRect(int elementIndex) {
    Rectangle firstCell = getScrollable().getCellRect(elementIndex, 0, true);
    Rectangle lastCell = getScrollable().getCellRect(elementIndex, getScrollable().getColumnCount() - 1, true);
    return new Rectangle(firstCell.x, firstCell.y, lastCell.x - firstCell.x + lastCell.width, firstCell.height);
  }

  @NotNull
  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelection;
  }

  public JComponent toComponent() {
    return this;
  }

  public void scrollSelectionToView() {
    int index = getSelectionAccessor().getSelectedIndex();
    if (index == -1)
      return;
    UIUtil.ensureRectVisiblePartially(this, getCellRect(index));
  }

  public Rectangle getCellRect(int index) {
    return getScrollable().getCellRect(index, -1, true);
  }

  public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
    return getScrollable().getCellRect(row, column, includeSpacing);
  }

  public void scrollSelectionToViewTop() {
    int index = getSelectionAccessor().getSelectedIndex();
    if (index == -1)
      return;
    Container parent = getParent();
    if (!(parent instanceof JViewport))
      return;
    ((JViewport) parent).setViewPosition(getCellRect(index).getLocation());
  }

  public void setDataRoles(DataRole... roles) {
    SelectionDataProvider.setRoles(this, roles);
  }

  public void addGlobalRoles(DataRole<?>... roles) {
    SelectionDataProvider.addRoles(this, roles);
    GlobalData.KEY.addClientValue(this.toComponent(), roles);
  }

  private TableModelAdapter<T> getModelAdapter() {
    return getScrollable().getModel();
  }

  public AListModel<TableColumnAccessor<? super T, ?>> getColumnModel() {
    return getModelAdapter().getColumns();
  }

//  public ListModelHolder<TableColumnAccessor<T, ?>> getColumnModelHolder() {
//    return (ListModelHolder<TableColumnAccessor<T, ?>>) getModelAdapter().getColumns();
//  }

  public AListModel<? extends T> getDataModel() {
    return getDataModelHolder();
  }

  public JTableHeader getSwingHeader() {
    return getScrollable().getTableHeader();
  }

  public int convertColumnIndexToModel(int column) {
    return getScrollable().convertColumnIndexToModel(column);
  }

  public TableColumnModel getTableColumnModel() {
    return getScrollable().getColumnModel();
  }

  public ListModelHolder<? extends T> getDataModelHolder() {
    return (ListModelHolder<? extends T>) getModelAdapter().getData();
  }

  public ATableModel<T> getTableModel() {
    return getModelAdapter().getTableModel();
  }

  public static <T> ATable<T> createInscrollPane(JScrollPane scrollPane) {
    ATable<T> table = new ATable<T>();
    table.wrapWithScrollPane(scrollPane);
    return table;
  }

  public static <T> ATable<T> create() {
    return new ATable<T>();
  }

  public static <T> ATable<T> create(TableDropHintProvider dropHintProvider) {
    return new ATable<T>(dropHintProvider);
  }

  public JScrollPane wrapWithScrollPane(JScrollPane scrollPane) {
    scrollPane.setViewportView(this);
    scrollPane.setColumnHeaderView(getSwingHeader());
    return scrollPane;
  }

  public JScrollPane wrapWithScrollPane() {
    JScrollPane scrollPane = new JScrollPane();
    wrapWithScrollPane(scrollPane);
    return scrollPane;
  }

  public void setRowHeight(int index, int height) {
    getScrollable().setRowHeight(index, height);
  }

  public void setRowHeight(int height) {
    getScrollable().setRowHeight(height);
  }

  public void setRowHeightByRenderer(boolean rowHeightByRenderer) {
    getScrollable().setRowHeightByRenderer(rowHeightByRenderer);
  }

  public void setDefaultFocusTraversalKeys(boolean isDefault) {
    getScrollable().setDefaultFocusTraversalKeys(isDefault);
  }

  public void resizeColumns() {
    getScrollable().resizeColumns();
  }

  public void resizeColumn(TableColumnAccessor<T, Comparable> columnAccessor) {
    getScrollable().resizeColumn(columnAccessor);
  }

  public int getColumnWidth(int index) {
    TableColumnModel columnModel = getScrollable().getColumnModel();
    if (index < 0 || index >= columnModel.getColumnCount())
      return 0;
    return columnModel.getColumn(index).getWidth();
  }

  public void setShowGrid(boolean horizontal, boolean vertical) {
    getScrollable().setShowHorizontalLines(horizontal);
    getScrollable().setShowVerticalLines(vertical);
  }

  public void setGridHidden() {
    JTable jtable = (JTable) getSwingComponent();
    jtable.setShowVerticalLines(false);
    jtable.setShowHorizontalLines(false);
    jtable.setIntercellSpacing(new Dimension(0, 0));
  }


  public boolean isSelectedAtPoint(Point p) {
    JTableAdapter<T> table = getScrollable();
    int row = table.rowAtPoint(p);
    return row >= 0 && table.isRowSelected(row);
  }

  public void setIntercellSpacing(Dimension dimension) {
    getScrollable().setIntercellSpacing(dimension);
  }

  public TableCellEditor getSwingCellEditor() {
    return getScrollable().getCellEditor();
  }

  public boolean isOrderChangeAllowed(int index) {
    AListModel<? extends TableColumnAccessor<?, ?>> columns = getColumnModel();
    if (index >= 0 && index < columns.getSize()) {
      TableColumnAccessor<?, ?> accessor = columns.getAt(index);
      return accessor.isOrderChangeAllowed();
    } else {
      return true;
    }
  }


  private int getColumnIndex(TableColumn column) {
    TableColumnModel columnModel = getTableColumnModel();
    for (int i = 0; i < columnModel.getColumnCount(); i++) {
      if (column == columnModel.getColumn(i))
        return i;
    }
    return -1;
  }

  public Color getGridColor() {
    return getScrollable().getGridColor();
  }

  TableTooltipProvider getTooltipProvider() {
    return myTooltipProvider;
  }

  public void addTooltipProvider(TableTooltipProvider provider) {
    myTooltipProvider.addProvider(provider);
  }

  public void removeTooltipProvider(TableTooltipProvider provider) {
    myTooltipProvider.removeProvider(provider);
  }

  public void setTransfer(ContextTransfer transfer) {
    super.setTransfer(transfer);
    if (!GraphicsEnvironment.isHeadless())
      getScrollable().setDragEnabled(true);
    if (isDisplayable()) {
      registerInDndManager();
    }
  }

  public void addNotify() {
    super.addNotify();
    if (mySwingLife.cycleStart()) {
      if (getTransfer() != null) {
        registerInDndManager();
      }
    }
  }

  public void removeNotify() {
    mySwingLife.cycleEnd();
    super.removeNotify();
  }

  private void registerInDndManager() {
    DndManager dndManager = DndManager.require();
    Lifespan lifespan = mySwingLife.lifespan();
    dndManager.registerSource(lifespan, this);
    dndManager.registerTarget(lifespan, this);
  }

  public JComponent getTargetComponent() {
    return getSwingComponent();
  }

  public void dragNotify(DndEvent event) {
    myDndHelper.dragNotify(event, getTransfer(), (JTableAdapter) getSwingComponent());
  }

  public void forcePreferredColumnWidth(int columnIndex) {
    getScrollable().forcePreferredColumnWidth(columnIndex);
  }

  public void forcePreferredColumnWidths() {
    getScrollable().forcePreferredColumnWidths();
  }

  public static TableColumnAccessor<?, ?> getColumnAccessor(TableColumn swingColumn) {
    Object identifier = swingColumn.getIdentifier();
    if (identifier instanceof TableColumnAccessor)
      return (TableColumnAccessor<?, ?>) identifier;
    else
      return null;
  }

  public static AnAction createForceColumnWidthAction(ATable<?> table) {
    return new ForceColumnWidth(table);
  }

  public static AnAction createAutoAdjustAllColumnsAction(ATable<?> table) {
    return new AutoAdjustAllColumns(table);
  }

  public static void addHeaderActions(ATable<?> table, AnAction... actions) {
    JTableHeader tableHeader = table.getSwingHeader();
    new MenuBuilder().addActions(actions)
      .addAction(ATable.createForceColumnWidthAction(table))
      .addAction(ATable.createAutoAdjustAllColumnsAction(table))
      .addToComponent(Lifespan.FOREVER, tableHeader);
  }

  public void setSelectionWhenDndDone(int[] indeces) {
    getScrollable().setSelectionWhenDndDone(indeces);
  }

  public void setHighlightPattern(Pattern highlightPattern) {
    getScrollable().setHighlightPattern(highlightPattern);
  }

  public CellState getCellState(int row, int column) {
    return getScrollable().getCellState(row, column);
  }

  private final class ATableHeader extends JTableHeader {
    private int myLastDragX = 0;
    private boolean myAutoResizeAllowed = false;

    public ATableHeader(TableColumnModel cm) {
      super(cm);
      addMouseListener(new ColumnAutoSizer());
    }

    public void setCursor(Cursor cursor) {
      myAutoResizeAllowed = !Util.equals(cursor, Cursor.getDefaultCursor());
      super.setCursor(cursor);
    }

    public void setDraggedColumn(TableColumn column) {
      if (column != null) {
        if (!isOrderChangeAllowed(column.getModelIndex())) {
          // ignore!
          return;
        }
      }
      super.setDraggedColumn(column);
    }

    protected void processMouseEvent(MouseEvent e) {
      if (e.getID() == MouseEvent.MOUSE_PRESSED) {
        myLastDragX = e.getX();
      }
      super.processMouseEvent(e);
    }

    protected void processMouseMotionEvent(MouseEvent e) {
      if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
        int mouseX = e.getX();
        int dragDirection = mouseX < myLastDragX ? -1 : 1;
        myLastDragX = mouseX;
        TableColumn column = getDraggedColumn();
        if (column != null) {
          int columnIndex = getColumnIndex(column);
          int distance = getDraggedDistance();
          int reorderDirection = distance == 0 ? dragDirection : (distance < 0 ? -1 : 1);
          int newColumnIndex = columnIndex + reorderDirection;
          TableColumnModel columnModel = getColumnModel();
          if (newColumnIndex < 0 || newColumnIndex >= columnModel.getColumnCount())
            newColumnIndex = columnIndex;
          int min = Math.min(columnIndex, newColumnIndex);
          int max = Math.max(columnIndex, newColumnIndex);
          for (int i = min; i <= max; i++) {
            if (!isOrderChangeAllowed(i)) {
              // ignore event
              e.consume();
              setDraggedDistance(0);
              columnModel.moveColumn(columnIndex, columnIndex);
              return;
            }
          }
        }
      }
      super.processMouseMotionEvent(e);
    }

    private class ColumnAutoSizer extends MouseAdapter {
      public void mouseClicked(MouseEvent e) {
        if (myAutoResizeAllowed && e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON1) {
          Point p = e.getPoint();
          int column = columnAtPoint(p);
          if (column >= 0) {
            Rectangle r = getHeaderRect(column);
            int midPoint = r.x + r.width / 2;
            if (getComponentOrientation().isLeftToRight()) {
              column = (p.x < midPoint) ? column - 1 : column;
            } else {
              column = (p.x < midPoint) ? column : column - 1;
            }
            if (column >= 0) {
              getScrollable().forcePreferredColumnWidth(column);
            }
          }
        }
      }
    }
  }


  private final class ATableColumnModel extends DefaultTableColumnModel {
    public void moveColumn(int columnIndex, int newIndex) {
      if (columnIndex != newIndex) {
        int min = Math.min(columnIndex, newIndex);
        int max = Math.max(columnIndex, newIndex);
        for (int i = min; i <= max; i++) {
          TableColumn column = getColumn(i);
          if (!isOrderChangeAllowed(column.getModelIndex())) {
            // ignore!
            JTableHeader header = getScrollable().getTableHeader();
            TableColumn draggedColumn = header.getDraggedColumn();
            if (draggedColumn != null) {
              header.setDraggedDistance(0);
              TableColumnModel cm = header.getColumnModel();
              for (int j = 0; j < cm.getColumnCount(); j++) {
                if (cm.getColumn(j) == draggedColumn) {
                  super.moveColumn(j, j);
                  break;
                }
              }
              header.setDraggedColumn(null);
            }
            return;
          }
        }
      }
      super.moveColumn(columnIndex, newIndex);
    }
  }


  public static abstract class HeaderAction extends SimpleAction {
    public HeaderAction(@Nullable String name, @Nullable Icon icon) {
      super(name, icon);
    }

    protected int getColumn(ActionContext context) throws CantPerformException {
      MouseEvent event = getMouseEvent(context);
      if (event == null) return -1;
      Point point = event.getPoint();
      JTableHeader header = getTable().getSwingHeader();
      int column = header.columnAtPoint(point);
      TableColumnModel model = header.getColumnModel();
      return column >= 0 && column < model.getColumnCount() ? column : -1;
    }

    @Nullable
    protected MouseEvent getMouseEvent(ActionContext context) {
      InputEvent event = null;
      try {
        event = context.getSourceObject(MenuBuilder.POPUP_INPUT_EVENT);
      } catch (CantPerformException e) {
        return null;
      }
      return event instanceof MouseEvent ? (MouseEvent) event : null;
    }

    protected abstract ATable<?> getTable();

    protected TableColumn getColumn(int index) {
      return getTable().getSwingHeader().getColumnModel().getColumn(index);
    }

    protected AListModel<? extends TableColumnAccessor<?, ?>> getColumnModel() {
      return getTable().getColumnModel();
    }

    @Nullable
    protected TableColumnAccessor<?, ?> getColumnAccessor(ActionContext context) throws CantPerformException {
      int mi = getColumnModelIndex(context);
      if (mi < 0) return null;
      AListModel<? extends TableColumnAccessor<?, ?>> aModel = getColumnModel();
      return aModel.getAt(mi);
    }

    protected int getColumnModelIndex(ActionContext context) throws CantPerformException {
      int column = getColumn(context);
      if (column < 0) return -1;
      int mi = getColumn(column).getModelIndex();
      AListModel<? extends TableColumnAccessor<?, ?>> aModel = getColumnModel();
      return mi < 0 || mi >= aModel.getSize() ? -1 : mi;
    }
  }

  public static class ForceColumnWidth extends HeaderAction {
    private final ATable<?> myTable;

    public ForceColumnWidth(ATable<?> table) {
      super(null, null);
      myTable = table;
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    }

    protected ATable<?> getTable() {
      return myTable;
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      TableColumnAccessor<?, ?> column = getColumnAccessor(context);
      if (column != null && column.getSizePolicy() != ColumnSizePolicy.FIXED) {
        context.setEnabled(EnableState.ENABLED);
        context.putPresentationProperty(PresentationKey.NAME, "Auto-Adjust \"" + column.getName() + "\"");
      }
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      int column = getColumn(context);
      if (column >= 0) {
        getTable().forcePreferredColumnWidth(column);
      }
    }
  }


  public static class AutoAdjustAllColumns extends EnabledAction {
    private final ATable<?> myTable;

    public AutoAdjustAllColumns(ATable<?> table) {
      super("Auto-Adjust All Columns");
      myTable = table;
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      myTable.forcePreferredColumnWidths();
    }
  }
}