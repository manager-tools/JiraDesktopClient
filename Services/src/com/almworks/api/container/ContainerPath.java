package com.almworks.api.container;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContainerPath {
  private final @NotNull String myName;
  private final @Nullable ContainerPath myParentPath;

  public ContainerPath(@Nullable ContainerPath parentPath, @NotNull String name) {
    myParentPath = parentPath;
    myName = name;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @Nullable
  public ContainerPath getParentPath() {
    return myParentPath;
  }
}
