package com.almworks.items.impl;

import com.almworks.items.api.WriteTransaction;
import com.almworks.items.impl.dbadapter.DBTransaction;
import com.almworks.items.impl.sqlite.TransactionContext;

import java.util.concurrent.ExecutionException;

class WriteHandle<T> extends AbstractHandle<T> implements DBTransaction {
  private final WriteTransaction<T> myTransaction;

  public WriteHandle(WriteTransaction<T> transaction) {
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
  public void transaction(TransactionContext context) throws Throwable {
    ImplUtil.setDbThread();
    try {
      DBWriterImpl writer = new DBWriterImpl(context, WriteHandle.this);
      setIcn(writer.getTransactionIcn() - 1);
      InconsistentReader.RUNNING_READER.set(writer);
      T r = myTransaction.transaction(writer);
      setIcn(writer.getTransactionIcn());
      setResult(r);
    } finally {
      InconsistentReader.RUNNING_READER.remove();
    }
  }
}
