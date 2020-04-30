package com.almworks.util.components.tabs;

import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.GUITestCase;
import util.concurrent.Synchronized;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class TabsManagerTests extends GUITestCase {
  private TabsManager myTabsManager;
  private JComponent myTabsHolderComponent;

  protected void setUp() throws Exception {
    super.setUp();
    myTabsManager = new TabsManager();
    myTabsHolderComponent = myTabsManager.getComponent();
  }

  public void testAddingTabs() {
    assertEquals(0, getTabsCount());
    ContentTab tab1 = myTabsManager.createTab();
    assertEquals(0, getTabsCount());
    tab1.setName("1");
    JLabel label1 = new JLabel();
    tab1.setComponent(label1);
    assertEquals(1, getTabsCount());
    assertSame(label1, getTab(0));

    ContentTab tab2 = myTabsManager.createTab();
    tab2.setName("2");
    JLabel label2 = new JLabel("2");
    tab2.setComponent(label2);
    assertEquals(2, getTabsCount());
    assertEquals("1", getName(0));
    assertEquals("2", getName(1));
    assertSame(label1, getTab(0));
    assertSame(label2, getTab(1));
  }

  public void testReplacingComponent() {
    ContentTab tab1 = myTabsManager.createTab();
    ContentTab tab2 = myTabsManager.createTab();
    tab2.setName("2");
    tab2.setComponent(new JLabel("t2"));
    assertEquals("t2", getLabelText(0));
    tab1.setName("t1");
    tab1.setComponent(new JLabel("t1"));
    assertEquals("t1", getLabelText(0));
    tab1.setComponent(new JLabel("t1a"));
    assertEquals("t1a", getLabelText(0));
  }

  public void testRemove() {
    ContentTab tab1 = myTabsManager.createTab();
    ContentTab tab2 = myTabsManager.createTab();
    ContentTab tab3 = myTabsManager.createTab();
    tab3.setComponent(new JLabel("3"));
    tab1.setComponent(new JLabel("1"));
    tab2.setComponent(new JLabel("2"));
    assertEquals("3", getLabelText(2));
    assertEquals("2", getLabelText(1));
    assertEquals("1", getLabelText(0));
    tab1.clearComponent();
    assertEquals(2, getTabsCount());
    assertEquals("2", getLabelText(0));
    assertEquals("3", getLabelText(1));
    tab3.clearComponent();
    assertEquals(1, getTabsCount());
    assertEquals("2", getLabelText(0));
    tab2.clearComponent();
    assertEquals(0, getTabsCount());
  }

  public void testUpdateName() {
    ContentTab tab1 = myTabsManager.createTab();
    ContentTab tab2 = myTabsManager.createTab();
    tab1.setName("1");
    tab1.setComponent(new JLabel());
    tab2.setName("2");
    tab2.setComponent(new JLabel());
    assertEquals("2", getName(1));
    tab2.setName("2a");
    assertEquals("2a", getName(1));
  }

  public void testVisibility() {
    ContentTab tab1 = myTabsManager.createTab();
    tab1.setName("1");
    ContentTab tab2 = myTabsManager.createTab();
    tab2.setName("2");
    tab1.setVisible(false);
    tab1.setComponent(new JLabel("1"));
    assertEquals(0, getTabsCount());
    tab2.setComponent(new JLabel("2"));
    assertEquals(1, getTabsCount());
    assertEquals("2", getLabelText(0));
    tab1.setVisible(true);
    assertEquals(2, getTabsCount());
    assertEquals("1", getName(0));
  }

  public void testSelection() {
    assertNull(myTabsManager.getSelectedTab());
    ContentTab tab1 = myTabsManager.createTab("1");
    assertNull(myTabsManager.getSelectedTab());
    tab1.setComponent(new JLabel("1"));
    assertEquals(tab1, myTabsManager.getSelectedTab());
    ContentTab tab2 = myTabsManager.createTab("2");
    assertEquals(tab1, myTabsManager.getSelectedTab());
    tab2.setComponent(new JLabel("2"));
    assertEquals(tab2, myTabsManager.getSelectedTab());
    JTabbedPane tabbedPane = getTabbedPane();
    tabbedPane.setSelectedIndex(0);
    assertEquals(tab1, myTabsManager.getSelectedTab());
    tab1.setVisible(false);
    assertEquals(tab2, myTabsManager.getSelectedTab());
  }

  public void testSelectionListener() {
    final Synchronized<ContentTab> selection = new Synchronized<ContentTab>(null);
    myTabsManager.addSelectionListener(new ChangeListener1<ContentTab>() {
      public void onChange(ContentTab tab) {
        selection.set(tab);
      }
    }, ThreadGate.STRAIGHT);
    ContentTab tab1 = myTabsManager.createTab("1");
    assertNull(selection.get());
    tab1.setComponent(new JLabel());
    assertSame(tab1, selection.get());
    ContentTab tab2 = myTabsManager.createTab("2");
    tab2.setComponent(new JLabel());
    assertSame(tab2, selection.get());
    getTabbedPane().setSelectedIndex(0);
    assertSame(tab1, selection.get());
    tab1.setVisible(false);
    assertSame(tab2, selection.get());
  }

  public void testDefaultComponent() {
    JLabel defaultComponent = new JLabel("default");
    myTabsManager.setDefaultComponent(defaultComponent);
    assertEquals("default", getLabelText(0));
    assertSame(null, myTabsManager.getSelectedTab());
    ContentTab tab1 = myTabsManager.createTab("1");
    tab1.setComponent(new JLabel("l1"));
    assertEquals("l1", getLabelText(0));
    assertEquals(1, getTabsCount());
    assertTrue(myTabsHolderComponent.getComponent(0) instanceof JTabbedPane);
    assertSame(tab1, myTabsManager.getSelectedTab());
    tab1.delete();
    assertEquals("default", getLabelText(0));
    assertSame(defaultComponent, myTabsHolderComponent.getComponent(0));
    assertSame(null, myTabsManager.getSelectedTab());
    myTabsManager.setDefaultComponent(new JLabel("new"));
    assertEquals("new", getLabelText(0));
  }

  private int getTabsCount() {
    if (myTabsHolderComponent.getComponentCount() == 0)
      return 0;
    assertEquals(1, myTabsHolderComponent.getComponentCount());
    Component component = myTabsHolderComponent.getComponent(0);
    if (component instanceof JTabbedPane)
      return ((JTabbedPane) component).getTabCount();
    return 1;
  }

  private JTabbedPane getTabbedPane() {
    return (JTabbedPane) myTabsManager.getComponent().getComponent(0);
  }

  private JComponent getTab(int index) {
    Component component = myTabsHolderComponent.getComponent(0);
    if (component instanceof JTabbedPane)
      return (JComponent) ((JTabbedPane) component).getComponentAt(index);
    else {
      assertEquals(0, index);
      return (JComponent) component;
    }
  }

  private String getName(int index) {
    Component component = myTabsHolderComponent.getComponent(0);
    if (component instanceof JTabbedPane)
      return ((JTabbedPane) component).getTitleAt(index);
    fail("Component count: " + getTabsCount());
    return "";
  }

  private int getSelectedIndex() {
    Component component = myTabsHolderComponent.getComponent(0);
    if (component instanceof JTabbedPane)
      return ((JTabbedPane) component).getSelectedIndex();
    return 0;
  }

  private String getLabelText(int index) {
    JComponent tab = getTab(index);
    assertTrue(tab instanceof JLabel);
    return ((JLabel) tab).getText();
  }
}
