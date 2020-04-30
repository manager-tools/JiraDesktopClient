package com.almworks.items.impl.dbadapter;

import com.almworks.items.impl.sqlite.TransactionContext;

public interface DBRead extends DBCallback {
  public abstract void read(TransactionContext context) throws Throwable;

  public void dbSuccess();

  public void dbFailure(Throwable throwable);
}
