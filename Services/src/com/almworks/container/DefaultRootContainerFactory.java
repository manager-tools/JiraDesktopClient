package com.almworks.container;

import com.almworks.api.container.RootContainer;
import com.almworks.api.container.RootContainerFactory;

public class DefaultRootContainerFactory extends RootContainerFactory {
  public RootContainer create() {
    return new RootContainerImpl();
  }
}
