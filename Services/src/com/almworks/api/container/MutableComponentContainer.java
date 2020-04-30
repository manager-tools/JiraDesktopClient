package com.almworks.api.container;

import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;

public interface MutableComponentContainer extends ComponentContainer {
  Role<MutableComponentContainer> ROLE = Role.role(MutableComponentContainer.class);

  <I, C extends I> void registerActor(@NotNull Role<I> role, @NotNull C actor);

  void registerActor(@NotNull Object actor);

  <I> void registerActorClass(@NotNull Role<I> role, @NotNull Class<? extends I> actorClass);

  <I> void registerSelector(@NotNull Class<I> actorInterface, @NotNull Class<? extends ActorSelector<I>> selectorClass);

  <I, C extends I> void reregisterActor(@NotNull Role<I> role, @NotNull C actor);

  void start();

  void startWithDebugging();

  void stop();
}
