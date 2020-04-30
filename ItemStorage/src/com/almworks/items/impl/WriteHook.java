package com.almworks.items.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.impl.sqlite.TransactionContext;

public interface WriteHook {
  <T> void onSetValue(TransactionContext context, long item, DBAttribute<T> attribute, T value) throws Exception;
}
