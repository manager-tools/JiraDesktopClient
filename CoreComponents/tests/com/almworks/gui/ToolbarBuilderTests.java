package com.almworks.gui;

import com.almworks.util.components.AToolbar;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.exec.AwtImmediateThreadGate;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.ui.actions.ActionBridge;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ToolbarBuilderTests extends BaseTestCase {
  private ToolbarBuilder myBuilder;
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private AToolbar myToolbar;

  public ToolbarBuilderTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myBuilder = new ToolbarBuilder();
  }

  protected void tearDown() throws Exception {
    myBuilder = null;
    if (myToolbar != null) {
      myToolbar.removeNotify();
      myToolbar = null;
    }
    super.tearDown();
  }

  public void testAllActionsRegistered() {
    MockAction action = new MockAction("1");
    MockAction privateAction = new MockAction("2");
    myBuilder.addAction(action);
    myBuilder.addAction(privateAction);
    toolbarBuilt();
    checkButtons(new String[]{"1", "2"});
  }

  private void toolbarBuilt() {
    myToolbar = myBuilder.createHorizontalToolbar();
    myToolbar.addNotify();
    ActionBridge.updateActionsNow();
  }

  private void checkButtons(String[] buttons) {
    ActionBridge.updateActionsNow();
    List<String> actual = getActualButtonsState();
    CHECK.order(buttons, actual);
  }

  private List<String> getActualButtonsState() {
    List<String> actual = Collections15.arrayList();
    for (int i = 0; i < myToolbar.getComponentCount(); i++) {
      AbstractButton button = (AbstractButton) myToolbar.getComponent(i);
      actual.add(button.isVisible() ? button.getText() : null);
    }
    return actual;
  }

  static {
    AwtImmediateThreadGate.testSetOurCheckFXThread(false);
    try {
      LAFUtil.installExtensions();
    } finally {
      AwtImmediateThreadGate.testSetOurCheckFXThread(true);
    }
  }
}
