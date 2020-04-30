package com.almworks.items.impl.dbadapter;

import com.almworks.items.impl.sqlite.TransactionContext;

public interface DBTransaction extends DBCallback {
  public abstract void transaction(TransactionContext context) throws Throwable;
}
