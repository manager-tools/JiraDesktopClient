package com.almworks.util.ui.actions;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.ui.ComponentProperty;
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
public interface DataProvider {
  ComponentPropertyKey DATA_PROVIDER = new ComponentPropertyKey();

  @Nullable
    <T> List<T> getObjectsByRole(TypedKey<? extends T> role);

  boolean hasRole(@NotNull TypedKey<?> role);

  void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener);

  /**
   * Provides actual data source component. If the provider delegates to providers in other components (such as globalize provider) this method returns the actual source component.<br>
   * The implementation may assume that it does actually provide the role, so provider may return not null value even if it does not provide any value for the role.
   * @param role get actual source for role
   * @param originalComponent the component where this provider found - default for providers that provides own data only
   * @return component that has actual data provider for the role or null if no actual provider for the role exists now
   */
  @Nullable
  JComponent getSourceComponent(@NotNull TypedKey<?> role, @NotNull JComponent originalComponent);

  @NotNull
  Collection<? extends TypedKey> getCurrentlyAvailableRoles();

  /**
   * @deprecated
   */
  class NoDataException extends Exception {
    public NoDataException() {
    }

    public NoDataException(String message) {
      super(message);
    }
  }

  public static class ComponentPropertyKey extends ComponentProperty.Simple<DataProvider> {
    public ComponentPropertyKey() {
      super("dataProvider");
    }

    public void putClientValue(JComponent component, DataProvider provider) {
      DataProvider prev = getClientValue(component);
      if (prev != null) {
        CompositeDataProvider composite;
        if (prev instanceof CompositeDataProvider)
          composite = (CompositeDataProvider) prev;
        else {
          composite = new CompositeDataProvider();
          composite.addProvider(prev);
        }
        composite.addProvider(provider);
        provider = composite;
      }
      super.putClientValue(component, provider);
    }

    public void removeAllProviders(JComponent component) {
      component.putClientProperty(this, null);
    }

    @NotNull
    public Collection<? extends DataProvider> getAllProviders(JComponent component) {
      DataProvider provider = DATA_PROVIDER.getClientValue(component);
      if (provider == null)
        return Collections15.emptyCollection();
      if (!(provider instanceof CompositeDataProvider))
        return Collections.singleton(provider);
      return ((CompositeDataProvider) provider).getProviders();
    }

    public void removeProviders(JComponent component, Condition<DataProvider> condition) {
      DataProvider provider = DATA_PROVIDER.getClientValue(component);
      if (provider == null)
        return;
      if (!(provider instanceof CompositeDataProvider)) {
        if (condition.isAccepted(provider))
          removeAllProviders(component);
      } else
        ((CompositeDataProvider) provider).removeProviders(condition);
    }
  }
}
