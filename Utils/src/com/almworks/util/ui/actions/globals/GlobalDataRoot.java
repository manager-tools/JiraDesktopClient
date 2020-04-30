package com.almworks.util.ui.actions.globals;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author dyoma
 */
public class GlobalDataRoot implements DataRoot, GlobalDataWatcher.WatcherCallback {
  private final GlobalDataWatcher myWatcher = new GlobalDataWatcher(this);
  private final List<ActionCounterpart> myCounterpartPool = Collections15.arrayList();
  private final HashMap<TypedKey<?>, RoleSubscribtion> mySubscriptions = Collections15.hashMap();
  private ActionCounterpart myLastAction = null;


  public GlobalDataRoot(JComponent root) {
    myWatcher.watch(root);
    DataRoot.KEY.putClientValue(root, this);
  }

  private static void fireAll(@Nullable Collection<? extends ChangeListener> listeners) {
    if (listeners == null)
      return;
    for (ChangeListener listener : listeners) {
      listener.onChange();
    }
  }

  public static void install(JComponent component) {
    GlobalDataRoot root = new GlobalDataRoot(component);
    DataProvider provider = root.createProvider();
    DataProvider.DATA_PROVIDER.putClientValue(component, provider);
  }

  private DataProvider createProvider() {
    return new Provider();
  }

  public void onGlobalsChanged(JComponent component) {
    myWatcher.reviewDataAt(component);
  }

  public void onDataAppears(@NotNull Collection<? extends TypedKey<?>> roles) {
    myLastAction = null;
    for (TypedKey<?> role : roles) {
      RoleSubscribtion subscribtion = mySubscriptions.remove(role);
      if (subscribtion != null) subscribtion.endLife();
    }
  }

  public void onDataDisappears(@NotNull Collection<? extends TypedKey<?>> roles) {
    myLastAction = null;
    for (TypedKey<?> role : roles) {
      RoleSubscribtion subscribtion = mySubscriptions.remove(role);
      if (subscribtion != null) subscribtion.endLife();
    }
  }

  private class Provider implements DataProvider, Function<TypedKey<?>, DataProvider> {
    @Nullable
    public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
      DataProvider provider = myWatcher.getProvider(role);
      if (provider == null)
        return null;
      return provider.getObjectsByRole(role);
    }

    public boolean hasRole(@NotNull TypedKey<?> role) {
      return true;
    }

    @NotNull
    public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
      return myWatcher.getRoles();
    }

    public void addRoleListener(Lifespan life, final TypedKey role, ChangeListener listener) {
      ActionCounterpart action = ActionCounterpart.createIfNotSame(myLastAction, life, listener, myCounterpartPool);
      if (action == null) return;
      myLastAction = action;
      RoleSubscribtion subscribtion = RoleSubscribtion.getOrCreate(role, mySubscriptions, this);
      subscribtion.subscribe(action);
    }

    @Nullable
    public JComponent getSourceComponent(TypedKey<?> role, JComponent originalComponent) {
      JComponent component = myWatcher.getComponent(role);
      if (component == null) return null;
      DataProvider provider = DataProvider.DATA_PROVIDER.getClientValue(component);
      return provider != null ? provider.getSourceComponent(role, component) : null;
    }

    public DataProvider invoke(TypedKey<?> role) {
      return myWatcher.getProvider(role);
    }
  }
}
