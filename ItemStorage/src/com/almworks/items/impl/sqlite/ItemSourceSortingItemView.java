package com.almworks.items.impl.sqlite;

import com.almworks.integers.*;
import com.almworks.items.impl.dbadapter.*;
import com.almworks.sqlite4java.*;
import com.almworks.util.SimpleWrapper;
import com.almworks.util.ValueWrapper;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.almworks.util.Collections15.arrayList;

class ItemSourceSortingItemView extends SortingItemView implements ItemSource.Listener {
  private final ItemSource mySource;
  private final ItemViewConfig myConfig;

  /**
   * Protected with "this"
   */
  private TransactionContext myRunningContext;

  public ItemSourceSortingItemView(ItemSource source, ItemViewConfig config) {
    mySource = source;
    myConfig = config;
  }

  protected void lifeStarted(Lifespan life) {
    super.lifeStarted(life);
    mySource.addListener(life, getPriority(), this);
  }

  @ThreadSafe
  protected void lifeEnded() {
    super.lifeEnded();
    synchronized (this) {
      // we have to call cancel under a lock, otherwise we might cancel something else
      // todo double-check locks
      TransactionContext context = myRunningContext;
      if (context != null) {
        // context may have been cancelled already - no problem
        context.cancel();
      }
    }
  }

  protected void priorityChanged(int priority) {
    mySource.setPriority(priority, this);
  }

  @CanBlock
  public void reload(TransactionContext context, String idTableName) throws SQLiteException {
    context.checkCancelled();
    if (!hasListeners())
      throw new SQLiteInterruptedException();
    LongList items;
    LongList itemGraph;
    ValueWrapper<AbstractGroupsDist> groups = SimpleWrapper.create();
    synchronized (this) {
      assert myRunningContext == null : myRunningContext;
      myRunningContext = context;
    }
    try {
      items = loadGroupedSortedItems(context, idTableName, groups);
      itemGraph = loadGraph(context, idTableName);
    } finally {
      synchronized (this) {
        assert myRunningContext == context;
        myRunningContext = null;
      }
    }
    reload(items, itemGraph, groups.getValue());
  }


  public void update(final TransactionContext context, ItemSourceUpdateEvent event, String idTableName, boolean forced)
    throws SQLiteException
  {
    context.checkCancelled();
    if (!hasListeners())
      throw new SQLiteInterruptedException();
    // todo refactor for effectiveness (the code is whole-imported from QueryResultEventProcessor)
    LongList removed = event.getRemovedItemsSorted();
    LongList added = event.getAddedItemsSorted();
    LongList updated = event.getUpdatedItemsSorted();
    Inserter inserter = null;
    ItemViewConfig.SparseGraphTreeInfo treeInfo = myConfig.getTreeInfo();
    LongList graphUpdate = treeInfo == null ? null : LongList.EMPTY;
    boolean replaceGraph = false;
    int insertSize = updated.size() + added.size();
    if (insertSize > 0) {
      inserter = new Inserter() {
        public void insert(LongList source, LongListInsertingDecorator target,
          @Nullable GroupsDistInsertingDecorator groups) throws SQLiteException
        {
          // todo cache sql in inserter to be reused when both inserts and updates are there; dispose after calling super.update
          insertItems(source, target, groups, context);
        }
      };

      if (treeInfo != null) {
        DBTable table = treeInfo.getTable();
        String tableName = context.getTableName(table.getDefinition(), false);
        if (tableName != null) {
//          replaceGraph = insertSize > BIND_PARAMS_COUNT;
          replaceGraph = false; // todo may be reload graph some time
          if (replaceGraph) {
            graphUpdate = loadGraph(context, idTableName);
          } else {
            DBIntColumn left = treeInfo.getItemLeftColumn();
            DBIntColumn right = treeInfo.getItemRightColumn();
            assert table.getDefinition().containsColumn(left) : table + " " + left;
            assert table.getDefinition().containsColumn(right) : table + " " + right;
            LongList concat = LongListConcatenation.concatUnmodifiable(updated, added);
            graphUpdate = loadGraphUpdate(context, tableName, left, right, concat);
          }
        } else {
          replaceGraph = true;
        }
      }
    }
    update(added, removed, updated, inserter, graphUpdate, replaceGraph);
  }


  private WritableLongList loadGraphUpdate(TransactionContext context, String tableName, DBIntColumn left,
    DBIntColumn right, LongList items) throws SQLiteException
  {
    SQLiteLongArray array = context.useArray(items);
    WritableLongList result;
    try {
      SQLParts parts = new SQLParts();
      parts.append("SELECT ")
        .append(left.getName())
        .append(" rleft, ")
        .append(right.getName())
        .append(" rright ")
        .append("FROM ")
        .append(tableName)
        .append(" WHERE ")
        .append(" rleft IN ")
        .append(array.getName())
        .append(" OR rright IN ")
        .append(array.getName())
        .append(" ORDER BY rleft, rright");
      result = IntCollectionFactory.createLongList();
      SQLiteStatement st = context.prepare(parts);
      try {
        context.addCancellable(st);
        while (st.step()) {
          result.add(st.columnLong(0));
          result.add(st.columnLong(1));
        }
      } finally {
        st.dispose();
        context.removeCancellable(st);
      }
    } finally {
      array.dispose();
    }
    return result;
  }

  /**
   * @return applicable incidence list
   */
  @Nullable
  private LongList loadGraph(TransactionContext context, String idTableName) throws SQLiteException {
    ItemViewConfig.SparseGraphTreeInfo treeInfo = myConfig.getTreeInfo();
    if (treeInfo == null)
      return null;
    DBTable table = treeInfo.getTable();
    DBIntColumn left = treeInfo.getItemLeftColumn();
    DBIntColumn right = treeInfo.getItemRightColumn();
    assert table.getDefinition().containsColumn(left) : table + " " + left;
    assert table.getDefinition().containsColumn(right) : table + " " + right;
    String tableName = context.getTableName(table.getDefinition(), false);
    if (tableName == null)
      return LongList.EMPTY;
    SQLParts parts = new SQLParts();
    parts.append("SELECT rleft, rright FROM (")
      .append("SELECT ")
      .append(left.getName())
      .append(" rleft, ")
      .append(right.getName())
      .append(" rright ")
      .append("FROM ")
      .append(tableName)
      .append(" rel INNER JOIN ")
      .append(idTableName)
      .append(" rs1 ON rleft = rs1.id")
      .append(") s1 INNER JOIN ")
      .append(idTableName)
      .append(" rs2 ON rright = rs2.id ORDER BY rleft, rright");
    SQLiteStatement st = context.getConnection().prepare(parts, false);
    WritableLongList result = IntCollectionFactory.createLongList();
    try {
      context.addCancellable(st);
      while (st.step()) {
        result.add(st.columnLong(0));
        result.add(st.columnLong(1));
      }
    } finally {
      st.dispose();
      context.removeCancellable(st);
    }
    // more effectively
    return result;
  }

  private WritableLongList loadGroupedSortedItems(TransactionContext context, String idTableName,
    ValueWrapper<AbstractGroupsDist> groups) throws SQLiteException
  {
    assert groups != null;
    SQLiteStatement st =
      context.getConnection().prepare(new SQLParts(getSQL(context, SelectType.NORMAL, idTableName, null)), false);
    try {
      context.addCancellable(st);
      WritableLongList result = IntCollectionFactory.createLongList();
      // use effective one-column load if no grouping is selected
      if (myConfig.getGroupings().isEmpty()) {
        SQLUtil.loadLongs(st, context, result);
      } else {
        List<IntSameValuesList> backingGroupsList = null;
        int groupColCount = -1;
        while (st.step()) {
          if (backingGroupsList == null) {
            groupColCount = st.columnCount() - 1;
            // groupColCount = -1: no columns in the result; groupColCount = 0: no columns for grouping in the result
            assert groupColCount > 0;
            backingGroupsList = arrayList(groupColCount);
            for (int i = 0; i < groupColCount; ++i) {
              backingGroupsList.add(new IntSameValuesList());
            }
          }
          result.add(st.columnInt(0));
          for (int i = 0; i < groupColCount; ++i) {
            backingGroupsList.get(i).add(st.columnInt(i + 1));
          }
        }
        groups.setValue(new GroupsDist(backingGroupsList));
      }
      return result;
    } finally {
      st.dispose();
      context.removeCancellable(st);
    }
  }

  private void insertItems(LongList source, LongListInsertingDecorator target,
    @Nullable GroupsDistInsertingDecorator groups, TransactionContext context) throws SQLiteException
  {
    SQLiteLongArray array = null;
    SQLiteLongArray arrayForGroups = null;
    SQLiteStatement stmt = null;
    SQLiteStatement stmtForGroups = null;
    try {
      array = context.getConnection().createArray();
      SQLParts sqlParts = new SQLParts(getSQL(context, SelectType.LIMIT_IDS_NO_GROUPS, null, array));
      stmt = context.prepare(sqlParts);
      context.addCancellable(stmt);
      if (groups != null) {
        arrayForGroups = context.getConnection().createArray(null, false);
        stmtForGroups = context.prepare(new SQLParts(getSQL(context, SelectType.LIMIT_IDS, null, arrayForGroups)));
        context.addCancellable(stmtForGroups);
      }
      FindInsertIndex findInsert = new FindInsertIndex(stmt, 40);
      insert(context, findInsert, source, groups, stmtForGroups, target, array, arrayForGroups);
    } finally {
      context.removeCancellable(stmtForGroups);
      context.removeCancellable(stmt);
      if (stmtForGroups != null) stmtForGroups.dispose();
      if (stmt != null) stmt.dispose();
      if (array != null) array.dispose();
      if (arrayForGroups != null) arrayForGroups.dispose();
    }
  }

  private static void insert(TransactionContext context, FindInsertIndex find, LongList source,
    @Nullable GroupsDistInsertingDecorator groups, @Nullable SQLiteStatement stmtForGroups,
    LongListInsertingDecorator target, SQLiteLongArray array, SQLiteLongArray arrayForGroups) throws SQLiteException
  {
    assert (groups == null) == (stmtForGroups == null);
    int processed = 0;
    final int sourceSize = source.size();
    boolean hasRow = false;
    int colCount = -1;
    WritableIntList groupIds = null;
    while (processed < sourceSize) {
      long item;
      if (groups == null) {
        item = source.get(processed);
      } else {
        // load groupIds for items
        if (hasRow) {
          hasRow = stmtForGroups.step();
        }
        if (!hasRow) {
          // load BIND_PARAMS_COUNT (or less if less remained unprocessed) items with groups using stmt
          context.bindArray(arrayForGroups, source.subList(processed, sourceSize), false, false);
          hasRow = stmtForGroups.step();
        }
        assert hasRow;
        if (colCount == -1) {
          colCount = stmtForGroups.columnCount();
          assert colCount > 1 : colCount == 1 ? "no grouping columns (though grouping is defined)" : "no columns";
          groupIds = IntCollectionFactory.createList(colCount - 1);
          groupIds.expand(0, colCount - 1);
        }
        item = stmtForGroups.columnInt(0);
        for (int j = 1; j < colCount; ++j) {
          groupIds.set(j - 1, stmtForGroups.columnInt(j));
        }
      }
      int index = find.findInsertIndex(target, item, groupIds, groups);
      target.insert(index, item);
      if (groups != null) {
        groups.insert(index, groupIds);
      }
      context.checkCancelled();
      ++processed;
    }
  }

  String getSQL(TableResolver context, SelectType selectType, String sourceTable, SQLiteLongArray array)
    throws SQLiteException
  {
    String idsTable;
    String itemIdColumn;
    if (selectType.limitIds()) {
      idsTable = Schema.ITEMS;
      itemIdColumn = DBColumn.ITEM.getName();
    } else {
      idsTable = sourceTable;
      // todo magic "id"
      itemIdColumn = "id";
    }
    JoinBuilder sql = startSelect(idsTable, itemIdColumn, selectType.loadGroups());
    if (selectType.loadGroups()) {
      addGroups(sql, idsTable, itemIdColumn);
    }
    addSort(sql, context);
    if (selectType.limitIds()) {
      sql.addWhere(sql.getJoinColumn() + " IN " + array.getName());
    }
    return sql.getSQL();
  }

  private JoinBuilder startSelect(String idsTable, String itemIdColumn, boolean loadGroups) {
    JoinBuilder sql = new JoinBuilder("LEFT OUTER JOIN", idsTable, itemIdColumn);
    String groupingColumns = loadGroups ? getGroupingColumns() : "";
    sql.start("SELECT " + sql.getJoinColumn() + (groupingColumns.length() == 0 ? "" : ", ") + groupingColumns);
    return sql;
  }

  private String getGroupingColumns() {
    StringBuilder groupingColumns = new StringBuilder();
    String prefix = "";
    for (ItemViewConfig.Grouping grouping : myConfig.getGroupings()) {
      groupingColumns.append(prefix)
        .append(grouping.getAttribute().getTable())
        .append('.')
        .append(grouping.getAttribute().getNextColumn());
      prefix = ", ";
    }

    return groupingColumns.toString();
  }

  private void addGroups(JoinBuilder sql, String idsTable, String itemIdColumn) {
    for (ItemViewConfig.Grouping grouping : myConfig.getGroupings()) {
      String fromTable = idsTable;
      String fromColumn = itemIdColumn;
      for (AttributeStep step : grouping.getAllSteps()) {
        sql.addTable(fromTable, step.getTable(), fromColumn, step.getPrevColumn());
        fromTable = step.getTable();
        fromColumn = step.getNextColumn();
      }
      sql.addSortBy(fromTable, fromColumn, false);
    }
  }

  private void addSort(JoinBuilder sql, TableResolver context) throws SQLiteException {
    for (ItemViewConfig.SortingElement order : myConfig.getSorting()) {
      String table = context.getTableName(order.getTable().getDefinition(), false);
      if (table == null)
        continue;
      sql.addTable(table);
      sql.addSortBy(table, order.getColumn().getName(), order.isDescending());
    }
  }

  private enum SelectType {
    NORMAL(false, true),
    LIMIT_IDS(true, true),
    LIMIT_IDS_NO_GROUPS(true, false);

    private final boolean myLimitIds;
    private final boolean myLoadGroups;

    private SelectType(boolean limitIds, boolean loadGroups) {
      myLimitIds = limitIds;
      myLoadGroups = loadGroups;
    }

    public boolean limitIds() {
      return myLimitIds;
    }

    public boolean loadGroups() {
      return myLoadGroups;
    }
  }
}
