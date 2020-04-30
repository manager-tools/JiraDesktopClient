package com.almworks.loadstatus;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.util.properties.Role;

public class LoadStatusDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer registrator) {
    registrator.registerActorClass(SplashHider.ROLE, SplashHider.class);
    registrator.registerActorClass(Role.anonymous(), ActionsWatcher.class);
  }
}
