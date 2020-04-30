package com.almworks.util.ui.actions;

import com.almworks.util.collections.ChangeListener;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ComponentDelegateDataProvider implements DataProvider {
  private final Set<TypedKey<?>> myRoles;
  private final DefaultActionContext myContext;

  public ComponentDelegateDataProvider(JComponent sourceComponent) {
    myRoles = null;
    myContext = new DefaultActionContext(sourceComponent);
  }

  public ComponentDelegateDataProvider(JComponent sourceComponent, TypedKey<?> ... roles) {
    myRoles = roles == null ? null : Collections15.hashSet(roles);
    myContext = new DefaultActionContext(sourceComponent);
  }

  @Nullable
  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    if (myRoles != null && !myRoles.contains(role)) {
      return null;
    }
    try {
      return myContext.getSourceCollection(role);
    } catch (CantPerformException e) {
      return Collections15.emptyList();
    }
  }

  public boolean hasRole(@NotNull TypedKey<?> role) {
    DataProvider delegate = getDelegate(role);
    return delegate != null;
  }

  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
    DataProvider delegate = getDelegate(role);
    if (delegate != null)
      delegate.addRoleListener(life, role, listener);
  }

  @Nullable
  public JComponent getSourceComponent(@NotNull TypedKey<?> role, @NotNull JComponent originalComponent) {
    if (myRoles != null && !myRoles.contains(role)) return null;
    try {
      return myContext.getComponentContext(JComponent.class, role).getComponent();
    } catch (CantPerformException e) {
      return null;
    }
  }

  @NotNull
  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    Set<TypedKey> roles = myContext.getAvailableRoles();
    if (myRoles != null)
      roles.retainAll(myRoles);
    return roles;
  }

  @Nullable
  private DataProvider getDelegate(TypedKey<?> role) {
    return myRoles == null || myRoles.contains(role) ? myContext.findProvider(role) : null;
  }
}
