package com.almworks.search;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.api.search.types.SearchWords;
import com.almworks.util.properties.Role;

public class TextSearchDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer registrator) {
    registrator.registerActorClass(TextSearchImpl.ROLE, TextSearchImpl.class);
    registrator.registerActorClass(TextSearchComponent.ROLE, TextSearchComponent.class);
    registrator.registerActorClass(Role.anonymous(), SearchWords.class);
  }
}
