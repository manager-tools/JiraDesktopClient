package com.almworks.api.container;

import com.almworks.util.properties.Role;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ComponentContainer {
  Role<ComponentContainer> ROLE = Role.role(ComponentContainer.class);

  @Nullable <I> I getActor(@NotNull TypedKey<? extends I> role);

  @Nullable <I> I getActor(@NotNull Class<I> clazz);

  @NotNull <I> I requireActor(@NotNull TypedKey<? extends I> role);

  @NotNull <I> I requireActor(@NotNull Class<I> clazz);

  @NotNull MutableComponentContainer createSubcontainer(@NotNull String name);

  @NotNull <I> I instantiate(@NotNull Class<? extends I> concreteClass, @Nullable String selectionId);

  @NotNull <I> I instantiate(@NotNull Class<? extends I> concreteClass);

  boolean isRegistered(@NotNull TypedKey<?> role);

  @NotNull Set<Role> getRegisteredRoles();

  @Nullable
  <I> I getSelectable(@NotNull Class<I> clazz, String selectorName);
}
