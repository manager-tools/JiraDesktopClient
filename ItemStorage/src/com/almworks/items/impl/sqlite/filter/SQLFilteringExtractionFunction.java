package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

public abstract class SQLFilteringExtractionFunction extends ExtractionFunction {
  private final ExtractionFunction myInput;

  protected SQLFilteringExtractionFunction(ExtractionFunction input) {
    myInput = input;
  }

  public final ExtractionFunction getInput() {
    return myInput;
  }

  protected boolean preExecute(TransactionContext context, ExtractionVisitor visitor)
    throws SQLiteException
  {
    return true;
  }

  protected abstract void applySql(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException;

  public final void execute(TransactionContext context, final ExtractionVisitor visitor)
    throws SQLiteException
  {
    if (!preExecute(context, visitor)) {
      return;
    }
    myInput.execute(context, new ExtractionVisitor() {
      public void visitStarted(TransactionContext context) {
        visitor.visitStarted(context);
      }

      public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
        applySql(context, sql);
        visitor.visitSQL(context, sql);
      }

      public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
        SQLItemSelectBuilder sql = new SQLItemSelectBuilder();
        sql.whereItemInArray(items);
        visitSQL(context, sql);
      }

      public void visitFinished(TransactionContext context) throws SQLiteException {
        visitor.visitFinished(context);
      }
    });
  }
}
