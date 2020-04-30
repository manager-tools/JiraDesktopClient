package com.almworks.util.ui.actions;

import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.EventObject;
import java.util.List;
import java.util.Set;

/**
 * @author : Dyoma
 */
public class DefaultActionContext implements ActionContext {
  private final ComponentVisitor myComponentIterator = new ComponentVisitor();

  @NotNull
  private final Component myComponent;
  @Nullable
  private final DataProvider myProvider;
  private ByRoleSearch myByRoleSearch = null;

  public DefaultActionContext(@NotNull EventObject eventObject) {
    //noinspection ConstantConditions
    this(Util.castNullable(JComponent.class, eventObject.getSource()));
  }

  public DefaultActionContext(@NotNull Component component) {
    this(component, null);
  }

  @Nullable
  public static <T> T getSourceObject(Component component, TypedKey<T> dataKey) {
    try {
      return new DefaultActionContext(component).getSourceObject(dataKey);
    } catch (CantPerformException e) {
      return null;
    }
  }

  private DefaultActionContext(@NotNull Component component, @Nullable DataProvider provider) {
    if (component == null) throw new NullPointerException("component");
    if (component instanceof JComponent) {
      JComponent actual = ACTUAL_COMPONENT.getClientValue((JComponent) component);
      if (actual != null)
        component = actual;
    }
    myComponent = component;
    myProvider = provider;
  }

  public void iterateDataProviders(final ElementVisitor<DataProvider> iterator) {
    if (myProvider != null && !iterator.visit(myProvider))
      return;
    iterateDataProviders(iterator, myComponentIterator.isBusy() ? null : myComponentIterator, myComponent);
  }

  private static void iterateDataProviders(ElementVisitor<DataProvider> iterator, @Nullable ComponentVisitor componentIterator, Component component) {
    if (componentIterator == null)
      componentIterator = new ComponentVisitor();
    componentIterator.setVisitor(iterator);
    iterateComponents(componentIterator, component);
    componentIterator.free();
  }

  @NotNull
  public Set<TypedKey> getAvailableRoles() {
    final Set<TypedKey> result = Collections15.hashSet();
    iterateDataProviders(new ElementVisitor<DataProvider>() {
      public boolean visit(DataProvider provider) {
        result.addAll(provider.getCurrentlyAvailableRoles());
        return true;
      }
    });
    return result;
  }

  private static boolean visitDataProvider(JComponent component, final ElementVisitor<DataProvider> iterator) {
    DataProvider dataProvider = DataProvider.DATA_PROVIDER.getClientValue(component);
    if (dataProvider == null)
      return true;
    return iterator.visit(dataProvider);
  }

  public void iterateComponents(ElementVisitor<JComponent> iterator) {
    iterateComponents(iterator, myComponent);
  }

  @NotNull
  public <C> ComponentContext<C> getComponentContext(@NotNull Class<? extends C> aClass, @NotNull TypedKey<?> role)
    throws CantPerformException
  {
    ComponentSearch<C> iterator = new ComponentSearch<C>(aClass, role);
    iterateComponents(iterator);
    C component = iterator.myComponent;
    if (component == null)
      throw new CantPerformException("Component: " + aClass + " Role: " + role);
    return new ComponentContext<C>(component);
  }

  @NotNull
  public ActionContext childContext(DataProvider provider) {
    return new DefaultActionContext(myComponent, provider);
  }

  @NotNull
  public ActionEvent createActionEvent() {
    return new ActionEvent(myComponent, ActionEvent.ACTION_PERFORMED, null);
  }

  @NotNull
  public Component getComponent() {
    return myComponent;
  }

  @NotNull
  public <T> List<T> getSourceCollection(@NotNull TypedKey<? extends T> role) throws CantPerformException {
    ByRoleSearch<T> iterator = myByRoleSearch;
    if (iterator == null) iterator = new ByRoleSearch<T>(role);
    else {
      myByRoleSearch = null;
      iterator.setRole(role);
    }
    iterateDataProviders(iterator);
    List<T> result = iterator.getCandidate();
    iterator.reset();
    myByRoleSearch = iterator;
    if (result == null) throw new CantPerformException(role.toString());
    return result;
  }

  public static <T> List<T> getSourceCollection(@NotNull TypedKey<? extends T> role, Component component) throws CantPerformException {
    ByRoleSearch<T> iterator = new ByRoleSearch<T>(role);
    iterateDataProviders(iterator, null, component);
    return iterator.getNNCandidate();
  }

  public static <T> T getSourceObject(final TypedKey<? extends T> role, Component component) throws CantPerformException {
    return CantPerformException.ensureSingleElement(getSourceCollection(role, component));
  }

  @NotNull
  public <T> T getSourceObject(final TypedKey<? extends T> role) throws CantPerformException {
    return CantPerformException.ensureSingleElement(getSourceCollection(role));
  }

  @Nullable
  public DataProvider findProvider(final TypedKey<?> role) {
    final DataProvider[] result = {null};
    iterateDataProviders(new ElementVisitor<DataProvider>() {
      public boolean visit(DataProvider provider) {
        if (result[0] != null)
          return false;
        if (!provider.hasRole(role))
          return true;
        result[0] = provider;
        return false;
      }
    });
    return result[0];
  }

  public static void iterateComponents(ElementVisitor<JComponent> iterator, Component from) {
    for (Component component = from; component != null; component = getUpperComponent(component)) {
      if (component instanceof JComponent) {
        if (!iterator.visit((JComponent) component)) {
          break;
        }
      }
    }
  }

  public static Container getUpperComponent(Component component) {
    if (component instanceof JComponent) {
      JComponent jComponent = (JComponent) component;
      JComponent next = ComponentProperty.JUMP.getClientValue(jComponent);
      if (next != null)
        return next;
    }
    return component.getParent();
  }

  private static class ByRoleSearch<T> implements ElementVisitor<DataProvider> {
    private TypedKey<? extends T> myRole;
    private List<T> myCandidate;

    public ByRoleSearch(@NotNull TypedKey<? extends T> role) {
      myRole = role;
    }

    public void setRole(TypedKey<? extends T> role) {
      myRole = role;
    }

    public boolean visit(DataProvider dataProvider) {
      if (myCandidate != null)
        return false;
      List<T> objects;
      objects = dataProvider.getObjectsByRole(myRole);
      if (objects == null)
        return true;
      myCandidate = objects;
      return false;
    }

    public List<T> getNNCandidate() throws CantPerformException {
      if (myCandidate == null)
        throw new CantPerformException(myRole.toString());
      return myCandidate;
    }

    public List<T> getCandidate() {
      return myCandidate;
    }

    public void reset() {
      myRole = null;
      myCandidate = null;
    }
  }


  private static class ComponentSearch<C> implements ElementVisitor<JComponent> {
    @NotNull
    private final Class<? extends C> myClass;
    @Nullable
    private C myComponent;
    private final TypedKey<? extends Object> myRole;

    public ComponentSearch(@NotNull Class<? extends C> aClass, @NotNull TypedKey<? extends Object> role) {
      myRole = role;
      myClass = aClass;
    }

    public boolean visit(JComponent component) {
      DataProvider provider = DataProvider.DATA_PROVIDER.getClientValue(component);
      if (provider == null || !provider.hasRole(myRole))
        return true;
      List<Object> value = provider.getObjectsByRole(myRole);
      if (value == null)
        return true;
      JComponent sourceComponent = provider.getSourceComponent(myRole, component);
      myComponent = Util.castNullable(myClass, sourceComponent);
      return false;
    }
  }


  private static class ComponentVisitor implements ElementVisitor<JComponent> {
    private ElementVisitor<DataProvider> myIterator;

    public boolean visit(JComponent component) {
      return visitDataProvider(component, myIterator);
    }

    public boolean isBusy() {
      return myIterator != null;
    }

    public void setVisitor(ElementVisitor<DataProvider> iterator) {
      myIterator = iterator;
    }

    public void free() {
      myIterator = null;
    }
  }
}
