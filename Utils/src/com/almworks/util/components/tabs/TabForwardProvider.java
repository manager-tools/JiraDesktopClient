package com.almworks.util.components.tabs;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.commons.Function;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.globals.*;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author dyoma
 */
class TabForwardProvider implements DataProvider, DataRoot {
  private final TabsManager myManager;
  private final GlobalDataCache myCache = new GlobalDataCache();
  private List<TypedKey<?>> myTabGlobalRoles = Collections15.emptyList();

  private TabForwardProvider(TabsManager manager) {
    myManager = manager;
    myManager.addSelectionListener(new ChangeListener1<ContentTab>() {
      public void onChange(ContentTab tab) {
        replaceGlobalData();
      }
    }, ThreadGate.AWT);
    myManager.getComponent().addContainerListener(new DescendantGlobalDataListener() {
      protected void subTreeAdded(Container container) {
        if (isDecenandOfSelected(container) && DataGlobalizationUtil.isAncestorOfDataHost(container)) replaceGlobalData();
      }

      protected void subTreeRemoved(Container container) {
        if (isDecenandOfSelected(container) && DataGlobalizationUtil.isAncestorOfDataHost(container)) replaceGlobalData();
      }
    });
  }

  private void replaceGlobalData() {
    List<TypedKey<?>> oldRoles = myTabGlobalRoles;
    JComponent selected = myManager.getSelectedComponent();
    if (selected != null) {
      myTabGlobalRoles = Collections15.arrayList(getDataHosts(selected).keySet());
      GlobalData.KEY.replaceClientValue(myManager.getComponent(), oldRoles, myTabGlobalRoles);
    } else {
      GlobalData.KEY.removeAll(myManager.getComponent(), myTabGlobalRoles);
      myTabGlobalRoles = Collections15.emptyList();
    }
    myCache.rebuild(selected);
  }

  @NotNull
  private Map<TypedKey<?>, JComponent> getDataHosts(JComponent tabComponent) {
    Map<TypedKey<?>, JComponent> hosts = Collections15.hashMap();
    DataGlobalizationUtil.collectDataHosts(tabComponent, hosts);
    DataGlobalizationUtil.appendDataHost(tabComponent, hosts);
    return hosts;
  }

  public static void install(TabsManager manager) {
    TabForwardProvider provider = new TabForwardProvider(manager);
    JComponent component = manager.getComponent();
    DataProvider.DATA_PROVIDER.putClientValue(component, provider);
    DataRoot.KEY.putClientValue(component, provider);
  }

  @Nullable
  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    return myCache.getObjectsByRole(role);
  }

  @NotNull
  public Collection<? extends TypedKey<?>> getCurrentlyAvailableRoles() {
    ContentTab tab = myManager.getSelectedTab();
    if (tab == null)
      return Collections15.emptySet();
    Map<TypedKey<?>, JComponent> hosts = getDataHosts(tab.getJComponent());
    return Collections.unmodifiableSet(hosts.keySet());
  }

  public boolean hasRole(TypedKey<?> role) {
    return true;
  }

  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
    myCache.addRoleListener(life, role, listener);
  }

  @Nullable
  public JComponent getSourceComponent(TypedKey<?> role, JComponent originalComponent) {
    return myCache.getSourceComponent(role);
  }

  public void onGlobalsChanged(JComponent component) {
    if (isDecenandOfSelected(component)) replaceGlobalData();
  }

  private boolean isDecenandOfSelected(Component component) {
    JComponent tabComponent = myManager.getSelectedComponent();
    return tabComponent != null && SwingTreeUtil.isAncestor(tabComponent, component);
  }

  private class GlobalDataCache implements Function<TypedKey<?>, DataProvider> {
    private final HashMap<TypedKey<?>, JComponent> myProviders = Collections15.hashMap();
    private final List<ActionCounterpart> myCounterpartPool = Collections15.arrayList();
    private final HashMap<TypedKey<?>, RoleSubscribtion> mySubscriptions = Collections15.hashMap();
    private ActionCounterpart myLastAction = null;

    public void rebuild(JComponent ancestor) {
      myProviders.clear();
      myLastAction = null;
      for (RoleSubscribtion subscribtion : mySubscriptions.values()) subscribtion.endLife();
      mySubscriptions.clear();
      if (ancestor != null) {
        DataGlobalizationUtil.collectDataHosts(ancestor, myProviders);
        DataGlobalizationUtil.appendDataHost(ancestor, myProviders);
      }
    }

    public JComponent getSourceComponent(TypedKey<?> role) {
      return myProviders.get(role);
    }

    public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
      DataProvider provider = getProvider(role);
      return provider != null ? provider.getObjectsByRole(role) : null;
    }

    private <T> DataProvider getProvider(TypedKey<? extends T> role) {
      JComponent component = myProviders.get(role);
      DataProvider provider = component != null ? DataProvider.DATA_PROVIDER.getClientValue(component) : null;
      return provider;
    }

    public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
      ActionCounterpart counterpart = ActionCounterpart.createIfNotSame(myLastAction, life, listener, myCounterpartPool);
      if (counterpart == null) return;
      myLastAction = counterpart;
      RoleSubscribtion subscribtion = getSubscribtion(role);
      subscribtion.subscribe(counterpart);
//
//      DataProvider provider = getProvider(role);
//      if (provider != null) DataListenerDetach.addRoleListener(life, myRoleWatchers, provider, role, listener);
//      else life.add(myMissingListeners.addReturningDetach(role, listener));
    }

    public Collection<? extends TypedKey<?>> getCurrentlyAvailableRoles() {
      return Collections.unmodifiableSet(myProviders.keySet());
    }

    public DataProvider invoke(TypedKey<?> role) {
      return getProvider(role);
    }

    private RoleSubscribtion getSubscribtion(TypedKey<?> role) {
      return RoleSubscribtion.getOrCreate(role, mySubscriptions, this);
    }
  }
}
