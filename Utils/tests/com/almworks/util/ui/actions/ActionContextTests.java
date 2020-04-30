package com.almworks.util.ui.actions;

import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.TypedKey;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class ActionContextTests extends BaseTestCase {
  private static final TypedKey<Object> ROLE_A = TypedKey.create("a");
  private static final TypedKey<Object> ROLE_B = TypedKey.create("b");
  private static final TypedKey<Object> ROLE_C = TypedKey.create("c");
  private ConstProvider PROVIDER_A;
  private ConstProvider PROVIDER_B;
  private ConstProvider PROVIDER_C;

  public ActionContextTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    PROVIDER_A = ConstProvider.singleData(ROLE_A, "1");
    PROVIDER_B = ConstProvider.singleData(ROLE_B, "2");
    PROVIDER_C = ConstProvider.singleData(ROLE_C, "3");
  }

  public void testAddingProviders() throws CantPerformException {
    JComponent component = new JPanel();
    DataProvider.DATA_PROVIDER.putClientValue(component, PROVIDER_A);
    assertEquals("1", new DefaultActionContext(component).getSourceObject(ROLE_A));

    DataProvider.DATA_PROVIDER.putClientValue(component, PROVIDER_B);
    ActionContext context = new DefaultActionContext(component);
    assertEquals("1", context.getSourceObject(ROLE_A));
    assertEquals("2", context.getSourceObject(ROLE_B));

    DataProvider.DATA_PROVIDER.putClientValue(component, PROVIDER_C);
    context = new DefaultActionContext(component);
    assertEquals("1", context.getSourceObject(ROLE_A));
    assertEquals("2", context.getSourceObject(ROLE_B));
    assertEquals("3", context.getSourceObject(ROLE_C));
  }

  public void testChildProvider() throws CantPerformException {
    JComponent component = new JPanel();
    DataProvider.DATA_PROVIDER.putClientValue(component, PROVIDER_A);
    ActionContext context = new DefaultActionContext(component);
    ActionContext child = context.childContext(PROVIDER_B);
    assertEquals("1", child.getSourceObject(ROLE_A));
    assertEquals("2", child.getSourceObject(ROLE_B));
  }

  public void testParentSearch() throws CantPerformException {
    JPanel parent = new JPanel();
    JPanel child = new JPanel();
    parent.add(child);
    JPanel jump = new JPanel();
    ComponentProperty.JUMP.putClientValue(parent, jump);
    DataProvider.DATA_PROVIDER.putClientValue(child, PROVIDER_A);
    DataProvider.DATA_PROVIDER.putClientValue(parent, PROVIDER_B);
    DataProvider.DATA_PROVIDER.putClientValue(jump, PROVIDER_C);

    ActionContext context = new DefaultActionContext(child);
    assertEquals("1", context.getSourceObject(ROLE_A));
    assertEquals("2", context.getSourceObject(ROLE_B));
    assertEquals("3", context.getSourceObject(ROLE_C));
  }

  public void testAttachContext() {
    JPanel panel = new JPanel();
    ActionBridge bridge = new ActionBridge(new SimpleAction("Mock") {
      {
        watchRole(ROLE_A);
      }
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.getSourceObject(ROLE_A);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
      }
    }, panel);
    MockProvider provider = new MockProvider(ROLE_A);
    DataProvider.DATA_PROVIDER.putClientValue(panel, provider);
    bridge.startUpdate();
    try {
      assertTrue(bridge.isUpToDate());
      assertFalse(bridge.isEnabled());
      provider.setValue("1");
      assertFalse(bridge.isUpToDate());
      bridge.ensureUpToDate();
      assertTrue(bridge.isEnabled());
    } finally{
      bridge.stopUpdate();
    }
  }

  public void testUpdateFailedWithException() {
    class MockUpdatableAction extends SimpleAction {
      private EnableState myEnableState;
      protected MockUpdatableAction() {
        super("Mock action");
      }

      public void setEnableState(EnableState enableState) {
        myEnableState = enableState;
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.setEnabled(myEnableState);
        throw new CantPerformException();
      }

      protected void doPerform(ActionContext context) throws CantPerformException {}
    }

    MockUpdatableAction action = new MockUpdatableAction();
    action.setEnableState(EnableState.ENABLED);
    ActionBridge bridge = new ActionBridge(action, new JPanel());
    bridge.startUpdate();
    try {
      bridge.updateNow();
      assertTrue(bridge.isVisible());
      assertFalse(bridge.isEnabled());

      action.setEnableState(EnableState.DISABLED);
      bridge.updateNow();
      assertTrue(bridge.isVisible());
      assertFalse(bridge.isEnabled());

      action.setEnableState(EnableState.INVISIBLE);
      bridge.updateNow();
      assertFalse(bridge.isVisible());
      assertFalse(bridge.isEnabled());
    } finally{
      bridge.stopUpdate();
    }
  }

}
