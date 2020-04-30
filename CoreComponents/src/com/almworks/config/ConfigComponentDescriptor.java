package com.almworks.config;

import com.almworks.api.config.MiscConfig;
import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.util.config.Configuration;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ConfigComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerSelector(Configuration.class, ConfigComponent.class);
    container.registerActorClass(MiscConfig.ROLE, MiscConfigImpl.class);
  }
}
