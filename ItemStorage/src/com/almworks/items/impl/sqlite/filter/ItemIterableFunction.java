package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

public class ItemIterableFunction extends ExtractionFunction {
  private final LongIterable myItems;

  public ItemIterableFunction(LongIterable items) {
    myItems = items;
  }

  @Override
  public void execute(TransactionContext context, ExtractionVisitor visitor)
    throws SQLiteException
  {
    visitor.visitStarted(context);
    visitor.visitItems(context, myItems);
    visitor.visitFinished(context);
  }
}
