package com.almworks.api.container;

import org.jetbrains.annotations.NotNull;

public interface ActorSelector <I> {
  @NotNull I selectImplementation(@NotNull ContainerPath selectionKey);
}
