package com.almworks.progress;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;

/**
 * @author : Dyoma
 */
public class ProgressComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    // todo remove and check platform loader that cached component descriptor won't crash the app
//    container.registerActorClass(Role.role("progressIndicator"), ProgressIndicatorImpl.class);
  }
}
