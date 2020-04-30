package com.almworks.tags;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.TagNode;
import com.almworks.explorer.tree.FavoritesNode;
import com.almworks.util.L;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.*;

public class EditTagAction extends SimpleAction {
  public EditTagAction() {
    super(Local.parse("Edit &Tag"));
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Change tag's name and icon"));
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    boolean enabled = node instanceof TagNode && !(node instanceof FavoritesNode);
    context.setEnabled(enabled ? EnableState.ENABLED : EnableState.INVISIBLE);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    if (node instanceof TagNode) {
      ((TagNode) node).editNode(context);
    }
  }
}
