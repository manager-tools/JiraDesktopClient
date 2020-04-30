package com.almworks.util.ui.actions;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author dyoma
 */
public class CompositeDataProvider implements DataProvider {
  private final List<DataProvider> myProviders = Collections15.arrayList();

  @Nullable
  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    assert checkSingleProvider(role);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myProviders.size(); i++) {
      DataProvider provider = myProviders.get(i);
      if (!provider.hasRole(role))
        continue;
      List<T> values = provider.getObjectsByRole(role);
      if (values != null)
        return values;
    }
    return null;
  }

  public boolean hasRole(TypedKey<?> role) {
    for (int i = 0; i < myProviders.size(); i++) {
      if (myProviders.get(i).hasRole(role)) return true;
    }
    return false;
  }

  @NotNull
  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    Collection<DataProvider> providers = myProviders;
    return getCurrentlyAvailableRoles(providers);
  }

  @NotNull
  public static Collection<? extends TypedKey> getCurrentlyAvailableRoles(Collection<DataProvider> providers) {
    int size = providers.size();
    if (size == 0)
      return Collections15.emptyCollection();
    if (size == 1)
      return providers.iterator().next().getCurrentlyAvailableRoles();
    Set<TypedKey> result = Collections15.hashSet();
    for (DataProvider provider : providers) {
      result.addAll(provider.getCurrentlyAvailableRoles());
    }
    return result;
  }

  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
    assert checkSingleProvider(role);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myProviders.size(); i++) {
      DataProvider provider = myProviders.get(i);
      if (provider.hasRole(role)) {
        provider.addRoleListener(life, role, listener);
        return;
      }
    }
    assert false : role;
  }

  @Nullable
  public JComponent getSourceComponent(@NotNull TypedKey<?> role, @NotNull JComponent originalComponent) {
    for (DataProvider provider : myProviders) {
      if (!provider.hasRole(role))
        continue;
      if (provider.getObjectsByRole(role) == null)
        continue;
      return provider.getSourceComponent(role, originalComponent);
    }
    return null;
  }

  public void addProvider(DataProvider provider) {
    assert provider != null;
    myProviders.add(provider);
  }

  private boolean checkSingleProvider(TypedKey role) {
    DataProvider first = null;
    for (DataProvider provider : myProviders) {
      if (provider.hasRole(role) && provider.getObjectsByRole(role) != null) {
        assert first == null : "First: " + first + ", second:" + provider + " role: " + role;
        first = provider;
      }
    }
    return true;
  }

  private <T> T findProvider(Class<T> aClass) {
    return (T) Condition.<Object>isInstance(aClass).detect(myProviders);
  }

  public Collection<? extends DataProvider> getProviders() {
    return Collections.unmodifiableCollection(myProviders);
  }

  @Nullable
  public static <T> T findProvider(Class<T> aClass, JComponent component) {
    DataProvider provider = DataProvider.DATA_PROVIDER.getClientValue(component);
    if (provider == null)
      return null;
    T cast = Util.castNullable(aClass, provider);
    if (cast != null) return cast;
    if (provider instanceof CompositeDataProvider)
      return ((CompositeDataProvider) provider).findProvider(aClass);
    return null;
  }

  public void removeProviders(Condition<DataProvider> condition) {
    for (int i = 0; i < myProviders.size(); i++) {
      DataProvider provider = myProviders.get(i);
      if (condition.isAccepted(provider))
        myProviders.remove(i);
    }
  }
}
