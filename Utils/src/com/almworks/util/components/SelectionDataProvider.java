package com.almworks.util.components;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author dyoma
 */
public class SelectionDataProvider implements DataProvider {
  @NotNull
  private final SelectionAccessor<?> mySelection;
  private final ACollectionComponent<?> myComponent;
  private final Set<DataRole<?>> myRoles = Collections15.hashSet();
  private final SimpleModifiable mySelectionModifiable = new SimpleModifiable();

  protected SelectionDataProvider(@NotNull SelectionAccessor<?> selection, ACollectionComponent<?> component) {
    mySelection = selection;
    mySelection.addSelectedItemsListener(mySelectionModifiable);
    myComponent = component;
  }

  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    if (!(role instanceof DataRole<?>))
      return null;
    DataRole<? extends T> dataRole = (DataRole<? extends T>) role;
    if (dataRole instanceof SelectionRole) {
      SelectionRole<T> selectionRole = (SelectionRole<T>) dataRole;
      dataRole = selectionRole.getDataRole();
      if (myRoles.contains(dataRole)) {
        Object data = selectionRole.extractData(mySelection);
        if (data == null) {
          return Collections15.emptyList();
        } else {
          return provideData(dataRole, Collections15.list(data));
        }
      }
    } else if (myRoles.contains(dataRole)) {
      List<?> items = mySelection.getSelectedItems();
      return provideData(dataRole, items);
    }
    return null;
  }

  @Nullable
  protected <T> List<T> provideData(DataRole<? extends T> dataRole, List<?> items) {
    return DataProviders.ensureMatchesAll(dataRole, items);
  }

  public boolean hasRole(@NotNull TypedKey<?> role) {
    //noinspection SuspiciousMethodCalls
    if (myRoles.contains(role))
      return true;
    if (role instanceof SelectionRole) {
      return myRoles.contains(((SelectionRole) role).getDataRole());
    }
    return false;
  }

  @NotNull
  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    // todo shall we add SelectionRoles ?
    return Collections.unmodifiableSet(myRoles);
  }

  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
    if (!hasRole(role)) {
      assert false : role;
      return;
    }
    mySelectionModifiable.addStraightListener(life, listener);
  }

  @Nullable
  public JComponent getSourceComponent(TypedKey<?> role, JComponent originalComponent) {
    return myComponent.toComponent();
  }

  static void installTo(ACollectionComponent<?> component) {
    replaceProvider(component, new SelectionDataProvider(component.getSelectionAccessor(), component));
  }

  public static void replaceProvider(ACollectionComponent<?> component, SelectionDataProvider dataProvider) {
    DATA_PROVIDER.removeProviders(component.toComponent(),
      Condition.<DataProvider>isInstance(SelectionDataProvider.class));
    SELECTION_DATA.putClientValue(component.toComponent(), dataProvider);
  }

  public static void setRoles(ACollectionComponent<?> component, Collection<? extends DataRole<?>> roles) {
    assert !component.toComponent().isDisplayable() : component;
    SelectionDataProvider provider = SELECTION_DATA.getClientValue(component.toComponent());
    assert provider != null : component;
    provider.replaceRoles(roles);
  }

  public static void addRoles(ACollectionComponent<?> component, Collection<? extends DataRole<?>> roles) {
    assert !component.toComponent().isDisplayable() : component;
    SelectionDataProvider provider = SELECTION_DATA.getClientValue(component.toComponent());
    assert provider != null : component;
    provider.addRoles(roles);
  }

  void replaceRoles(Collection<? extends DataRole<?>> roles) {
    for (DataRole<?> role : roles) {
      assert !(role instanceof SelectionRole) : role;
    }
    myRoles.clear();
    myRoles.addAll(roles);
  }

  void addRoles(Collection<? extends DataRole<?>> roles) {
    myRoles.addAll(roles);
  }

  public static void setRoles(ACollectionComponent<?> component, DataRole<?>[] roles) {
    setRoles(component, roles == null ? Collections15.<DataRole<?>>emptyList() : Arrays.asList(roles));
  }

  public static void addRoles(ACollectionComponent<?> component, DataRole<?>[] roles) {
    if (roles != null)
      addRoles(component, Arrays.asList(roles));
  }

  private static final ComponentProperty<SelectionDataProvider> SELECTION_DATA =
    ComponentProperty.delegate(DATA_PROVIDER, SelectionDataProvider.class);
}
