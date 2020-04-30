package com.almworks.tags;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.explorer.tree.TagEditor;
import com.almworks.explorer.tree.TagsFolderNode;
import com.almworks.util.L;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.*;

public class NewTagAction extends SimpleAction {
  private final boolean myNodeOnly;

  public NewTagAction(boolean nodeOnly) {
    super(Local.parse("New &Tag"));
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Create a new tag"));
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    myNodeOnly = nodeOnly;
    if (nodeOnly) {
      watchRole(GenericNode.NAVIGATION_NODE);
    }
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    if (myNodeOnly) {
      context.setEnabled(EnableState.INVISIBLE); //if two or more nodes selected,third line skipped
      boolean enabled = context.getSourceObject(GenericNode.NAVIGATION_NODE) instanceof TagsFolderNode;
      context.setEnabled(enabled ? EnableState.ENABLED : EnableState.INVISIBLE);
    } else {
      context.setEnabled(EnableState.ENABLED);
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    TagEditor.editAndCreateNode(context);
  }
}
