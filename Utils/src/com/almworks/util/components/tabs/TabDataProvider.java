package com.almworks.util.components.tabs;

import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
class TabDataProvider implements DataProvider {
  private final TabsManager myManager;

  public TabDataProvider(TabsManager manager) {
    myManager = manager;
  }

  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    if (!hasRole(role))
      return null;
    ContentTab tab = myManager.getSelectedTab();
    return tab != null ? Collections.singletonList((T) tab) : Collections15.<T>emptyList();
  }

  public boolean hasRole(TypedKey<?> role) {
    return role == ContentTab.DATA_ROLE;
  }

  @NotNull
  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    return Collections.singletonList(ContentTab.DATA_ROLE);
  }

  @SuppressWarnings({"RawUseOfParameterizedType"})
  public void addRoleListener(Lifespan life, TypedKey role, final com.almworks.util.collections.ChangeListener listener) {
    if (hasRole(role))
      life.add(myManager.addSelectionListener(new ChangeListener1<ContentTab>() {
        public void onChange(ContentTab object) {
          listener.onChange();
        }
      }, ThreadGate.AWT));
  }

  @Nullable
  public JComponent getSourceComponent(TypedKey<?> role, JComponent originalComponent) {
    return hasRole(role) ? myManager.getComponent() : null;
  }
}
