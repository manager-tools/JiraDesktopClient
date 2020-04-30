package com.almworks.util.ui;

import com.almworks.util.tests.GUITestCase;

import javax.swing.*;

public class ComponentEnablerTests extends GUITestCase {
  public void testChangeComponentsWhenEnableChanged() {
    JCheckBox button = new JCheckBox();
    JLabel component = new JLabel();
    ComponentEnabler.create(button, component);
    button.setSelected(true);
    assertTrue(component.isEnabled());

    button.setSelected(false);
    assertFalse(component.isEnabled());

    button.setSelected(true);
    assertTrue(component.isEnabled());
    button.setEnabled(false);
    assertFalse(component.isEnabled());
  }
}
