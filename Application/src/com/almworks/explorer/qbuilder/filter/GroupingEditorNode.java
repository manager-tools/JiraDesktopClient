package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.EditorNode;
import com.almworks.api.application.qb.EditorNodeType;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.explorer.qbuilder.EditorGroupNode;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.UIComponentWrapper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * @author dyoma
 */
public abstract class GroupingEditorNode extends EditorGroupNode {
  private final Set<EditorNode> myOriginalChildren = Collections15.hashSet();
  private final Object myGroupId;

  public GroupingEditorNode(EditorContext context, Object groupId, TreeModelBridge<EditorNode> treeNode,
    EditorNodeType type) {
    super(context, treeNode, type);
    myGroupId = groupId;
  }

  protected List<FilterNode> collectFilters(List<EditorNode> constraints) {
    List<FilterNode> arguments = Collections15.arrayList();
    for (EditorNode editor : constraints) {
      FilterNode filterNode;
      filterNode = editor.createFilterNodeTree();
      assert filterNode != null;
      arguments.add(filterNode);
    }
    return arguments;
  }

  public boolean isModified() {
     Set<EditorNode> original = Collections15.hashSet(myOriginalChildren);
     List<EditorNode> constraints = getChildConstraints();
     if (constraints.isEmpty())
       return false;
     for (EditorNode editor : constraints) {
       if (!original.contains(editor) || editor.isModified())
         return true;
       original.remove(editor);
     }
     return !original.isEmpty();
   }

  protected List<EditorNode> getChildConstraints() {
    List<EditorNode> result = Collections15.arrayList();
    TreeModelBridge<EditorNode> treeNode = getTreeNode();
    for (int i = 0; i < treeNode.getChildCount(); i++) {
      TreeModelBridge<? extends EditorNode> childNode = treeNode.getChildAt(i);
      EditorNode editor = childNode.getUserObject();
      assert editor != null;
      if (!editor.isNoConstraint())
        result.add(editor);
    }
    return result;
  }

  @Nullable
  public UIComponentWrapper createEditor(Configuration configuration) {
    return null;
  }

  @NotNull
  public Object getGroupId() {
    return myGroupId;
  }

  public boolean isNoConstraint() {
    return getChildConstraints().isEmpty();
  }

  public void dispose() {
    List<EditorNode> children = getTreeNode().childObjectsToList();
    for (EditorNode editorNode : children) {
      editorNode.dispose();
      myOriginalChildren.remove(editorNode);
    }
    for (EditorNode node : myOriginalChildren)
      node.dispose();
  }

  public void addChildConstraints(List<FilterNode> children) {
    for (FilterNode argument : children) {
      EditorNode editor = argument.createEditorNode(getContext());
      getTreeNode().insert(editor.getTreeNode(), getTreeNode().getChildCount());
      myOriginalChildren.add(editor);
    }
  }

  public String toString() {
    return "Group: " + myGroupId;
  }
}
