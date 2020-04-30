package com.almworks.util.ui.actions;

import com.almworks.util.components.ATreeNode;

import java.util.List;

public class BasicActions {
  public static final AnActionListener REMOVE_OPERATION = new AnActionListener() {
    public void perform(ActionContext context) throws CantPerformException {
      List<ATreeNode> selection = context.getSourceCollection(ATreeNode.ATREE_NODE);
      for (ATreeNode node : selection)
        node.removeFromParent();
    }
  };
  public static final AnAction REMOVE_ACTION = ActionUtil.createAction(REMOVE_OPERATION, "Remove");
}
