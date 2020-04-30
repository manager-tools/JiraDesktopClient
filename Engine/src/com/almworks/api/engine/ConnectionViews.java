package com.almworks.api.engine;

import com.almworks.items.api.DBFilter;
import com.almworks.util.collections.Modifiable;
import org.jetbrains.annotations.NotNull;

public interface ConnectionViews {
  DBFilter getConnectionItems();

  @NotNull
  Modifiable connectionItemsChange();

  DBFilter getOutbox();
}

