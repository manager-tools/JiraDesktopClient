package com.almworks.container;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.util.properties.Role;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

class ComponentContainerDelegator implements ComponentContainer {
  private final @NotNull ComponentContainer myDelegate;

  public ComponentContainerDelegator(@NotNull ComponentContainer delegate) {
    myDelegate = delegate;
  }

  public <I>I getActor(TypedKey<? extends I> role) {
    return myDelegate.getActor(role);
  }

  public <I>I getActor(Class<I> clazz) {
    return myDelegate.getActor(clazz);
  }

  @NotNull
  public <I> I requireActor(@NotNull TypedKey<? extends I> role) {
    return myDelegate.requireActor(role);
  }

  @NotNull
  public <I> I requireActor(@NotNull Class<I> clazz) {
    return myDelegate.requireActor(clazz);
  }

  @NotNull
  public MutableComponentContainer createSubcontainer(String name) {
    return myDelegate.createSubcontainer(name);
  }

  @NotNull
  public <I>I instantiate(Class<? extends I> concreteClass, String selectionId) {
    return myDelegate.instantiate(concreteClass, selectionId);
  }

  @NotNull
  public <I>I instantiate(Class<? extends I> concreteClass) {
    return myDelegate.instantiate(concreteClass);
  }

  public boolean isRegistered(TypedKey<?> role) {
    return myDelegate.isRegistered(role);
  }

  @NotNull
  public Set<Role> getRegisteredRoles() {
    return myDelegate.getRegisteredRoles();
  }

  @Nullable
  public <I> I getSelectable(@NotNull Class<I> clazz, String selectorName) {
    return myDelegate.getSelectable(clazz, selectorName);
  }
}
