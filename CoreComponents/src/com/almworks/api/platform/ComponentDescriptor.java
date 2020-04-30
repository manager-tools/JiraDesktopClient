package com.almworks.api.platform;

import com.almworks.api.container.RootContainer;

public interface ComponentDescriptor {
  void registerActors(RootContainer container);
}
