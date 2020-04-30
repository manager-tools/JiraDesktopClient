package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

public class ExtractIterableFunction extends ExtractionFunction {
  private final LongIterable myIterable;

  public ExtractIterableFunction(LongIterable iterable) {
    myIterable = iterable;
  }

  public void execute(TransactionContext context, ExtractionVisitor visitor)
    throws SQLiteException
  {
    visitor.visitStarted(context);
    visitor.visitItems(context, myIterable);
    visitor.visitFinished(context);
  }
}
