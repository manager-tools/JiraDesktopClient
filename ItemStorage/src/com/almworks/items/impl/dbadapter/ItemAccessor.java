package com.almworks.items.impl.dbadapter;

import org.jetbrains.annotations.Nullable;

// todo rename to cache item accessor?
public interface ItemAccessor {
  long getItem();

  boolean hasValues();

  @Nullable
  Object getValue(SyncValueLoader attribute);

  int getInt(SyncValueLoader attribute, int missingValue);

  long getLong(SyncValueLoader attribute, long missingValue);

  boolean hasUptodateValue(SyncValueLoader attribute);
  
}
