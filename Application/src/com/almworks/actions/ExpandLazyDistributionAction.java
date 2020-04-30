package com.almworks.actions;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.LazyDistributionExpanderNode;
import com.almworks.util.L;
import com.almworks.util.ui.actions.*;

// todo remove
public class ExpandLazyDistributionAction extends SimpleAction {
  public ExpandLazyDistributionAction() {
    super("Expand Distribution");
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Fill distribution with values"));
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    context.setEnabled(node instanceof LazyDistributionExpanderNode ? EnableState.ENABLED : EnableState.INVISIBLE);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    if (node instanceof LazyDistributionExpanderNode)
      ((LazyDistributionExpanderNode) node).expand();
  }
}
