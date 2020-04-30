package com.almworks.items.api;

public interface WriteTransaction<T> {
  T transaction(DBWriter writer) throws DBOperationCancelledException;
}
