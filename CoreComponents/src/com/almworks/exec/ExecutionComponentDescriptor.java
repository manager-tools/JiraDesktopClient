package com.almworks.exec;

import com.almworks.api.container.RootContainer;
import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.exec.ExceptionsManager;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.util.properties.Role;

/**
 * @author : Dyoma
 */
public class ExecutionComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerStartupActorClass(ApplicationManager.ROLE, ApplicationManagerImpl.class);
    container.registerStartupActorClass(ExceptionsManager.ROLE, ExceptionsManagerImpl.class);

    container.registerActorClass(Role.role("applicationManagerActions"), ApplicationManagerActions.class);
  }
}
