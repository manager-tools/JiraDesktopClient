package com.almworks.tags;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.util.properties.Role;

public class TagsComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(TagsComponentImpl.ROLE, TagsComponentImpl.class);
    container.registerActorClass(ImportTagsOnFirstRun.ROLE, ImportTagsOnFirstRun.class);
    container.registerActorClass(Role.role("tagsFeatures"), TagsFeatures.class);
  }
}
