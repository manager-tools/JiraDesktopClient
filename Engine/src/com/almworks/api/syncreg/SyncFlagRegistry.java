package com.almworks.api.syncreg;

public interface SyncFlagRegistry {
  boolean isSyncFlag(String connectionID, String id);

  void setSyncFlag(String connectionID, String id, boolean flag);
}
