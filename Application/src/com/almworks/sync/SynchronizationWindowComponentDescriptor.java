package com.almworks.sync;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.api.sync.SynchronizationWindow;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SynchronizationWindowComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer registrator) {
    registrator.registerActorClass(SynchronizationWindow.ROLE, SynchronizationWindowImpl.class);
  }
}
