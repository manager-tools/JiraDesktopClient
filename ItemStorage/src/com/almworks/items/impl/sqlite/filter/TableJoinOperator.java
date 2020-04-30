package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.sqlite.TransactionContext;
import org.jetbrains.annotations.NotNull;

public class TableJoinOperator extends TableBasedOperator {
  public TableJoinOperator(@NotNull DBTable table, boolean negated) {
    super(table, negated);
  }

  @Override
  protected WhereBuilder where(TransactionContext context) {
    return WhereBuilder.EXISTS;
  }

  @Override
  public String toString() {
    return (isAcceptMissing() ? "NOT(IN TABLE " : "(IN TABLE ") + getTable() + ")";
  }
}
