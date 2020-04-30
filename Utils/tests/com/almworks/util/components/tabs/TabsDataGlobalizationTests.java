package com.almworks.util.components.tabs;

import com.almworks.util.collections.ChangeCounter;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.DefaultActionContext;
import com.almworks.util.ui.actions.globals.GlobalDataRoot;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author dyoma
 */
public class TabsDataGlobalizationTests extends GUITestCase {
  private static final TypedKey<Object> ROLE = TypedKey.create("role");
  private static final Object VALUE1 = new Object();
  private static final Object VALUE2 = new Object();

  private final JButton myLeaf = new JButton();
  private final JPanel myRoot = new JPanel();
  private TabsManager myManager;

  protected void setUp() throws Exception {
    super.setUp();
    myManager = new TabsManager();
    myRoot.add(myManager.getComponent());
    GlobalDataRoot.install(myRoot);
    myRoot.add(myLeaf);
  }

  public void testTabsDataSelection() {
    createTab("1", VALUE1);
    assertSame(VALUE1, getData());
    createTab("2", VALUE2);
    assertSame(VALUE2, getData());
  }

  public void testChangeNotification() {
    ChangeCounter counter = new ChangeCounter();
    subsribe(counter);
    assertNull(getData());
    createTab("1", VALUE1);
    counter.assertIncremented();
    assertSame(VALUE1, getData());
    subsribe(counter);
    ContentTab tab2 = createTab("2", VALUE2);
    counter.assertIncremented();
    assertSame(VALUE2, getData());
    subsribe(counter);
    tab2.setVisible(false);
    counter.assertIncremented();
    assertSame(VALUE1, getData());
  }

  public void testAddTree() {
    ChangeCounter counter = new ChangeCounter();
    subsribe(counter);
    ContentTab tab = myManager.createTab("1");
    JPanel panel = new JPanel();
    JLabel source1 = createDataSource(VALUE1);
    panel.add(source1);
    counter.assertNotCalled();
    tab.setComponent(panel);
    counter.assertIncremented();
  }

  public void testAddWatching() {
    ChangeCounter counter = new ChangeCounter();
    subsribe(counter);
    ContentTab tab = myManager.createTab("1");
    JPanel panel = new JPanel();
    tab.setComponent(panel);
    counter.assertNotCalled();
    panel.add(createDataSource(VALUE1));
    counter.assertIncremented();
  }

  private void subsribe(ChangeListener counter) {
    DataProvider.DATA_PROVIDER.getClientValue(myRoot).addRoleListener(new DetachComposite(), ROLE, counter);
  }

  private ContentTab createTab(String name, Object value) {
    ContentTab tab = myManager.createTab(name);
    JLabel component1 = createDataSource(value);
    tab.setComponent(component1);
    return tab;
  }

  private JLabel createDataSource(Object value) {
    JLabel component1 = new JLabel();
    ConstProvider.addGlobalValue(component1, ROLE, value);
    return component1;
  }

  @Nullable
  private Object getData() {
    DefaultActionContext context = new DefaultActionContext(myLeaf);
    try {
      return context.getSourceObject(ROLE);
    } catch (CantPerformException e) {
      return null;
    }
  }
}
