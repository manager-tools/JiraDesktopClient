package com.almworks.util.components.tables;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ListModelHolder;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.components.ATable;
import com.almworks.util.components.ATableModel;
import com.almworks.util.components.SortingListener;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.threads.Threads;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

/**
 * @author dyoma
 */
public class SortingTableHeaderController<T> {
  private final ATableState myState;
  private final FireEventSupport<SortingListener> myListeners = FireEventSupport.create(SortingListener.class);
  private final JTableHeader myHeader;
  private final ListModelHolder<TableColumnAccessor<T, ?>> myUserFilteredColumns = ListModelHolder.create();

  public SortingTableHeaderController(@NotNull JTableHeader header, @NotNull ATableState state) {
    myHeader = header;
    myState = state;
    myUserFilteredColumns.setModel(SubsetModel.<TableColumnAccessor<T, ?>>empty());

    if(!Aqua.isAqua()) {
      // Aqua LAF client properties are used on the Mac, no need for a custom header renderer.
      myHeader.setDefaultRenderer(new SortingHeaderRenderer(this, header.getDefaultRenderer()));
    }

    myHeader.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        onHeaderClicked(e);
      }
    });
  }

  public Detach addSortingListener(SortingListener listener) {
    DetachComposite life = new DetachComposite();
    myListeners.addStraightListener(life, listener);
    return life;
  }

  public Detach addTableColumnListener(final TableColumnModelListener listener) {
    return ATableModel.addTableColumnListener(listener, getSwingColumnModel());
  }

  @Nullable
  public TableColumnAccessor<T, ?> getColumnAccessor(TableColumn swingColumn) {
    //noinspection unchecked
    return (TableColumnAccessor<T, ?>) ATable.getColumnAccessor(swingColumn);
  }

  @Nullable
  public TableColumn getColumnAt(int visualIndex) {
    Threads.assertAWTThread();
    if (visualIndex < 0)
      return null;
    TableColumnModel model = getSwingColumnModel();
    if (visualIndex >= model.getColumnCount())
      return null;
    return model.getColumn(visualIndex);
  }

  public AListModel<TableColumnAccessor<T, ?>> getColumnModel() {
    return getUserColumnsSubsetModel();
  }

  public int getColumnSortState(TableColumn tableColumn) {
    TableColumnAccessor<T, ?> accessor = getColumnAccessor(tableColumn);
    return accessor == null ? 0 : getColumnSortState(accessor);
  }

  public int getColumnSortState(TableColumnAccessor<T, ?> columnAccessor) {
    return myState.getColumnSortState(columnAccessor);
  }

  public TableColumnModel getSwingColumnModel() {
    return myHeader.getColumnModel();
  }

  public SubsetModel<TableColumnAccessor<T, ?>> getUserColumnsSubsetModel() {
    return (SubsetModel<TableColumnAccessor<T, ?>>) myUserFilteredColumns.getModel();
  }

  public AListModel<TableColumnAccessor<T, ?>> getUserFilteredColumnModel() {
    return myUserFilteredColumns;
  }

  public SortingListener getSortingListener() {
    return new SortingListener() {
      public void onSortedBy(TableColumnAccessor<?, ?> column, boolean reverse) {
        Threads.assertAWTThread();
        myHeader.repaint();
        myListeners.getDispatcher().onSortedBy(column, reverse);
      }
    };
  }

  public void setResizingAllowed(boolean allowed) {
    myHeader.setReorderingAllowed(allowed);
  }

  public void setUserFullColumnsModel(Lifespan lifespan, AListModel<? extends TableColumnAccessor<T, ?>> columns,
    boolean selectedByDefault)
  {
    myUserFilteredColumns.setModel(SubsetModel.create(lifespan, columns, selectedByDefault));
  }

  public void sortByColumn(TableColumnAccessor<T, ?> columnAccessor, boolean reverse) {
    if (columnAccessor.isSortingAllowed()) {
      myState.sortBy(columnAccessor, reverse);

      if(Aqua.isAqua()) {
        final String sortColumnId = columnAccessor.getId();
        final TableColumnModel swingColumnModel = myHeader.getTable().getColumnModel();
        int sortColumnIndex = -1;

        for(int i = 0; i < swingColumnModel.getColumnCount(); i++) {
          TableColumnAccessor<T, ?> accessor = getColumnAccessor(swingColumnModel.getColumn(i));
          if(accessor != null && accessor.getId().equals(sortColumnId)) {
            sortColumnIndex = i;
            break;
          }
        }

        if(sortColumnIndex >= 0) {
          myHeader.putClientProperty("JTableHeader.selectedColumn", sortColumnIndex);
          myHeader.putClientProperty("JTableHeader.sortDirection", reverse ? "decending" : "ascending");
        }
      }
    }
  }

  private void onHeaderClicked(MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 1)
      return;
    int m = e.getModifiersEx();
    if ((m & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.ALT_DOWN_MASK | MouseEvent.META_DOWN_MASK)) != 0)
      return;
    if (myHeader.getCursor() != Cursor.getDefaultCursor())
      return;
    TableColumnModel columnModel = getSwingColumnModel();
    int x = e.getPoint().x;
    TableColumn column = null;
    for (int i = 0; i < columnModel.getColumnCount(); i++) {
      TableColumn currentColumn = columnModel.getColumn(i);
      x = x - currentColumn.getWidth();
      if (x < 0) {
        column = currentColumn;
        break;
      }
    }
    if (column == null)
      return;
    if (x > -3 || x < -column.getWidth() + 3)
      return;
    int modelIndex = column.getModelIndex();
    int sortState = getColumnSortState(column);
    sortByColumn(myState.getColumnAccessor(modelIndex), sortState == 1);
  }

  public static <T> SortingTableHeaderController<T> create(ATable<T> table) {
    ATableStateImpl tableState = new ATableStateImpl(table);
    SortingTableHeaderController<T> result = new SortingTableHeaderController<T>(table.getSwingHeader(), tableState);
    tableState.addSortingListener(result.getSortingListener());
    return result;
  }

  private static class ATableStateImpl<T> implements ATableState<T> {
    private final FireEventSupport<SortingListener> myListeners =
      FireEventSupport.createSynchronized(SortingListener.class);
    private final ATable<T> myTable;

    public ATableStateImpl(ATable<T> table) {
      myTable = table;
    }

    public int getColumnSortState(TableColumnAccessor<T, ?> columnAccessor) {
      AListModel<? extends T> originalModel = getOriginalModel();
      Comparator currentComparator =
        originalModel instanceof SortedListDecorator ? ((SortedListDecorator) originalModel).getComparator() : null;
      int same = Containers.checkSameComparators(currentComparator, columnAccessor.getComparator());
      if (same == 0)
        return 1;
      else if (same == 1)
        return -1;
      return 0;
    }

    @Nullable
    private AListModel<? extends T> getOriginalModel() {
      return myTable.getDataModelHolder().getModel();
    }

    public TableColumnAccessor<? super T, ?> getColumnAccessor(int modelIndex) {
      return myTable.getColumnModel().getAt(modelIndex);
    }

    public void sortBy(TableColumnAccessor<? super T, ?> column, boolean reverse) {
      AListModel<? extends T> originalModel = getOriginalModel();
      if (!(originalModel instanceof SortedListDecorator))
        return;
      if (column.isSortingAllowed()) {
        Comparator<? super T> columnComparator = column.getComparator();
        if (reverse)
          columnComparator = Containers.reverse(columnComparator);
        ((SortedListDecorator) originalModel).setComparator(columnComparator);
        myListeners.getDispatcher().onSortedBy(column, reverse);
      }
    }

    public Detach addSortingListener(SortingListener listener) {
      return myListeners.addStraightListener(listener);
    }
  }
}
