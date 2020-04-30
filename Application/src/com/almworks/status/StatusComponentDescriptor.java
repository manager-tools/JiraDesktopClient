package com.almworks.status;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;

public class StatusComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer registrator) {
    registrator.registerActorClass(StatusComponentInitializer.ROLE, StatusComponentInitializer.class);
  }
}
