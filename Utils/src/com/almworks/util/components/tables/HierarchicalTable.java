package com.almworks.util.components.tables;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.CachingConvertingListDecorator;
import com.almworks.util.advmodel.ConvertingListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.CollectionSortPolicy;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public class HierarchicalTable<T> implements FlatCollectionComponent<T> {
  private static final Set<Object> ourTreeActions = createTreeActions();
  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final ComponentProperty<HierarchicalTable> HTABLE = ComponentProperty.createProperty("controller");

  private final ATable<T> myComponent;
  private final JTreeRenderer myTreeColumnRenderer;
  private final SortingTableHeaderController<T> myHeaderController;
  private final TreeInputHandler myTreeInputHandler = new TreeInputHandler(ourTreeActions, new MyTreeInputCallback());
  private final FireEventSupport<SortingListener> mySortingListeners = FireEventSupport.create(SortingListener.class);
  private final SelectionAccessor<T> mySelectionAccessor;

  @Nullable
  private TreeToListAdapter<T> myListAdapter;

  private CollectionSortPolicy mySortPolicy = CollectionSortPolicy.DEFAULT;
  private final HierarchicalTable<T>.MyATableState myTableState;
  private boolean myShowRootHanders = false;
  private int myTreeColumnIndex = 0;

  private final Lifecycle myColumnModelCycle = new Lifecycle();
  private final Lifecycle myRootLife = new Lifecycle();

  public Pattern getHighlightPattern() {
    return myHighlightPattern;
  }

  public void setHighlightPattern(Pattern highlightPattern) {
    myHighlightPattern = highlightPattern;
    myComponent.setHighlightPattern(highlightPattern);
  }

  private Pattern myHighlightPattern;


  public HierarchicalTable(ATable<T> component) {
    assert HTABLE.getClientValue(component) == null : HTABLE.getClientValue(component);
    HTABLE.putClientValue(component, this);
    myComponent = component;
    mySelectionAccessor = new Deduplicator<T>(myComponent.getSelectionAccessor());
    assert ActionContext.ACTUAL_COMPONENT.getClientValue(component) != null : component;
    myTreeColumnRenderer = new JTreeRenderer(component);
    int rowHeight = ((JTable) component.getSwingComponent()).getRowHeight();
    if (rowHeight > 0)
      myTreeColumnRenderer.setRowHeight(rowHeight);
    setRootVisible(true);
    myComponent.getSwingComponent().addMouseListener(new MyMouseAdapter());
    myComponent.getSwingComponent().addKeyListener(new MyKeyListener());
    myTableState = new MyATableState();
    myHeaderController = new SortingTableHeaderController<T>(myComponent.getSwingHeader(), myTableState);
    addSortingListener(myHeaderController.getSortingListener());
  }

  public void setStriped(boolean striped) {
    myComponent.setStriped(striped);
  }

  public void setColumnBackgroundsPainted(boolean painted) {
    myComponent.setColumnBackgroundsPainted(painted);
  }

  public void setColumnLinesPainted(boolean painted) {
    myComponent.setColumnLinesPainted(painted);
  }

  public Detach addSortingListener(SortingListener listener) {
    return mySortingListeners.addAWTListener(listener);
  }

  @NotNull
  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelectionAccessor;
  }

  public JComponent toComponent() {
    return myComponent.toComponent();
  }

  public JComponent getSwingComponent() {
    return myComponent.getSwingComponent();
  }

  public void scrollSelectionToView() {
    myComponent.scrollSelectionToView();
  }

  public void setDataRoles(DataRole... roles) {
    myComponent.setDataRoles(roles);
  }

  public void setTransfer(ContextTransfer transfer) {
    myComponent.setTransfer(transfer);
  }

  public void addGlobalRoles(DataRole<?>... roles) {
    myComponent.addGlobalRoles(roles);
  }

  public ListSelectionModel getSelectionModel() {
    return myComponent.getSelectionModel();
  }

  public AListModel<? extends T> getCollectionModel() {
    return myComponent.getCollectionModel();
  }

  public Detach setCollectionModel(AListModel<? extends T> model) {
    throw new UnsupportedOperationException();
  }

  public int getElementIndexAt(int x, int y) {
    return myComponent.getElementIndexAt(x, y);
  }

  public T getElementAt(Point point) {
    return AComponentUtil.getElementAtPoint(this, point);
  }

  public int getScrollingElementAt(int x, int y) {
    return myComponent.getScrollingElementAt(x, y);
  }

  public Rectangle getElementRect(int elementIndex) {
    return myComponent.getElementRect(elementIndex);
  }

  public int getTreeColumnIndex() {
    return myTreeColumnIndex;
  }

  public void setTreeColumnIndex(int treeColumnIndex) {
    if (treeColumnIndex == myTreeColumnIndex)
      return;
    myTreeColumnIndex = treeColumnIndex;
    myComponent.repaint();
  }

  public boolean isShowRootHanders() {
    return myShowRootHanders;
  }

  public void setShowRootHanders(boolean showRootHanders) {
    if (myShowRootHanders == showRootHanders)
      return;
    myShowRootHanders = showRootHanders;
    myComponent.repaint();
  }

  public SortingTableHeaderController<T> getHeaderController() {
    return myHeaderController;
  }

  public ATable<T> getTable() {
    return myComponent;
  }

  @Nullable
  public TreeToListAdapter<T> getTreeAdapter() {
    return myListAdapter;
  }

  public void setColumnModel(AListModel<TableColumnAccessor<T, ?>> columns) {
    myColumnModelCycle.cycle();
    Convertor<TableColumnAccessor<T, ?>, TableColumnAccessor<T, ?>> convertor =
      new Convertor<TableColumnAccessor<T, ?>, TableColumnAccessor<T, ?>>() {
        public TableColumnAccessor<T, ?> convert(TableColumnAccessor<T, ?> value) {
          return new TreeTableColumn(value);
        }
      };
    ConvertingListDecorator<TableColumnAccessor<T, ?>, TableColumnAccessor<T, ?>> convertedColumns =
      CachingConvertingListDecorator.createCaching(myColumnModelCycle.lifespan(), columns, convertor);
    myComponent.setColumnModel(convertedColumns);
  }

  public void setGridHidden() {
    JTable jtable = (JTable) getSwingComponent();
    jtable.setShowVerticalLines(false);
    jtable.setShowHorizontalLines(false);
    jtable.setIntercellSpacing(new Dimension(0, 0));
  }

  public void setRoot(ATreeNode<T> rootNode) {
    myRootLife.cycle();
    DefaultTreeModel model = new TreeModelBridge(null).becomeRoot();
    rootNode.replaceRoot(model);
    TreeToListAdapter<T> listAdapter = TreeToListAdapter.create(myRootLife.lifespan(), model);
    listAdapter.setRootVisible(myTreeColumnRenderer.isRootVisible());
    Comparator<? super T> comparator = myListAdapter != null ? myListAdapter.getComparator() : null;
    myListAdapter = listAdapter;
    if (comparator != null)
      listAdapter.sort(comparator, mySortPolicy);
    myComponent.setDataModel(listAdapter.getListModel());
  }

  public void expandAll() {
    TreeToListAdapter<T> adapter = myListAdapter;
    if (adapter != null) {
      adapter.expandAll();
    }
  }

  public void clearRoot() {
    setRoot(new TreeModelBridge<T>(null));
  }

  public void setRootVisible(boolean visible) {
    myTreeInputHandler.setRootVisible(visible);
    myTreeColumnRenderer.setRootVisible(visible);
    if (myListAdapter != null)
      myListAdapter.setRootVisible(visible);
  }

  public boolean isRootVisible() {
    return myTreeColumnRenderer.isRootVisible();
  }

  public void setResetTreeColumnSizeCache(boolean reset) {
    myTreeColumnRenderer.setResetSizeCache(reset);
  }

  @Nullable
  private TreeNodeState getNodeState(int index) {
    if (index < 0)
      return null;
    TreeToListAdapter<T> listAdapter = myListAdapter;
    if (listAdapter == null) {
      assert false : this;
      return null;
    }
    if (index < 0 || index >= listAdapter.getRowCount()) {
      Log.warn("bad index " + listAdapter.getRowCount() + " " + index + " " + this);
      return null;
    }
    JTable table = (JTable) myComponent.getSwingComponent();
    TableColumnModel columnModel = table.getColumnModel();
    if (columnModel.getColumnCount() < 1) {
      assert false : this;
      return null;
    }
    int width = columnModel.getColumn(0).getWidth();
    Rectangle rect = myComponent.getElementRect(index);
    rect.width = width;
    TreePath path = listAdapter.getPathAt(index);
    boolean[] last = new boolean[path.getPathCount()];
    last[0] = true;
    int ancestorIndex = index;
    for (int level = last.length - 1; level > 0; level--) {
      last[level] = listAdapter.isLastChild(ancestorIndex);
      if (level > 1) {
        ancestorIndex = listAdapter.getParentIndex(ancestorIndex);
        assert
          ancestorIndex >= 0 || (!listAdapter.isRootVisible() && level == 1) :
          index + " " + ancestorIndex + " " + level;
      }
    }
    boolean topMiddle = !last[last.length - 1] && !listAdapter.isRootVisible() && path.getPathCount() == 2 && index > 0;
    boolean topRoot = index == 0 && !isRootVisible() && path.getPathCount() == 2;
    return new TreeNodeState(path, rect, listAdapter.getFirstChild(index), listAdapter.isExpanded(index),
      table.getSelectionModel().getLeadSelectionIndex() == index, last, myShowRootHanders, topMiddle, topRoot);
  }

  private static Set<Object> createTreeActions() {
    Set<Object> actions = Collections15.hashSet();
    actions.add("selectChild");
    actions.add("selectParent");
    return Collections.unmodifiableSet(actions);
  }

  public JTableHeader getSwingHeader() {
    return myComponent.getSwingHeader();
  }

  public void setSortPolicy(CollectionSortPolicy sortPolicy) {
    mySortPolicy = sortPolicy == null ? CollectionSortPolicy.DEFAULT : sortPolicy;
  }

  public void sortBy(int column, boolean reverse) {
    AListModel<TableColumnAccessor<? super T, ?>> columns = getTable().getColumnModel();
    if (column >= 0 && column < columns.getSize()) {
      myTableState.sortBy(columns.getAt(column), reverse);
    }
  }

  public void sortBy(TableColumnAccessor<T, ?> column, boolean reverse) {
    myTableState.sortBy(column, reverse);
  }

  public static <T> HierarchicalTable<T> getHierarchicalTable(ATable<T> table) {
    HierarchicalTable<T> controller = HTABLE.getClientValue(table);
    assert controller != null;
    return controller;
  }

  public void setRowHeight(int rowHeight) {
    myComponent.setRowHeight(rowHeight);
  }

  private class TreeTableColumn extends TableColumnAccessor.DelegatingColumn<T, Object> {
    private final MyCollectionRenderer myRenderer = new MyCollectionRenderer();
    private final DelegatingTooltipProvider myTooltipProvider = new DelegatingTooltipProvider();
    private boolean myGettingColumnPreferredWidth = false;

    public TreeTableColumn(TableColumnAccessor<T, ?> column) {
      //noinspection unchecked
      super((TableColumnAccessor<? super T, Object>) column);
    }

    protected TableColumnAccessor<T, Object> getDelegate() {
      //noinspection unchecked
      return (TableColumnAccessor<T, Object>) super.getDelegate();
    }

    public CollectionRenderer<T> getDataRenderer() {
      return myRenderer;
    }

    public int getPreferredWidth(JTable table, ATableModel<T> tableModel, ColumnAccessor<T> renderingAccessor,
      int columnIndex)
    {
      myGettingColumnPreferredWidth = true;
      try {
        return getDelegate().getPreferredWidth(table, tableModel, renderingAccessor, columnIndex);
      } finally {
        myGettingColumnPreferredWidth = false;
      }
    }

    @Override
    public ColumnTooltipProvider<T> getTooltipProvider() {
      return myTooltipProvider;
    }

    private class MyCollectionRenderer implements CollectionRenderer<T>, RowBorderBounding {
      private boolean myLastTreeWrapped = false;

      public JComponent getRendererComponent(CellState state, T item) {
        myLastTreeWrapped = false;
        CollectionRenderer<? super T> dataRenderer = getDelegate().getDataRenderer();
        if (dataRenderer == null) {
          assert false : getDelegate();
          return new JLabel();
        }
        if (state.getCellColumn() != myTreeColumnIndex || myListAdapter == null)
          return dataRenderer.getRendererComponent(state, item);
        int index = state.getCellRow();
        if (index < 0) {
          // ?
          return dataRenderer.getRendererComponent(state, item);
        }
        TreeNodeState nodeState = getNodeState(index);
        if (nodeState == null) {
          Log.warn("no node state for " + this + " " + index);
          return dataRenderer.getRendererComponent(state, item);
        }
        myLastTreeWrapped = true;
        state.setHighlightPattern(myHighlightPattern);
        myTreeColumnRenderer.preparePath(nodeState, state, dataRenderer, myGettingColumnPreferredWidth);
        return myTreeColumnRenderer;
      }

      public int getRowBorderX(JTable table, Graphics g) {
        int x = getDelegateRowBorderX(table, g);
        if (!myLastTreeWrapped || x < 0)
          return x;
        return myTreeColumnRenderer.getCurrentCellBounds().x + x;
      }

      private int getDelegateRowBorderX(JTable table, Graphics g) {
        CollectionRenderer<? super T> dataRenderer = getDelegate().getDataRenderer();
        if (dataRenderer instanceof RowBorderBounding) {
          return ((RowBorderBounding) dataRenderer).getRowBorderX(table, g);
        } else {
          return 0;
        }
      }

      int getTreeInCellWidth(CellState state) {
        TreeNodeState nodeState = getNodeState(state.getCellRow());
        CollectionRenderer<? super T> renderer = getDelegate().getDataRenderer();
        if (renderer == null) return 0;
        myTreeColumnRenderer.preparePath(nodeState, state, renderer, false);
        return myTreeColumnRenderer.getCurrentCellBounds().x;
      }
    }

    private class DelegatingTooltipProvider implements ColumnTooltipProvider<T> {
      @Override
      public String getTooltip(CellState cellState, T element, Point cellPoint, Rectangle cellRect) {
        // translate the position and shrink the rectangle
        ColumnTooltipProvider<T> provider = getDelegate().getTooltipProvider();
        if (provider == null) return null;
        Point shiftedPoint = cellPoint;
        Rectangle shrunkRect = cellRect;
        if (cellState.getCellColumn() == myTreeColumnIndex) {
          int shift = myRenderer.getTreeInCellWidth(cellState);
          shiftedPoint = new Point(cellPoint.x - shift, cellPoint.y);
          shrunkRect = new Rectangle(cellRect.x + shift, cellRect.y, cellRect.width - shift, cellRect.height);
          if (shrunkRect.getWidth() <= 0) return null;
        }
        return provider.getTooltip(cellState, element, shiftedPoint, shrunkRect);
      }
    }
  }


  private class MyTreeInputCallback implements TreeInputHandler.Callback {
    public void treeExpanded(TreePath actualPath) {
      if (myListAdapter != null)
        myListAdapter.expand(actualPath);
    }

    public void treeCollapsed(TreePath actualPath) {
      if (myListAdapter != null) {
        myListAdapter.collapse(actualPath);
      }
    }

    public void pathSelected(TreeSelectionEvent e, TreeNodeState state, TreePath rootPath) {
//      TreePath newPath = e.getNewLeadSelectionPath();
//      if (newPath == null)
//        return;
//      else {
//        TreePath actualPath = state.restorePath(newPath, rootPath);
//        if (actualPath != null && myListAdapter != null) {
//          int index = myListAdapter.getPathIndex(actualPath);
//          assert index >= 0;
//          ListSelectionModel selection = myComponent.getSelectionModel();
//          if (selection.getLeadSelectionIndex() != index)
//            selection.setLeadSelectionIndex(index);
////          selection.setLeadSelectionIndex(index);
////          myComponent.getSelectionAccessor().setSelectedIndex(index);
//        }
//      }
    }
  }


  private class MyMouseAdapter extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      dispatchEvent(e);
    }

    public void mousePressed(MouseEvent e) {
      dispatchEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
      dispatchEvent(e);
    }

    private void dispatchEvent(MouseEvent event) {
      TreeToListAdapter<T> listAdapter = myListAdapter;
      if (listAdapter == null)
        return;
      int rowIndex = myComponent.getElementIndexAt(event.getX(), event.getY());
      if (rowIndex < 0 || rowIndex >= listAdapter.getRowCount())
        return;
      JTable table = (JTable) myComponent.getSwingComponent();
      TableColumnModel columnModel = table.getColumnModel();
      if (columnModel.getColumnCount() <= myTreeColumnIndex || myTreeColumnIndex < 0)
        return;
      int offset = 0;
      for (int i = 0; i < myTreeColumnIndex; i++)
        offset += columnModel.getColumn(i).getWidth();
      if (event.getX() < offset || event.getX() > offset + columnModel.getColumn(myTreeColumnIndex).getWidth())
        return;
      TreeNodeState nodeState = getNodeState(rowIndex);
      if (nodeState != null) {
        event.translatePoint(-offset, 0);
        myTreeInputHandler.processMouseEvent(event, nodeState);
        event.translatePoint(offset, 0);
      }
    }
  }


  private class MyKeyListener implements KeyListener {
    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
      processKeyStroke(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers(), false), e);
    }

    public void keyReleased(KeyEvent e) {
      processKeyStroke(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers(), true), e);
    }

    private void processKeyStroke(KeyStroke stroke, KeyEvent event) {
      int index = ((JTable) myComponent.getSwingComponent()).getSelectionModel().getLeadSelectionIndex();
      if (index >= 0 && myListAdapter != null && index < myListAdapter.getRowCount()) {
        TreeNodeState nodeState = getNodeState(index);
        if (nodeState != null) {
          myTreeInputHandler.processKeyStroke(stroke, event, nodeState);
        }
      }
    }
  }


  private class MyATableState implements ATableState<T> {
    public int getColumnSortState(@NotNull TableColumnAccessor<T, ?> columnAccessor) {
      TreeToListAdapter<T> treeAdapter = myListAdapter;
      if (treeAdapter == null)
        return 0;
      Comparator<? super T> order = treeAdapter.getOrder();
      if (order == null)
        return 0;
      Comparator<? super T> columnComparator = columnAccessor.getComparator();
      int same = Containers.checkSameComparators(order, columnComparator);
      if (same == 0)
        return 1;
      else if (same == 1)
        return -1;
      return 0;
    }

    public TableColumnAccessor<? super T, ?> getColumnAccessor(int modelIndex) {
      return myComponent.getColumnModel().getAt(modelIndex);
    }

    public void sortBy(TableColumnAccessor<? super T, ?> column, boolean reverse) {
      if (column.isSortingAllowed()) {
        TreeToListAdapter<T> adapter = myListAdapter;
        if (adapter == null)
          return;
        Comparator<? super T> comparator = column.getComparator();
        Comparator<? super T> comp;
        if (reverse)
          comp = Containers.reverse(comparator);
        else
          comp = comparator;
        adapter.sort(comp, mySortPolicy);
        mySortingListeners.getDispatcher().onSortedBy(column, reverse);
      }
    }
  }

  private static class Deduplicator<T> extends DelegatingSelectionAccessor<T> implements ChangeListener {
    private final Object myLock = new Object();
    private boolean myCacheValid = false;
    private final Set<T> myCacheSet = Collections15.linkedHashSet();
    private final List<T> myCacheList = Collections15.arrayList();

    public Deduplicator(SelectionAccessor<T> delegate) {
      super(delegate);
      delegate.addChangeListener(Lifespan.FOREVER, ThreadGate.AWT_IMMEDIATE, this);
    }

    @Override
    public void onChange() {
      synchronized(myLock) {
        myCacheValid = false;
      }
    }

    @NotNull
    @Override
    public List<T> getSelectedItems() {
      synchronized(myLock) {
        if(!myCacheValid) {
          myCacheSet.clear();
          myCacheSet.addAll(super.getSelectedItems());
          myCacheList.clear();
          myCacheList.addAll(myCacheSet);
          myCacheSet.clear();
          myCacheValid = true;
        }
        return Collections15.arrayList(myCacheList);
      }
    }
  }
}
