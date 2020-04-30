package com.almworks.util.components;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.ui.actions.CompositeDataProvider;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.*;

// todo 815 - replace with Global
public class TabbedPaneDataProvider extends SimpleModifiable implements DataProvider {
  private final Map<Integer, JComponent> myTabbedProviders = Collections15.hashMap();
  private final SingleSelectionModel myTabbedPaneModel;
  private boolean myConstructionClosed = false;

  private static final Convertor<JComponent, DataProvider> DATA_PROVIDER_FROM_COMPONENT = new Convertor<JComponent, DataProvider>() {
    public DataProvider convert(JComponent component) {
      return DATA_PROVIDER.getClientValue(component);
    }
  };

  private TabbedPaneDataProvider(JTabbedPane tabbedPane) {
    myTabbedPaneModel = tabbedPane.getModel();
  }

  public static TabbedPaneDataProvider install(JTabbedPane tabbedPane) {
    TabbedPaneDataProvider provider = new TabbedPaneDataProvider(tabbedPane);
    DATA_PROVIDER.putClientValue(tabbedPane, provider);
    return provider;
  }

  public TabbedPaneDataProvider provide(int tabIndex, JComponent component) {
    if (myConstructionClosed)
      throw new IllegalStateException("data provider construction closed");
    myTabbedProviders.put(tabIndex, component);
    return this;
  }

  public void addRoleListener(Lifespan life, TypedKey role, final ChangeListener listener) {
    myConstructionClosed = true;
    //noinspection ForLoopReplaceableByForEach
    for (Iterator<JComponent> ii = myTabbedProviders.values().iterator(); ii.hasNext();) {
      JComponent component = ii.next();
      DataProvider provider = DATA_PROVIDER.getClientValue(component);
      if (provider != null && provider.hasRole(role))
        provider.addRoleListener(life, role, listener);
    }
    final javax.swing.event.ChangeListener modelListener = new javax.swing.event.ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        listener.onChange();
      }
    };
    myTabbedPaneModel.addChangeListener(modelListener);
    life.add(new Detach() {
      protected void doDetach() {
        myTabbedPaneModel.removeChangeListener(modelListener);
      }
    });
  }

  @Nullable
  public JComponent getSourceComponent(@NotNull TypedKey<?> role, @NotNull JComponent originalComponent) {
    JComponent component = myTabbedProviders.get(myTabbedPaneModel.getSelectedIndex());
    DataProvider provider = DATA_PROVIDER.getClientValue(component);
    return provider != null ? provider.getSourceComponent(role, component) : null;
  }

  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    myConstructionClosed = true;
    JComponent component = myTabbedProviders.get(myTabbedPaneModel.getSelectedIndex());
    DataProvider provider = DATA_PROVIDER.getClientValue(component);
    if (provider != null && provider.hasRole(role))
      return provider.getObjectsByRole(role);
    else
      return null;
  }

  public boolean hasRole(TypedKey<?> role) {
    myConstructionClosed = true;
    for (Iterator<JComponent> ii = myTabbedProviders.values().iterator(); ii.hasNext();) {
      JComponent component = ii.next();
      DataProvider provider = DATA_PROVIDER.getClientValue(component);
      if (provider != null && provider.hasRole(role))
        return true;
    }
    return false;
  }

  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    Set<DataProvider> providers = DATA_PROVIDER_FROM_COMPONENT.collectSet(myTabbedProviders.values());
    providers.remove(null); // could be collected
    return CompositeDataProvider.getCurrentlyAvailableRoles(providers);
  }
}
