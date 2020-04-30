package com.almworks.items.impl;

import com.almworks.items.api.ReadTransaction;
import com.almworks.items.impl.dbadapter.DBRead;
import com.almworks.items.impl.sqlite.TransactionContext;

import java.util.concurrent.ExecutionException;

class ReadHandle<T> extends AbstractHandle<T> implements DBRead {
  private final ReadTransaction<T> myTransaction;

  public ReadHandle(ReadTransaction<T> transaction) {
    myTransaction = transaction;
  }


  @Override
  public T get() throws InterruptedException, ExecutionException {
    if (ImplUtil.isDbThread()) {
      assert false : this;
      return null;
    }

    return super.get();
  }

  @Override
  public void read(TransactionContext context) throws Throwable {
    ImplUtil.setDbThread();
    try {
      DBReaderImpl reader = new DBReaderImpl(context);
      setIcn(reader.getTransactionIcn() - 1);
      InconsistentReader.RUNNING_READER.set(reader);
      T r = myTransaction.transaction(reader);
      setResult(r);
    } finally {
      InconsistentReader.RUNNING_READER.remove();
    }
  }
}
