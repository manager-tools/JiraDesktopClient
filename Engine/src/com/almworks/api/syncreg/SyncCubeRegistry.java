package com.almworks.api.syncreg;

import com.almworks.items.api.DBAttribute;
import com.almworks.util.threads.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SyncCubeRegistry {
  @ThreadSafe
  boolean isSynced(@Nullable Hypercube<DBAttribute<?>, Long> cube);

  @ThreadSafe
  void setSynced(@NotNull Hypercube<DBAttribute<?>, Long> cube);

  @ThreadSafe
  void setUnsynced(@NotNull Hypercube<DBAttribute<?>, Long> cube);
}
