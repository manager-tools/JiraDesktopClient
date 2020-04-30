package com.almworks.actions;

import com.almworks.api.application.tree.ChildrenOrderPolicy;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.AppBook;
import com.almworks.util.i18n.LText;
import com.almworks.util.ui.actions.*;

import java.util.List;

class SortNodesAction extends AnAbstractAction {
  private static final String X = "Application.Actions.SortNodes.";
  private static final LText ACTION_NAME = AppBook.text(X + "ACTION_NAME", "S&ort Nested Nodes");
  private static final LText DESCRIPTION =
    AppBook.text(X + "DESCRIPTION", "Reorder nested queries and folders alphabetically");

  public SortNodesAction() {
    super(ACTION_NAME.format());
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, DESCRIPTION.format());
  }

  public void perform(ActionContext context) throws CantPerformException {
    List<GenericNode> selectedNavigationNodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    for (GenericNode node : selectedNavigationNodes) {
      node.sortChildren();
    }
  }

  public void update(UpdateContext context) throws CantPerformException {
    super.update(context);
    context.watchRole(GenericNode.NAVIGATION_NODE);
    List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);

    boolean haveChildren = false;
    boolean reorderAllowed = false;
    boolean reorderProhibited = false;
    boolean alreadySorted = false;
    for (GenericNode genericNode : nodes) {
      if (genericNode.getChildrenCount() > 0) {
        haveChildren = true;
      }
      if (genericNode.getChildrenOrderPolicy() == ChildrenOrderPolicy.REORDER_ON_REQUEST) {
        reorderAllowed = true;
      } else {
        reorderProhibited = true;
      }
    }
    if (nodes.size() == 1) {
      GenericNode node = nodes.get(0);
      alreadySorted = node.isSortedChildren();
    }

    if (!haveChildren || !reorderAllowed) {
      context.setEnabled(EnableState.INVISIBLE);
    } else {
      context.setEnabled(!reorderProhibited && !alreadySorted);
    }
  }
}
