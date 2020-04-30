package com.almworks.api.application.viewer;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;

class ExpandCommentsAction extends SimpleAction {
  public ExpandCommentsAction() {
    super("Expand All Comments", Icons.ACTION_EXPAND_ALL_COMMENTS);
    watchRole(CommentsController.ROLE);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke("control shift EQUALS"));
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.DISABLED);
    CommentsController<?> controller = context.getSourceObject(CommentsController.ROLE);
    AListModel<? extends CommentState<? extends Comment>> model = controller.getCommentsModel();
    context.updateOnChange(model);
    for (int i = 0; i < model.getSize(); i++) {
      if (model.getAt(i).isCollapsed()) {
        context.setEnabled(EnableState.ENABLED);
        break;
      }
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    context.getSourceObject(CommentsController.ROLE).expandAll();
  }
}
