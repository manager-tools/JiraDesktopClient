package com.almworks.util.components;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.GUITestCase;

import javax.swing.*;

/**
 * @author dyoma
 */
public class AMenuTests extends BaseTestCase {
  public void testVisibleMenu() {
    JMenu menu = AMenu.disabling();
    assertFalse(menu.isEnabled());

    JMenuItem child = new JMenuItem();
    child.setVisible(false);
    menu.add(child);
    GUITestCase.flushAWTQueue();
    assertFalse(menu.isEnabled());

    child.setVisible(true);
    GUITestCase.flushAWTQueue();
    assertTrue(menu.isEnabled());

    child.setVisible(false);
    GUITestCase.flushAWTQueue();
    assertFalse(menu.isEnabled());

    JMenuItem child2 = new JMenuItem();
    child2.setVisible(true);
    GUITestCase.flushAWTQueue();
    menu.add(child2);
    assertTrue(menu.isEnabled());
  }

  public void testAMenu() {
    JMenu menu = AMenu.hidding();
    assertFalse(menu.isVisible());

    JMenuItem child = new JMenuItem();
    child.setVisible(false);
    menu.add(child);
    GUITestCase.flushAWTQueue();
    assertFalse(menu.isVisible());

    JMenuItem child2 = new JMenuItem();
    child2.setVisible(true);
    menu.add(child2);
    GUITestCase.flushAWTQueue();
    assertTrue(menu.isVisible());

    child2.setVisible(false);
    GUITestCase.flushAWTQueue();
    assertFalse(menu.isVisible());

    child.setVisible(true);
    GUITestCase.flushAWTQueue();
    assertTrue(menu.isVisible());
  }
}
