package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.EditorNode;
import com.almworks.api.application.qb.EditorNodeType;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.explorer.qbuilder.EditorGroupNode;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.UIComponentWrapper;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

/**
 * @author : Dyoma
 */
class QueryTree extends EditorGroupNode {
  private final EditorNode myOriginalNode;

  private QueryTree(EditorContext context, EditorNode originalChild) {
    super(context, null, EditorNodeType.SPECIAL_NODE);
    myOriginalNode = originalChild;
  }

  @NotNull
  public FilterNode createFilterNodeTree() {
    EditorNode queryNode = getQueryNode();
    return queryNode.isNoConstraint() ?  FilterNode.ALL_ITEMS :  queryNode.createFilterNodeTree();
  }

  public boolean isModified() {
    EditorNode child = getQueryNode();
    return !myOriginalNode.equals(child) || child.isModified();
  }

  private EditorNode getQueryNode() {
    TreeModelBridge<EditorNode> treeNode = getTreeNode();
    int childCount = treeNode.getChildCount();
    if (childCount == 0) {
      // robustness or bug promotion?
      Log.warn("broken query structure");
      return createAnd(getContext(), null);
    }
    assert childCount == 1 : childCount;
    EditorNode node = treeNode.getChildAt(0).getUserObject();
    assert node != null : treeNode.getChildAt(0);
    return node;
  }

  @NotNull
  public Object getGroupId() {
    return ROOT_GROUP;
  }

  public boolean isNoConstraint() {
    return false;
  }

  public void dispose() {
    myOriginalNode.dispose();
    EditorNode node = getQueryNode();
    if (node != myOriginalNode)
      node.dispose();
  }

  public void renderOn(Canvas canvas, CellState state) {
  }

  public UIComponentWrapper createEditor(Configuration configuration) {
    assert false;
    return null;
  }

  public static TreeModelBridge<EditorNode> createRoot(EditorNode node) {
    TreeModelBridge<EditorNode> root = new QueryTree(node.getContext(), node).getTreeNode();
    root.add(node.getTreeNode());
    return root;
  }

  public String toString() {
    return "QueryRoot";
  }
}
