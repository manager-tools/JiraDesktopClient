package com.almworks.items.gui.meta;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.items.cache.DBImage;

public class MetaComponentDescriptor implements ComponentDescriptor {
  @Override
  public void registerActors(RootContainer container) {
    container.registerActorClass(DBImage.ROLE, DBImage.class);
    container.registerActorClass(GuiFeaturesManager.ROLE, GuiFeaturesManager.class);
    container.registerActor(MetaVersionChecker.INSTANCE);
  }
}
