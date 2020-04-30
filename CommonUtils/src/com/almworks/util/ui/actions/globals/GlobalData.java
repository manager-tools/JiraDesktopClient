package com.almworks.util.ui.actions.globals;

import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public class GlobalData extends ComponentProperty<List<? extends TypedKey<?>>> {
  private static final ComponentProperty<List<TypedKey<?>>> SET_KEY = ComponentProperty.createProperty("globalsSet");
  private static final ComponentProperty<List<TypedKey<?>>> UNMODIFIABLE = ComponentProperty.createProperty("globalSet.unmodifiable");
  public static final GlobalData KEY = new GlobalData();

  protected GlobalData() {
    super("globals");
  }

  public void addClientValue(JComponent component, TypedKey<?> ... roles) {
    putClientValue(component, Arrays.asList(roles));
  }

  public void replaceClientValue(JComponent component, Collection<TypedKey<?>> oldRoles, Collection<TypedKey<?>> newRoles) {
    List<TypedKey<?>> values = getOrCreateRoleList(component);
    values.removeAll(oldRoles);
    addNewRoles(values, newRoles);
    fireGlobalsChanged(component);
  }

  /**
   * @deprecated
   * Replaced with {@link #removeAll(javax.swing.JComponent, java.util.Collection< org.almworks.util.TypedKey >)}
   */
  @Deprecated
  public void removeAll(JComponent component) {
    List<TypedKey<?>> roles = SET_KEY.getClientValue(component);
    if (roles != null)
      roles.clear();
    SET_KEY.putClientValue(component, null);
    UNMODIFIABLE.putClientValue(component, null);
    fireGlobalsChanged(component);
  }

  public void removeAll(JComponent component, Collection<? extends TypedKey<?>> roles) {
    List<TypedKey<?>> values = SET_KEY.getClientValue(component);
    if (values == null) {
      assert UNMODIFIABLE.getClientValue(component) == null;
      return;
    }
    values.removeAll(roles);
    fireGlobalsChanged(component);
  }

  public static void fireGlobalsChanged(@Nullable Component component) {
    if (component == null)
      return;
    DataRoot root = DataRoot.KEY.searchAncestors(component.getParent());
    if (root != null)
      root.onGlobalsChanged((JComponent) component);
  }

  public void putClientValue(JComponent component, List<? extends TypedKey<?>> value) {
    List<TypedKey<?>> datas = getOrCreateRoleList(component);
    addNewRoles(datas, value);
    fireGlobalsChanged(component);
  }

  private List<TypedKey<?>> getOrCreateRoleList(JComponent component) {
    List<TypedKey<?>> datas = SET_KEY.getClientValue(component);
    if (datas == null) {
      datas = Collections15.arrayList(4);
      SET_KEY.putClientValue(component, datas);
      UNMODIFIABLE.putClientValue(component, Collections.unmodifiableList(datas));
    }
    return datas;
  }

  private void addNewRoles(List<TypedKey<?>> datas, Collection<? extends TypedKey<?>> newRoles) {
    for (TypedKey<?> key : newRoles) {
      if (!datas.contains(key))
        datas.add(key);
    }
  }

  @Nullable
  public List<? extends TypedKey<?>> getClientValue(JComponent component) {
    List<TypedKey<?>> datas = UNMODIFIABLE.getClientValue(component);
    return datas != null ? datas : null;
  }
}
