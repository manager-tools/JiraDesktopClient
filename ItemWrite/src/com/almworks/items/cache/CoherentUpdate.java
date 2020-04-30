package com.almworks.items.cache;

import com.almworks.items.api.DBReader;

public interface CoherentUpdate<T> {
  T readDB(DBReader reader);

  void awtUpdate(T loaded);
}
