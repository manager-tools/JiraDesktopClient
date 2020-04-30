package com.almworks.util.ui.actions.globals;

import com.almworks.util.collections.ChangeCounter;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.actions.*;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author dyoma
 */
public class GlobalDataTests extends GUITestCase {
  private static final TypedKey<Object> ROLE = TypedKey.create("data");
  private static final Object VALUE = new Object();
  private final JPanel myRoot = new JPanel();
  private final JLabel myDataComponent = new JLabel();
  private final JPanel myGlobalizer = new JPanel();
  private final JButton myLeaf = new JButton();

  protected void setUp() throws Exception {
    super.setUp();
    myRoot.add(myGlobalizer);
    myGlobalizer.add(myLeaf);
    ConstProvider.addRoleValue(myDataComponent, ROLE, VALUE);
    GlobalData.KEY.addClientValue(myDataComponent, ROLE);
  }

  public void testSimpleCase() {
    myGlobalizer.add(myDataComponent);
    GlobalDataRoot.install(myGlobalizer);

    assertSame(VALUE, getOrNull(myLeaf));
    assertNull(getOrNull(myRoot));
  }

  public void testAddRemoveComponent() {
    GlobalDataRoot.install(myGlobalizer);
    assertNull(getOrNull(myLeaf));
    myGlobalizer.add(myDataComponent);
    assertSame(VALUE, getOrNull(myLeaf));
    myGlobalizer.remove(myDataComponent);
    assertNull(getOrNull(myLeaf));
  }

  public void testSubscription() {
    GlobalDataRoot.install(myGlobalizer);
    DataProvider globalProvider = DataProvider.DATA_PROVIDER.getClientValue(myGlobalizer);
    assertNotNull(globalProvider);
    ChangeCounter counter = new ChangeCounter();
    globalProvider.addRoleListener(new DetachComposite(), ROLE, counter);
    counter.assertNotCalled();
    JLabel dataComponent = new JLabel();
    MockProvider provider = new MockProvider(ROLE);
    GlobalData.KEY.addClientValue(dataComponent, ROLE);
    DataProvider.DATA_PROVIDER.putClientValue(dataComponent, provider);
    myGlobalizer.add(dataComponent);
    counter.assertIncremented();
    globalProvider.addRoleListener(new DetachComposite(), ROLE, counter);
    provider.setValue(VALUE);
    counter.assertIncremented();
//    myGlobalizer.remove(dataComponent);
//    counter.assertIncremented();
  }

  public void testLateGlobalization() {
    GlobalDataRoot.install(myGlobalizer);
    DataProvider globalProvider = DataProvider.DATA_PROVIDER.getClientValue(myGlobalizer);
    ChangeCounter counter = new ChangeCounter();
    globalProvider.addRoleListener(new DetachComposite(), ROLE, counter);
    JLabel dataComponent = new JLabel();
    myGlobalizer.add(dataComponent);
    MockProvider provider = new MockProvider(ROLE);
    DataProvider.DATA_PROVIDER.putClientValue(dataComponent, provider);
    counter.assertNotCalled();
    assertNull(getOrNull(myGlobalizer));
    GlobalData.KEY.addClientValue(dataComponent, ROLE);
    counter.assertIncremented();
    globalProvider.addRoleListener(new DetachComposite(), ROLE, counter);
    provider.setValue(VALUE);
    counter.assertIncremented();
  }

  public void testComponentGlobalization() throws CantPerformException {
    GlobalDataRoot.install(myGlobalizer);
    myGlobalizer.add(myDataComponent);
    DefaultActionContext context = new DefaultActionContext(myLeaf);
    ComponentContext<JLabel> cc = context.getComponentContext(myDataComponent.getClass(), ROLE);
    assertSame(myDataComponent, cc.getComponent());
    assertSame(VALUE, cc.getSourceObject(ROLE));
  }

  public void testComponentAddRemoveWatching() {
    JPanel root = new JPanel();
    JButton leaf = new JButton();
    root.add(leaf);
    JPanel panel1 = new JPanel();
    root.add(panel1);
    JLabel comp1 = new JLabel();
    panel1.add(comp1);
    ConstProvider.addGlobalValue(comp1, ROLE, VALUE);
    GlobalDataRoot.install(root);
    assertSame(VALUE, getOrNull(leaf));
    panel1.remove(comp1);
    assertNull(getOrNull(leaf));
    panel1.add(comp1);
    assertSame(VALUE, getOrNull(leaf));
    root.remove(panel1);
    assertNull(getOrNull(leaf));
    root.add(panel1);
    assertSame(VALUE, getOrNull(leaf));
  }

  public void testStopsAtUpperDataRoot() {
    JPanel root = new JPanel();
    JPanel root2 = new JPanel();
    root.add(root2);
    JLabel comp1 = new JLabel();
    root2.add(comp1);
    DataRoot.KEY.putClientValue(root2, DataRoot.TERMINATOR);
    GlobalData.KEY.addClientValue(comp1, ROLE);
    MockProvider provider = new MockProvider(ROLE, comp1);
    DataProvider.DATA_PROVIDER.putClientValue(comp1, provider);
    GlobalDataRoot.install(root);
    DataProvider globalProvider = DataProvider.DATA_PROVIDER.getClientValue(root);
    ChangeCounter counter = new ChangeCounter();
    globalProvider.addRoleListener(new DetachComposite(), ROLE, counter);
    provider.setValue("a");
    counter.assertNotCalled();
    JLabel comp2 = new JLabel();
    ConstProvider.addGlobalValue(comp2, ROLE, "b");
    counter.assertNotCalled();
    root.add(comp2);
    counter.assertIncremented();
  }

  @Nullable
  private Object getOrNull(JComponent context) {
    try {
      return new DefaultActionContext(context).getSourceObject(ROLE);
    } catch (CantPerformException e) {
      return null;
    }
  }
}
