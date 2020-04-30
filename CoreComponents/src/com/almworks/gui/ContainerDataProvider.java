package com.almworks.gui;

import com.almworks.api.container.ComponentContainer;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class ContainerDataProvider implements DataProvider {
  private final ComponentContainer myContainer;
  private final HashMap<TypedKey<?>, List<?>> myActors = Collections15.hashMap();

  public ContainerDataProvider(ComponentContainer container) {
    myContainer = container;
  }

  @Nullable
  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    T instance = myContainer.getActor(role);
    if (instance == null) {
      myActors.remove(role);
      return null;
    }
    List<T> actors = (List<T>) myActors.get(role);
    if (actors == null) {
      actors = Collections.singletonList(instance);
      myActors.put(role, actors);
    }
    return actors;
  }

  public boolean hasRole(@NotNull TypedKey<?> role) {
    return myContainer.isRegistered(role);
  }

  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    return myContainer.getRegisteredRoles();
  }

  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
  }

  @Override
  public JComponent getSourceComponent(@NotNull TypedKey<?> role, @NotNull JComponent originalComponent) {
    return originalComponent;
  }
}
