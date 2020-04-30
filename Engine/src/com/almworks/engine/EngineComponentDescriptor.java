package com.almworks.engine;

import com.almworks.api.container.RootContainer;
import com.almworks.api.engine.ConnectionIconsManager;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.GlobalLoginController;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.settings.engine.EngineActions;
import com.almworks.util.properties.Role;

/**
 * :todoc:
 *
 * @author sereda
 */
public class EngineComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer registrator) {
    // The name of the role is "engine" so that the class receives configuration for tag "engine" (for backwards compatilility with old config files)
    registrator.registerActorClass(Role.role("engine"), ConnectionManagerImpl.class);
    // This role has the name "Engine", so if it used configuration, it would rewrite the ConnectionManager's. It does not now; beware if you add!
    registrator.registerActorClass(Engine.ROLE, EngineImpl.class);
    registrator.registerActorClass(Role.role("engineActions"), EngineActions.class);
    registrator.registerActorClass(PeriodicalSynchronization.ROLE, PeriodicalSynchronization.class);
    registrator.registerActorClass(ConnectionIconsManager.ROLE, ConnectionIconsManager.class);
    registrator.registerActorClass(GlobalLoginController.ROLE, GlobalLoginController.class);
  }
}
