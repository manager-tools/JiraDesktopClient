package com.almworks.engine.items;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.items.api.Database;

@SuppressWarnings({"UnusedDeclaration"})
public class ItemStorageDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(Database.ROLE, DatabaseComponent.class);
    container.registerActor(DatabaseCheck.DUMMY);
  }
}
