package com.almworks.items.wrapper;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import org.jetbrains.annotations.NotNull;

import static com.almworks.items.wrapper.DatabaseWrapperPrivateUtil.wrapReader;

class ReadTransactionWrapper<T> implements ReadTransaction<T> {
  private final ReadTransaction<T> myTransaction;

  public ReadTransactionWrapper(@NotNull ReadTransaction<T> transaction) {
    //noinspection ConstantConditions
    if (transaction == null) throw new NullPointerException();
    myTransaction = transaction;
  }

  @Override
  public T transaction(DBReader reader) throws DBOperationCancelledException {
    return myTransaction.transaction(wrapReader(reader));
  }
}
