package com.almworks.util.ui.actions;

import org.almworks.util.TypedKey;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author dyoma
 */
public class ComponentContext <C> {
  private final C myComponent;

  public ComponentContext(C component) {
    assert component instanceof JComponent : component;
    myComponent = component;
  }

  public <T> T getSourceObject(TypedKey<T> role) throws CantPerformException {
    List<T> list = getSourceCollection(role);
    if (list.size() != 1)
      throw new CantPerformException("Wrong size: " + list.size() + ". Role: " + role);
    return list.get(0);
  }

  public C getComponent() {
    return myComponent;
  }

  public <T> List<T> getSourceCollection(TypedKey<T> role) throws CantPerformException {
    DataProvider provider = DataProvider.DATA_PROVIDER.getClientValue((JComponent) myComponent);
    assert provider != null : myComponent;
    List<T> list = provider.getObjectsByRole(role);
    if (list == null)
      throw new CantPerformException(role.toString());
    return list;
  }

  public ActionEvent createActionEvent() {
    return new ActionEvent(myComponent, ActionEvent.ACTION_PERFORMED, null);
  }
}
