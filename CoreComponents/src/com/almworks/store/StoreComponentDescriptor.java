package com.almworks.store;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.api.store.Store;
import com.almworks.util.properties.Role;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StoreComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerSelector(Store.class, StoreComponent.class);
    container.registerActorClass(Role.anonymous(), SQLiteChecker.class);
  }
}
