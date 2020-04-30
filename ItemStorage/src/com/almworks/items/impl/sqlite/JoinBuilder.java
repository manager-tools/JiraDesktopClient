package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBColumn;
import org.almworks.util.Collections15;

import java.util.Set;

// todo combine with Extractor?
class JoinBuilder {
  private final StringBuilder mySource = new StringBuilder();
  private final StringBuilder myWhere = new StringBuilder();
  private final StringBuilder mySortBy = new StringBuilder();
  private final StringBuilder myAdditionalJoins = new StringBuilder();
  private final String myDefaultJoin;
  private final String mySourceTable;
  private final Set<String> myKnownTables = Collections15.hashSet();
  private final String myJoinColumn;

  public JoinBuilder(String defaultJoin, String sourceTable, String joinColumn) {
    myDefaultJoin = defaultJoin;
    mySourceTable = sourceTable;
    myJoinColumn = joinColumn;
  }

  public void start(String sqlStart) {
    if (mySource.length() != 0) {
      assert false;
      return;
    }
    mySource.append(sqlStart).append(" FROM ").append(mySourceTable);
  }

  public void addTable(String tableName) {
    addTable(tableName, myDefaultJoin);
  }

  public void addTable(String tableName, String join) {
    addTable(mySourceTable, join, tableName, myJoinColumn, DBColumn.ITEM.getName());
  }

  public void addTable(String fromTable, String toTable, String fromColumn, String toColumn) {
    addTable(fromTable, myDefaultJoin, toTable, fromColumn, toColumn);
  }

  public void addTable(String fromTable, String join, String toTable, String fromColumn, String toColumn) {
    if (join == null) {
      assert false;
      return;
    }
    if (fromTable.equals(toTable))
      return;
    if (mySource.length() == 0) {
      assert false;
      return;
    }
    if (myKnownTables.contains(toTable))
      return;
    if(fromTable.length() == 0) {
      fromTable = mySourceTable;
      fromColumn = myJoinColumn;
    }
    mySource.append(' ').append(join).append(' ').append(toTable).append(" ON ");
    appendColumn(mySource, fromTable, fromColumn);
    mySource.append(" = ");
    appendColumn(mySource, toTable, toColumn);
    myKnownTables.add(toTable);
  }

  private void appendColumn(StringBuilder part, String table, String column) {
    part.append(table).append('.').append(column);
  }

  public void addSortBy(String tableName, String column, boolean desc) {
    if (mySortBy.length() > 0)
      mySortBy.append(", ");
    appendColumn(mySortBy, tableName, column);
    if (desc)
      mySortBy.append(" DESC");
  }

  public void addJoin(String sql) {
    myAdditionalJoins.append(' ').append(sql);
  }

  public String getSQL() {
    return mySource.toString() + myAdditionalJoins.toString() + (myWhere.length() > 0 ? " WHERE " : "") + myWhere.toString() +
      (mySortBy.length() > 0 ? " ORDER BY " : "") + mySortBy.toString();
  }

  public String getJoinColumn() {
    return mySourceTable + "." + myJoinColumn;
  }

  public void addWhere(String sqlPart) {
    myWhere.append(sqlPart);
  }

  public StringBuilder getWhere() {
    return myWhere;
  }
}
