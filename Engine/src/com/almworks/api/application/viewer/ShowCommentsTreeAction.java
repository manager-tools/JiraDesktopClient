package com.almworks.api.application.viewer;

import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

class ShowCommentsTreeAction extends SimpleAction {
  public ShowCommentsTreeAction() {
    super("Threaded Comments View", Icons.SHOW_COMMENTS_TREE);
    watchRole(CommentsController.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    CommentsController<?> controller = context.getSourceObject(CommentsController.ROLE);
    if (!controller.canShowTree()) {
      context.setEnabled(EnableState.INVISIBLE);
      return;
    }
    context.updateOnChange(controller.getModifiable());
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, controller.isShowTree());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    CommentsController<?> controller = context.getSourceObject(CommentsController.ROLE);
    controller.showTree(!controller.isShowTree());
  }
}
