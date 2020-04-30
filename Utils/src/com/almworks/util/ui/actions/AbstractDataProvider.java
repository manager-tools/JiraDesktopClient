package com.almworks.util.ui.actions;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author dyoma
 */
public abstract class AbstractDataProvider implements DataProvider {
  private final Collection<? extends TypedKey<?>> myRoles;
  private final Map<TypedKey<?>, SimpleModifiable> myListeners = Collections15.hashMap();

  @SuppressWarnings({"RawUseOfParameterizedType"})
  protected AbstractDataProvider(Collection<? extends TypedKey<?>> roles) {
    assert roles != null;
    assert roles.size() > 0;
    myRoles = roles;
  }

  public AbstractDataProvider(TypedKey<?> ... roles) {
    this(Arrays.asList(roles));
  }

  public boolean hasRole(@NotNull TypedKey<?> role) {
    return myRoles.contains(role);
  }


  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
    if (hasRole(role)) {
      SimpleModifiable listeners = myListeners.get(role);
      if (listeners == null) {
        listeners = new SimpleModifiable();
        myListeners.put(role, listeners);
      }
      listeners.addStraightListener(life, listener);
    }
  }

  @Nullable
  public JComponent getSourceComponent(TypedKey<?> role, JComponent originalComponent) {
    return originalComponent;
  }

  protected Collection<? extends TypedKey<?>> getRolesInternal() {
    return myRoles;
  }

  @NotNull
  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    return Collections.unmodifiableCollection(myRoles);
  }

  public void fireChanged(TypedKey<?> role) {
    SimpleModifiable listeners = myListeners.get(role);
    if (listeners == null)
      return;
    listeners.fireChanged();
  }

  protected void fireChangedAll() {
    for (Map.Entry<TypedKey<?>,SimpleModifiable> entry : myListeners.entrySet()) {
      entry.getValue().fireChanged();
    }
  }
}
