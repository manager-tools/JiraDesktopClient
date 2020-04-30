package com.almworks.actions;

import com.almworks.api.application.tree.RenamableNode;
import com.almworks.api.gui.CommonDialogs;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.util.AppBook;
import com.almworks.util.i18n.LText;
import com.almworks.util.ui.actions.*;

class RenameAction extends SimpleAction {
  private static final String X = "Application.Actions.Rename.";
  private final static LText DESCRIPTION = AppBook.text(X + "DESCRIPTION", "Rename selected node");
  private final static LText NAME = AppBook.text(X + "NAME", "Re&name");

  public RenameAction() {
    super(NAME.format());
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, DESCRIPTION.format());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    RenamableNode node = context.getSourceObject(RenamableNode.RENAMEABLE);
    if (!node.isRenamable()) {
      assert false : node;
      return;
    }
    DialogEditorBuilder builder = CommonDialogs.createRenameDialog(context, node.getPresentation(),
      "Rename");
    builder.showWindow();
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.watchRole(RenamableNode.RENAMEABLE);
    context.setEnabled(EnableState.INVISIBLE);
    RenamableNode renamableNode = context.getSourceObject(RenamableNode.RENAMEABLE);
    context.setEnabled(renamableNode.isRenamable());
  }
}
