package com.almworks.recentitems;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.recentitems.gui.RecentItemsActions;
import com.almworks.recentitems.gui.RecentItemsComponent;
import com.almworks.recentitems.gui.RecentItemsDialog;
import com.almworks.recentitems.gui.RecentItemsLoader;
import com.almworks.util.properties.Role;

public class RecentItemsDescriptor implements ComponentDescriptor {
  @Override
  public void registerActors(RootContainer container) {
    container.registerActorClass(RecentItemsService.ROLE, RecentItemsServiceImpl.class);
    container.registerActorClass(RecentItemsLoader.ROLE, RecentItemsLoader.class);
    container.registerActorClass(Role.anonymous(), RecentItemsComponent.class);
    container.registerActorClass(Role.anonymous(), RecentItemsActions.class);
    container.registerActorClass(Role.anonymous(), RecentItemsDialog.class);
  }
}
