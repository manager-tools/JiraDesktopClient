package com.almworks.extservice;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.integrate.IntegrateWithIDEAction;

public class ExternalApiServiceDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(ExternalApiService.ROLE, ExternalApiService.class);
    container.registerActorClass(IntegrateWithIDEAction.ROLE, IntegrateWithIDEAction.class);
    container.registerActorClass(ExternalUriService.ROLE, ExternalUriService.class);
  }
}
