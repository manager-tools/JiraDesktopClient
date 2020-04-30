package com.almworks.items.api;

public interface ReadTransaction<T> {
  T transaction(DBReader reader) throws DBOperationCancelledException;
}
