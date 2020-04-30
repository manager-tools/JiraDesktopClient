package com.almworks.export;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;

public class ExportComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(ExportAction.ROLE, ExportAction.class);
  }
}
