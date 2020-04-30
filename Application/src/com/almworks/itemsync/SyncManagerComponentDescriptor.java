package com.almworks.itemsync;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.items.sync.SyncManager;

public class SyncManagerComponentDescriptor implements ComponentDescriptor {
  @Override
  public void registerActors(RootContainer container) {
    container.registerActorClass(SyncManager.ROLE, ApplicationSyncManager.class);
    container.registerActorClass(SyncManager.MODIFIABLE, SyncManagerModifiable.class);
  }
}
