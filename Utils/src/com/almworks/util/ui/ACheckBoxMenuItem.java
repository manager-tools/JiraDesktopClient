package com.almworks.util.ui;

import com.almworks.util.components.AActionButton;
import com.almworks.util.components.AMenuChild;
import com.almworks.util.components.AnActionHolder;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * @author dyoma
 */
public class ACheckBoxMenuItem extends JCheckBoxMenuItem implements AActionComponent<JCheckBoxMenuItem>, AMenuChild {
  private final AnActionHolder myActionHolder = new AnActionHolder(this);

  public ACheckBoxMenuItem() {
  }

  public ACheckBoxMenuItem(AnAction action) {
    setAnAction(action);
  }

  public Detach setAnAction(AnAction action) {
    assert !isDisplayable() || !isVisible() : action;
    return myActionHolder.setAnAction(action);
  }

  public void setActionById(String actionId) {
    myActionHolder.setActionById(actionId);
  }

  protected void configurePropertiesFromAction(Action a) {
    super.configurePropertiesFromAction(a);
    AActionButton.updateVisibility(this, a);
    Object toggledBoolean = a.getValue(PresentationKey.ACTION_KEY_TOGGLED_ON);
    setState(Boolean.TRUE.equals(toggledBoolean));
  }

  protected PropertyChangeListener createActionPropertyChangeListener(final Action a) {
    final PropertyChangeListener superListener = super.createActionPropertyChangeListener(a);
    return new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        superListener.propertyChange(evt);
        configurePropertiesFromAction(a);
      }
    };
  }

  public void setContextComponent(JComponent component) {
    assert !isDisplayable() || !isVisible() : component;
    myActionHolder.setContextComponent(component);
  }

  public void setPresentationMapping(String swingKey, PresentationMapping<?> mapping) {
    myActionHolder.setPresentationMapping(swingKey, mapping);
  }

  public void overridePresentation(Map mapping) {
    myActionHolder.overridePresentation(mapping);
  }

  public void updateNow() {
    myActionHolder.updateNow();
  }

  public JCheckBoxMenuItem toComponent() {
    return this;
  }

  public void parentStartsUpdate() {
    myActionHolder.startUpdate();
  }

  public void parentStopsUpdate() {
    myActionHolder.stopUpdate();
  }
}
