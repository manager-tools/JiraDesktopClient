package com.almworks.util.components;

import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.actions.*;

import javax.swing.*;

/**
 * @author dyoma
 */
public class AnActionHolderTests extends GUITestCase {
  private final AnAction myAction = new SimpleAction("action") {
    protected void customUpdate(UpdateContext context) throws CantPerformException {}

    protected void doPerform(ActionContext context) throws CantPerformException {}
  };

  public void testDefaultPresentationMapping() {
    JButton button = new JButton();
    AnActionHolder holder = new AnActionHolder(button);
    holder.setAnAction(myAction);
    Action action = button.getAction();
    holder.startUpdate();
    assertEquals("action", action.getValue(Action.NAME));
    assertTrue(action.isEnabled());
    assertTrue((Boolean)action.getValue(PresentationKey.ACTION_KEY_VISIBLE));
  }
}
