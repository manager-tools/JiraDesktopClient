package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.IntArray;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.tables.SortingTableHeaderController;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.config.UtilConfigNames;
import com.almworks.util.models.TableColumnAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

/**
 * @author : Dyoma
 */
public class ColumnsConfiguration {
  public static <T> void install(Lifespan life, Configuration config, SortingTableHeaderController<T> header) {
    boolean isEmptyConfig = !config.isSet(UtilConfigNames.COLUMN);
    install(life, config, isEmptyConfig, header);
  }

  public static <T> void install(Lifespan life, Configuration config, boolean emptyConfigMode, SortingTableHeaderController<T> header) {
    SortingColumnSetUpdater<T> updater = new SortingColumnSetUpdater<T>(config, header);
    SubsetModel<TableColumnAccessor<T, ?>> columnsSubset = header.getUserColumnsSubsetModel();
    if (emptyConfigMode) {
      updater.sortByFirst();
      updater.updateConfig();
    } else {
      updater.readConfig();
    }
    listenFullSetChange(life, updater, columnsSubset, emptyConfigMode);
    life.add(header.addTableColumnListener(updater));
    life.add(header.addSortingListener(updater));
    if (emptyConfigMode) {
      columnsSubset.setFull();
    }
  }

  public static void install(Lifespan life, Configuration config, ATable<?> table,
    SubsetModel<? extends TableColumnAccessor<?, ?>> columnSubset, boolean emptyConfig) {
    ColumnSetUpdater updater = new ATableUpdater(config, table, columnSubset);
    if (emptyConfig) {
      columnSubset.setFull();
      updater.updateConfig();
    } else {
      updater.readConfig();
    }
    listenFullSetChange(life, updater, columnSubset, emptyConfig);
    table.getTableModel().addTableColumnListener(life, updater);
  }

  private static void listenFullSetChange(Lifespan life, final ColumnSetUpdater updater,
    final SubsetModel<? extends TableColumnAccessor<?, ?>> columnsSubset, final boolean emptyConfigMode)
  {
    //noinspection unchecked
    life.add(((AListModel<TableColumnAccessor<?, ?>>) columnsSubset.getFullSet()).addListener(
      new AListModel.Adapter<TableColumnAccessor<?, ?>>() {
        public void onInsert(int index, int length) {
          if (emptyConfigMode) {
            int[] indices = new int[length];
            for (int i = 0; i < indices.length; i++)
              indices[i] = index + i;
            columnsSubset.addFromFullSet(indices);
          } else {
            updater.filterFullSet();
          }
        }

        public void onRemove(int index, int length, AListModel.RemovedEvent<TableColumnAccessor<?, ?>> event) {
          updater.filterFullSet();
        }
      }));
  }

  private static final class SearchByIdCondition extends Condition<TableColumnAccessor> {
    private String myColumnId;

    public boolean isAccepted(TableColumnAccessor value) {
      return value.getId().equals(myColumnId);
    }

    public void setColumnId(String columnId) {
      myColumnId = columnId;
    }
  }


  private static final class ColumnInfo implements Comparable<ColumnInfo> {
    public final String id;
    public final int order;
    public final int sortState;
    public final int width;

    public ColumnInfo(String id, ReadonlyConfiguration config) {
      this.id = id;
      int width = -1;
      try {
        width = Integer.parseInt(config.getSetting(UtilConfigNames.WIDTH, "-1"));
      } catch (NumberFormatException e) {
        Log.warn("invalid setting " + UtilConfigNames.WIDTH);
      }
      this.width = width;
      int order = 1000;
      try {
        order = Integer.parseInt(config.getSetting(UtilConfigNames.ORDER, "1000"));
      } catch (NumberFormatException e) {
        Log.warn("invalid setting " + UtilConfigNames.ORDER);
      }
      this.order = order;
      if (config.isSet(UtilConfigNames.SORTED_REVERSE))
        sortState = Boolean.valueOf(config.getSetting(UtilConfigNames.SORTED_REVERSE, "false")) ? -1 : 1;
      else
        sortState = 0;
    }

    public int compareTo(ColumnInfo another) {
      int c = Containers.compareInts(order, another.order);
      if (c != 0)
        return c;
      return id.compareTo(another.id);
    }

    public String toString() {
      return "ColumnInfo (" + id + "@" + order + " W:" + width + " sort:" + sortState + ")";
    }
  }


  private static abstract class DefaultSortStrategy<T> {
    private static final DefaultSortStrategy<?> SORT_BY_FIRST = new DefaultSortStrategy<Object>() {
      public boolean sort(SortingTableHeaderController<Object> tableModel) {
        AListModel<TableColumnAccessor<Object, ?>> columnModel = tableModel.getColumnModel();
        return columnModel.getSize() != 0 && doSort(tableModel, columnModel.getAt(0), false);
      }
    };

    public static <T> DefaultSortStrategy<T> sortByFirst() {
      //noinspection unchecked
      return (DefaultSortStrategy<T>) SORT_BY_FIRST;
    }

    public static <T> DefaultSortStrategy<T> sortByColumn(final String id, final boolean reverse) {
      return new DefaultSortStrategy<T>() {
        public boolean sort(SortingTableHeaderController<T> tableModel) {
          SearchByIdCondition searchById = new SearchByIdCondition();
          searchById.setColumnId(id);
          AListModel<TableColumnAccessor<T, ?>> fullSet = tableModel.getUserColumnsSubsetModel().getFullSet();
          TableColumnAccessor<T, ?> column = fullSet.detect(searchById);
          return doSort(tableModel, column, reverse);
        }
      };
    }

    public abstract boolean sort(SortingTableHeaderController<T> tableModel);

    protected <T> boolean doSort(SortingTableHeaderController<T> controller, TableColumnAccessor<T, ?> column,
      boolean reverse)
    {
      if (column == null)
        return false;
      controller.sortByColumn(column, reverse);
      return true;
    }
  }


  private static abstract class ColumnSetUpdater implements TableColumnModelListener {
    private final Configuration myConfig;
    private SortedSet<ColumnInfo> myShownColumns = Collections15.treeSet();

    public ColumnSetUpdater(Configuration config) {
      myConfig = config;
    }

    public void columnMarginChanged(ChangeEvent e) {
      updateConfig();
    }

    public void columnSelectionChanged(ListSelectionEvent e) {
    }

    public void columnAdded(TableColumnModelEvent e) {
      updateConfig();
    }

    public void columnMoved(TableColumnModelEvent e) {
      if (e.getFromIndex() != e.getToIndex())
        updateConfig();
    }

    public void columnRemoved(TableColumnModelEvent e) {
      updateConfig();
    }

    public void updateConfig() {
      myConfig.clear();
      myShownColumns.clear();
      SubsetModel<? extends TableColumnAccessor<?, ?>> columnModel = getColumnsSubsetModel();
      SearchByIdCondition searchById = new SearchByIdCondition();
      for (int i = 0; i < getSwingColumnCount(); i++) {
        TableColumn swingColumn = getSwingColumn(i);
        TableColumnAccessor<?, ?> column = ATable.getColumnAccessor(swingColumn);
        if (column == null)
          continue;
        String id = column.getId();
        searchById.setColumnId(id);
        if (((AListModel) columnModel).detectIndex(searchById) < 0)
          continue;
        Configuration columnConfig = myConfig.createSubset(UtilConfigNames.COLUMN);
        columnConfig.setSetting(UtilConfigNames.ID, id);
        columnConfig.setSetting(UtilConfigNames.ORDER, Integer.toString(i));
        if (swingColumn != null)
            columnConfig.setSetting(UtilConfigNames.WIDTH, Integer.toString(swingColumn.getWidth()));
        updatColumnConfig(swingColumn, columnConfig);
        myShownColumns.add(new ColumnInfo(id, columnConfig));
      }
    }

    protected abstract void updatColumnConfig(TableColumn swingColumn, Configuration columnConfig);

    protected abstract int getSwingColumnCount();

    public void readConfig() {
      List<? extends ReadonlyConfiguration> configs = myConfig.getAllSubsets(UtilConfigNames.COLUMN);
      for (ReadonlyConfiguration config : configs) {
        final String columnId = config.getSetting(UtilConfigNames.ID, null);
        if (columnId == null)
          continue;
        ColumnInfo info = new ColumnInfo(columnId, config);
        myShownColumns.add(info);
        processColumnInfo(columnId, info);
      }
      filterFullSet(myShownColumns);
      setupWidths(myShownColumns);
    }

    protected abstract void processColumnInfo(String columnId, ColumnInfo info);

    @Nullable
    protected abstract TableColumn getSwingColumn(int index);

    public void filterFullSet(SortedSet<ColumnInfo> shownColumns) {
      AListModel<? extends TableColumnAccessor<?, ?>> fullSet = getColumnsSubsetModel().getFullSet();
      SearchByIdCondition searchById = new SearchByIdCondition();
      IntArray fullSetIndices = new IntArray();
      for (ColumnInfo columnInfo : shownColumns) {
        searchById.setColumnId(columnInfo.id);
        int index = fullSet.detectIndex(searchById);
        if (index != -1)
          fullSetIndices.add(index);
      }
      getColumnsSubsetModel().setSubsetIndices(fullSetIndices.toNativeArray());
    }

    protected abstract SubsetModel<? extends TableColumnAccessor<?, ?>> getColumnsSubsetModel();

    private void setupWidths(SortedSet<ColumnInfo> shownColumns) {
      List<ColumnInfo> columnInfos = Collections15.arrayList(shownColumns);
      Collections.sort(columnInfos);
      for (ColumnInfo columnInfo : shownColumns) {
        int index = columnInfo.order;
        if (index >= getSwingColumnCount())
          continue;
        TableColumn column = getSwingColumn(index);
        if (column != null && columnInfo.width > 0)
          column.setPreferredWidth(columnInfo.width);
      }
    }

    public void filterFullSet() {
      filterFullSet(myShownColumns);
    }
  }

  private static class SortingColumnSetUpdater<T> extends ColumnSetUpdater implements SortingListener {
    private DefaultSortStrategy<T> myDefaultSortStrategy = null;
    private final SortingTableHeaderController<T> myHeader;

    public SortingColumnSetUpdater(Configuration config, SortingTableHeaderController<T> header) {
      super(config);
      myHeader = header;
    }

    public void onSortedBy(TableColumnAccessor<?, ?> column, boolean reverse) {
      updateConfig();
    }

    public void applySavedSorting() {
      if (myDefaultSortStrategy != null && myDefaultSortStrategy.sort(myHeader))
        myDefaultSortStrategy = null;
    }

    protected int getSwingColumnCount() {
      return myHeader.getSwingColumnModel().getColumnCount();
    }

    public void readConfig() {
      if (myDefaultSortStrategy == null)
        myDefaultSortStrategy = DefaultSortStrategy.sortByFirst();
      super.readConfig();
    }

    protected void processColumnInfo(String columnId, ColumnInfo info) {
      if (info.sortState != 0) {
        myDefaultSortStrategy = DefaultSortStrategy.sortByColumn(columnId, info.sortState < 0);
        assert myDefaultSortStrategy != null;
      }
    }

    public void filterFullSet(SortedSet<ColumnInfo> shownColumns) {
      super.filterFullSet(shownColumns);
      applySavedSorting();
    }

    protected SubsetModel<? extends TableColumnAccessor<?, ?>> getColumnsSubsetModel() {
      return myHeader.getUserColumnsSubsetModel();
    }

    @Nullable
    protected TableColumn getSwingColumn(int index) {
      return myHeader.getColumnAt(index);
    }

    protected void updatColumnConfig(TableColumn swingColumn, Configuration columnConfig) {
      int sortState = myHeader.getColumnSortState(swingColumn);
      if (sortState != 0)
        columnConfig.setSetting(UtilConfigNames.SORTED_REVERSE, Boolean.valueOf(sortState != 1).toString());
    }

    public void sortByFirst() {
      myDefaultSortStrategy = DefaultSortStrategy.sortByFirst();
      applySavedSorting();
    }
  }


  public static class ATableUpdater extends ColumnSetUpdater {
    private final ATable<?> myTable;
    private final SubsetModel<? extends TableColumnAccessor<?, ?>> myColumnSubset;

    public ATableUpdater(Configuration config, ATable<?> table,
      SubsetModel<? extends TableColumnAccessor<?, ?>> columnSubset) {
      super(config);
      myTable = table;
      myColumnSubset = columnSubset;
    }

    protected void updatColumnConfig(TableColumn swingColumn, Configuration columnConfig) {}

    protected int getSwingColumnCount() {
      return myTable.getTableColumnModel().getColumnCount();
    }

    protected void processColumnInfo(String columnId, ColumnInfo info) {}

    @Nullable
    protected TableColumn getSwingColumn(int index) {
      return myTable.getTableColumnModel().getColumn(index);
    }

    protected SubsetModel<? extends TableColumnAccessor<?, ?>> getColumnsSubsetModel() {
      return myColumnSubset;
    }
  }
}
