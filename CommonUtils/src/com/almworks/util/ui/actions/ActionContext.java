package com.almworks.util.ui.actions;

import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Set;

/**
 * @author dyoma
 */
public interface ActionContext {
  /**
   * For component wrappers. Refers to descendant component which user actually works with.
   */
  ComponentProperty<JComponent> ACTUAL_COMPONENT = ComponentProperty.createProperty("actualComponent");

  void iterateDataProviders(ElementVisitor<DataProvider> iterator);

  void iterateComponents(ElementVisitor<JComponent> iterator);

  @NotNull
  <C> ComponentContext<C> getComponentContext(@NotNull Class<? extends C> aClass, @NotNull TypedKey<?> role)
    throws CantPerformException;

  @NotNull
  ActionContext childContext(DataProvider provider);

  @NotNull
  ActionEvent createActionEvent();

  @NotNull
  Component getComponent();

  @NotNull
  <T> java.util.List<T> getSourceCollection(@NotNull TypedKey<? extends T> role) throws CantPerformException;

  @NotNull
  <T> T getSourceObject(TypedKey<? extends T> role) throws CantPerformException;

  @Nullable
  DataProvider findProvider(TypedKey<?> role);

  @NotNull
  Set<TypedKey> getAvailableRoles();
}
