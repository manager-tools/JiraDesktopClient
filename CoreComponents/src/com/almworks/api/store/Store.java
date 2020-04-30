package com.almworks.api.store;

import org.jetbrains.annotations.NotNull;

public interface Store {
  public static final String DELIMITER = ".";

  String getPrefixPath();

  Store getSubStore(String path);

  @NotNull
  StoreAccess access(String path);

  StoreAccess access(String path, StoreFeature[] features);
}
