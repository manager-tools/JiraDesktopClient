package com.almworks.api.application.viewer;

import com.almworks.util.ui.actions.*;

import javax.swing.*;

/**
 * @author dyoma
*/
class SortAction extends SimpleAction {
  private final boolean myDirect;

  public SortAction(String name, Icon icon, boolean direct) {
    super(name, icon);
    myDirect = direct;
    watchRole(CommentsController.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    CommentsController<?> controller = context.getSourceObject(CommentsController.ROLE);
    context.updateOnChange(controller.getModifiable());
    boolean sorted = controller.isDirectSorted() == myDirect;
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, sorted);
    context.setEnabled(sorted ? EnableState.DISABLED : EnableState.ENABLED);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    context.getSourceObject(CommentsController.ROLE).sort(myDirect);
  }
}
