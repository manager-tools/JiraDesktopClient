package com.almworks.container;

import com.almworks.api.container.ContainerPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

interface ComponentContainerParent {
  @NotNull PicoContainer getPico();

  @Nullable Selectors getSelectors();

  @Nullable ContainerPath getPath();

  @Nullable EventRouterImpl getEventRouter();
}
