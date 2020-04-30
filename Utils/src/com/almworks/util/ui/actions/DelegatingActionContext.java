package com.almworks.util.ui.actions;

import com.almworks.util.collections.ElementVisitor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Set;

public class DelegatingActionContext implements ActionContext {
  private final ActionContext myDelegate;

  public DelegatingActionContext(ActionContext delegate) {
    myDelegate = delegate;
  }

  protected ActionContext getDelegate() {
    return myDelegate;
  }

  public void iterateDataProviders(ElementVisitor<DataProvider> iterator) {
    getDelegate().iterateDataProviders(iterator);
  }

  public void iterateComponents(ElementVisitor<JComponent> iterator) {
    getDelegate().iterateComponents(iterator);
  }

  public <C> ComponentContext<C> getComponentContext(@NotNull Class<? extends C> aClass, @NotNull TypedKey<?> role)
    throws CantPerformException
  {
    return getDelegate().getComponentContext(aClass, role);
  }

  public ActionContext childContext(DataProvider provider) {
    return getDelegate().childContext(provider);
  }

  public ActionEvent createActionEvent() {
    return getDelegate().createActionEvent();
  }

  public Component getComponent() {
    return getDelegate().getComponent();
  }

  public <T> java.util.List<T> getSourceCollection(@NotNull TypedKey<? extends T> role) throws CantPerformException {
    return getDelegate().getSourceCollection(role);
  }

  public <T> T getSourceObject(TypedKey<? extends T> role) throws CantPerformException {
    return getDelegate().getSourceObject(role);
  }

  public DataProvider findProvider(TypedKey<?> role) {
    return getDelegate().findProvider(role);
  }

  public Set<TypedKey> getAvailableRoles() {
    return getDelegate().getAvailableRoles();
  }
}
