package com.almworks.syncreg;

import com.almworks.api.container.RootContainer;
import com.almworks.api.engine.Engine;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.util.DBNamespace;

public class SyncRegistryComponent implements ComponentDescriptor {
  public static final DBNamespace NS = Engine.NS.subNs("syncRegistry");
  public static final DBIdentifiedObject SYNC_HOLDER = NS.object("syncHolder");

  public void registerActors(RootContainer container) {
    container.registerActorClass(SyncRegistry.ROLE, SyncRegistryImpl.class);
  }
}
