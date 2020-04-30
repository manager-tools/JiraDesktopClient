package com.almworks.api.container;

import com.almworks.util.properties.Role;

public interface RootContainer extends MutableComponentContainer {
  <T> void registerStartupActor(Role<T> role, T actor);

  <T> void registerStartupActorClass(Role<T> role, Class<? extends T> actorClass);
}
