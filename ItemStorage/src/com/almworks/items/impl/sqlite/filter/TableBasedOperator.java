package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import org.jetbrains.annotations.NotNull;

public abstract class TableBasedOperator extends ExtractionOperator {
  @NotNull
  private final DBTable myTable;
  private final DBColumn<?> myJoinColumn;
  private final boolean myAcceptMissingRow;
  private final boolean mySeparateJoin;

  protected TableBasedOperator(@NotNull DBTable table, boolean acceptMissingRow) {
    this(table, acceptMissingRow, false);
  }

  protected TableBasedOperator(@NotNull DBTable table, boolean acceptMissingRow, boolean separateJoin) {
    myTable = table;
    myJoinColumn = DBColumn.ITEM;
    myAcceptMissingRow = acceptMissingRow;
    mySeparateJoin = separateJoin;
  }

  @Override
  public int getPerformanceHit() {
    return isAcceptMissing() ? -2 : -10;
  }

  @NotNull
  public DBTable getTable() {
    return myTable;
  }

  public boolean isAcceptMissing() {
    return myAcceptMissingRow;
  }

  protected abstract WhereBuilder where(TransactionContext context);

  public final ExtractionFunction apply(TransactionContext context, ExtractionFunction input) {
    return new SQLFilteringExtractionFunction(input) {
      private String myTableName;
      private WhereBuilder myWhereBuilder;

      protected boolean preExecute(TransactionContext context, ExtractionVisitor visitor)
        throws SQLiteException
      {
        myTableName = context.getTableName(myTable, false);
        if (myTableName == null) {
          // don't execute
          pass(context, visitor);
          return false;
        }
        myWhereBuilder = where(context);
        if (myWhereBuilder == null) {
          pass(context, visitor);
          return false;
        }
        return true;
      }

      private void pass(TransactionContext context, ExtractionVisitor visitor) throws SQLiteException {
        if (myAcceptMissingRow) {
          // pass execution to input: no filtering here
          getInput().execute(context, visitor);
        } else {
          // nothing is returned
          visitor.visitStarted(context);
          visitor.visitFinished(context);
        }
      }

      protected void applySql(TransactionContext context, SQLItemSelectBuilder sql) {
        assert myTableName != null;
        SQLItemSelectBuilder.Join join = sql.joinPrimaryTable(myTableName, myJoinColumn.getName(), myAcceptMissingRow, mySeparateJoin);
        myWhereBuilder.addWhere(sql, join, isAcceptMissing());
      }
    };
  }

}
