package com.almworks.gui;

import com.almworks.util.DECL;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.ui.actions.*;
import org.almworks.util.TypedKey;

import javax.swing.*;

/**
 * @author dyoma
 */
public class IdActionProxyTests extends BaseTestCase {
  private static final String ID = "id";
  private static final TypedKey<String> MOCK_NAME = TypedKey.create("mockData");

  private ActionRegistryImpl myRegistry = new ActionRegistryImpl();
  private final MockAction myAction = new MockAction("1") {
    public void update(UpdateContext context) throws CantPerformException {
      super.update(context);
      context.watchRole(MOCK_NAME);
      try {
       context.putPresentationProperty(PresentationKey.NAME, context.getSourceObject(MOCK_NAME));
      } catch(CantPerformException e) {
        DECL.ignoreException();
      }
    }
  };
  private final IdActionProxy myProxy = new IdActionProxy(ID);
  private ActionBridge myBridge;
  private final JPanel myPanel = new JPanel();
  private final SimpleProvider myProvider = new SimpleProvider(MOCK_NAME);

  public IdActionProxyTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myRegistry = new ActionRegistryImpl();
    ConstProvider.addRoleValue(myPanel, ActionRegistry.ROLE, myRegistry);
    DataProvider.DATA_PROVIDER.putClientValue(myPanel, myProvider);
    myBridge = new ActionBridge(myProxy, myPanel);
  }

  protected void tearDown() throws Exception {
    myRegistry = null;
    myBridge.stopUpdate();
    super.tearDown();
  }

  public void testExistingAction() throws CantPerformException {
    myRegistry.registerAction(ID, myAction);
    attachProxy();
    assertTrue(myBridge.isUpToDate());
    assertEquals("1", myBridge.getPresentation().getValue(Action.NAME));
    assertTrue(myBridge.isVisible());
    assertTrue(myBridge.isEnabled());

    myProvider.setSingleData(MOCK_NAME, "newName");
    assertFalse(myBridge.isUpToDate());
    myBridge.ensureUpToDate();
    assertTrue(myBridge.isUpToDate());
    assertEquals("newName", myBridge.getPresentation().getValue(Action.NAME));

    myAction.setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    myProvider.setSingleData(MOCK_NAME, "otherName");
    assertFalse(myBridge.isUpToDate());
    myBridge.ensureUpToDate();
    assertFalse(myBridge.isEnabled());
    assertTrue((myBridge.isVisible()));
  }

  private void attachProxy() {
    myBridge.startUpdate();
  }

  public void testRegisterOnTheFly() throws CantPerformException {
    attachProxy();
    assertFalse(myBridge.isEnabled());
    assertFalse(myBridge.isVisible());

    myRegistry.registerAction(ID, myAction);
    assertFalse(myBridge.isUpToDate());
    myBridge.ensureUpToDate();
    assertEquals("1", myBridge.getPresentation().getValue(Action.NAME));
    assertTrue(myBridge.isEnabled());
    assertTrue(myBridge.isVisible());

    myProvider.setSingleData(MOCK_NAME, "newName");
    assertFalse(myBridge.isUpToDate());
    myBridge.ensureUpToDate();
    assertEquals("newName", myBridge.getPresentation().getValue(Action.NAME));
  }
}
