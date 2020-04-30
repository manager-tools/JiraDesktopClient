package com.almworks.items.wrapper;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBWriter;
import com.almworks.items.api.WriteTransaction;
import org.jetbrains.annotations.NotNull;

import static com.almworks.items.wrapper.DatabaseWrapperPrivateUtil.wrapWriter;

class WriteTransactionWrapper<T> implements WriteTransaction<T> {
  private final WriteTransaction<T> myTransaction;

  public WriteTransactionWrapper(@NotNull WriteTransaction<T> transaction) {
    //noinspection ConstantConditions
    if (transaction == null) throw new NullPointerException();
    myTransaction = transaction;
  }

  @Override
  public T transaction(DBWriter writer) throws DBOperationCancelledException {
    return myTransaction.transaction(wrapWriter(writer));
  }
}
