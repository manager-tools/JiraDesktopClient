package com.almworks.gui;

import com.almworks.util.ui.actions.*;

/**
 * @author dyoma
 */
class MockAction extends AnAbstractAction {
  public int myPerformCounter = 0;

  public MockAction(String name) {
    super(name);
  }

  public void perform(ActionContext context) throws CantPerformException {
    myPerformCounter++;
  }

  public void setName(String name) {
    setDefaultPresentation(PresentationKey.NAME, name);
  }

  public void update(UpdateContext context) throws CantPerformException {
    super.update(context);
//    context.updateOnChange(getSelf);
  }
}
