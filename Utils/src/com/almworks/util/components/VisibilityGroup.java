package com.almworks.util.components;

import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * This class tracks changes to all JTextComponent in one group and makes all group invisible if
 * there's no text in all text components.
 */
public class VisibilityGroup {
  public static final ComponentProperty<VisibilityGroup> COMPONENT_PROPERTY = ComponentProperty.createProperty("VisibilityGroup");

  private final JComponent myWholeComponent;
  private final String myName;
  private final java.util.List<JComponent> myComponents = Collections15.arrayList();

  public VisibilityGroup(String name, JComponent wholeComponent) {
    myName = name;
    myWholeComponent = wholeComponent;
  }

  public void add(JComponent component, DetachComposite detach) {
    myComponents.add(component);
    COMPONENT_PROPERTY.putClientValue(component, this);
  }

  public String toString() {
    return myName;
  }

  public void checkVisibility() {
    boolean show = false;
    for (int i = 0; i < myComponents.size(); i++) {
      JComponent component = myComponents.get(i);
      if (component instanceof JTextComponent) {
        if (Util.NN(((JTextComponent) component).getText()).length() > 0) {
          show = true;
          break;
        }
      }
    }

    for (int i = 0; i < myComponents.size(); i++) {
      JComponent component = myComponents.get(i);
      boolean visible = component.isVisible();
      if (visible != show) {
        component.setVisible(show);
      }
    }
  }
}
