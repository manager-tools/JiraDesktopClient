package com.almworks.util.ui;

import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Set;

/**
 * @author dyoma
 */
public class ComponentEnabler implements ChangeListener, PropertyChangeListener {
  private final JToggleButton myButton;
  private final Set<JComponent> myComponents = Collections15.hashSet();
  private JComponent myDefaultComponent;
  private boolean myInvert = false;
  private boolean myOnlyEnabled = true;
  private boolean myChangeVisible = false;
  private boolean myChangeEnabled = true;

  private boolean myWasSelected;
  private boolean myWasEnabled;

  public ComponentEnabler(JToggleButton button) {
    myButton = button;
    myWasSelected = myButton.isSelected();
    myWasEnabled = myButton.isEnabled();
    myButton.addChangeListener(this);
    myButton.addPropertyChangeListener("enabled", this);
  }

  public ComponentEnabler addComponents(JComponent ... components) {
    myComponents.addAll(Arrays.asList(components));
    updateComponents();
    return this;
  }
  
  public ComponentEnabler setDefaultComponent(JComponent component) {
    if (myComponents.contains(component)) myDefaultComponent = component;
    return this;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    updateIfNeeded();
    myWasEnabled = myButton.isEnabled();
  }

  public void stateChanged(ChangeEvent e) {
    updateIfNeeded();
    myWasSelected = myButton.isSelected();
    if (myButton.isSelected() && myDefaultComponent != null) myDefaultComponent.requestFocusInWindow();
  }

  private void updateIfNeeded() {
    if (myWasSelected != myButton.isSelected() || (myOnlyEnabled && (myWasEnabled != myButton.isEnabled())))
      updateComponents();
  }

  public Detach createDetach() {
    return new Detach() {
      protected void doDetach() {
        ComponentEnabler.this.detach();
      }
    };
  }

  public void detach() {
    myButton.removeChangeListener(this);
    myButton.removePropertyChangeListener("enabled", this);
  }

  private void updateComponents() {
    myWasSelected = myButton.isSelected();
    boolean selected = shouldEnable();
    boolean newState = selected ? !myInvert : myInvert;
    for (JComponent component : myComponents) {
      if (myChangeEnabled)
        component.setEnabled(newState);
      if (myChangeVisible) {
        component.setVisible(newState);
        Container parent = component.getParent();
        while (parent instanceof JComponent) {
          ((JComponent) parent).revalidate();
          parent.repaint();
          parent = parent.getParent();
        }
      }
    }
  }

  private boolean shouldEnable() {
    return myButton.isSelected() && (!myOnlyEnabled || myButton.isEnabled());
  }

  public ComponentEnabler setInvert(boolean invert) {
    myInvert = invert;
    updateComponents();
    return this;
  }

  public ComponentEnabler setChangeVisible(boolean makeVisible) {
    myChangeVisible = makeVisible;
    updateComponents();
    return this;
  }

  public ComponentEnabler setChangeEnabled(boolean makeEnabled) {
    myChangeEnabled = makeEnabled;
    updateComponents();
    return this;
  }

  public ComponentEnabler setEnableWhenButtonDisabled(boolean onlyEnabled) {
    myOnlyEnabled = onlyEnabled;
    updateComponents();
    return this;
  }

  public static ComponentEnabler create(JToggleButton button, JComponent ... components) {
    ComponentEnabler result = new ComponentEnabler(button);
    result.addComponents(components);
    return result;
  }
}
