package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

public interface ExtractionVisitor {
  void visitStarted(TransactionContext context);

  void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException;

  void visitItems(TransactionContext context, LongIterable items) throws SQLiteException;

  void visitFinished(TransactionContext context) throws SQLiteException;
}
