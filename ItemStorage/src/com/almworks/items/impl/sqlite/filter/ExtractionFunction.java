package com.almworks.items.impl.sqlite.filter;


import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

public abstract class ExtractionFunction {
  public abstract void execute(TransactionContext context, ExtractionVisitor visitor)
    throws SQLiteException;
}
