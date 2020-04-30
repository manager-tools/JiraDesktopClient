package com.almworks.items.entities.dbwrite;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.sync.VersionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EntityWriteProcess {

  @Nullable
  DBAttribute<?> getAttribute(EntityKey<?> key);

  @NotNull
  VersionSource getVersionSource();
}
