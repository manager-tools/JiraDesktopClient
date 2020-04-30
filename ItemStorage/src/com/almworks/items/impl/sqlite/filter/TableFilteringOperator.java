package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBColumnType;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.sqlite.TransactionContext;
import org.jetbrains.annotations.NotNull;

public class TableFilteringOperator extends TableBasedOperator {
  private final WhereBuilder myWhereBuilder;

  public TableFilteringOperator(@NotNull DBTable table, boolean negated, boolean separateJoin, WhereBuilder whereBuilder) {
    super(table, negated, separateJoin);
    myWhereBuilder = whereBuilder;
  }

  @Override
  public int getPerformanceHit() {
    int r = super.getPerformanceHit();
    // something = ? (integer) is quicker than others
    if (myWhereBuilder instanceof WhereBuilder.Equals) {
      DBColumn column = ((WhereBuilder.Equals) myWhereBuilder).getColumn();
      if (column.getDatabaseClass() == DBColumnType.INTEGER) {
        return r;
      }
    }
    return r + 1;
  }

  @Override
  protected WhereBuilder where(TransactionContext context) {
    return myWhereBuilder;
  }

  @Override
  public String toString() {
    return (isAcceptMissing() ? "NOT(" : "(") + myWhereBuilder + ")";
  }
}
