package com.almworks.explorer.qbuilder;

import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.EditorNode;
import com.almworks.api.application.qb.EditorNodeType;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.explorer.qbuilder.filter.BinaryCommutative;
import com.almworks.explorer.qbuilder.filter.CompositeFilterNode;
import com.almworks.explorer.qbuilder.filter.GroupingEditorNode;
import com.almworks.explorer.qbuilder.filter.NeitherFilterNode;
import com.almworks.util.L;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.ModelAware;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.images.Icons;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author dyoma
 */
public abstract class EditorGroupNode extends EditorNode {
  // todo replace with EditorNodeType?
  public static final Object AND_GROUP = TypedKey.create("AND_GROUP");
  public static final Object OR_GROUP = TypedKey.create("OR_GROUP");
  public static final Object NEITHER_GROUP = TypedKey.create("NEITHER_GROUP");
  public static final Object ROOT_GROUP = TypedKey.create("ROOT_GROUP");

  protected EditorGroupNode(EditorContext context, TreeModelBridge<EditorNode> treeNode, EditorNodeType type) {
    super(context, treeNode, type);
  }

  @NotNull
  public abstract Object getGroupId();

  @Nullable
  public static Object getGroupId(@Nullable EditorNode node) {
    return node instanceof EditorGroupNode ? ((EditorGroupNode) node).getGroupId() : null;
  }

  @Nullable
  public static Object getGroupId(ATreeNode<? extends EditorNode> treeNode) {
    EditorNode userObject = treeNode.getUserObject();
    return userObject != null ? getGroupId((EditorNode) userObject) : null;
  }

  @NotNull
  public static GroupingEditorNode createOr(@NotNull EditorContext context,
    @Nullable TreeModelBridge<EditorNode> treeNode)
  {
    return createCommutative(context, treeNode, OR_GROUP, Icons.QUERY_OR_NODE, "OR",
      L.tooltip("At least one condition is met"), BinaryCommutative.OR_SERIALIZER, EditorNodeType.OR_NODE);
  }

  @NotNull
  public static GroupingEditorNode createAnd(@NotNull EditorContext context,
    @Nullable TreeModelBridge<EditorNode> treeNode)
  {
    return createCommutative(context, treeNode, AND_GROUP, Icons.QUERY_AND_NODE, "AND",
      L.tooltip("All conditions are met"), BinaryCommutative.AND_SERIALIZER, EditorNodeType.AND_NODE);
  }

  @NotNull
  public static GroupingEditorNode createNegation(@NotNull EditorContext context,
    @Nullable TreeModelBridge<EditorNode> treeNode)
  {
    return new NeitherEditorNode(context, treeNode);
  }

  private static GroupingEditorNode createCommutative(@NotNull EditorContext context,
    @Nullable TreeModelBridge<EditorNode> treeNode, final Object groupId, final Icon nodeIcon, final String treeName,
    final String tooltip, final CompositeFilterNode.InfixFormulaSerializer factory, EditorNodeType type)
  {
    return new GroupingEditorNode(context, groupId, treeNode, type) {
      public void renderOn(Canvas canvas, CellState state) {
        canvas.setIcon(nodeIcon);
        canvas.appendText(treeName);
        canvas.setToolTipText(tooltip);
      }

      public FilterNode createFilterNodeTree() {
        return createFilterNode();
      }

      private FilterNode createFilterNode() {
        List<EditorNode> constraints = getChildConstraints();
        int count = constraints.size();
        if (count == 1) {
          EditorNode singleChild = constraints.get(0);
          return singleChild.createFilterNodeTree();
        } else {
          assert count > 1 : count;
          return factory.create(collectFilters(constraints));
        }
      }
    };
  }

  private static class NeitherEditorNode extends GroupingEditorNode implements ModelAware {
    public NeitherEditorNode(EditorContext context, TreeModelBridge<EditorNode> treeNode) {
      super(context, EditorGroupNode.NEITHER_GROUP, treeNode, EditorNodeType.NEITHER_NODE);
    }

    public void renderOn(Canvas canvas, CellState state) {
      int childCount = getTreeNode().getChildCount();
      canvas.setIcon(Icons.QUERY_NOT_NODE);
      if (childCount < 2) {
        canvas.appendText(L.treeNode("NOT"));
        canvas.setToolTipText(L.tooltip("Condition is false"));
      } else {
        canvas.appendText(L.treeNode("NEITHER"));
        canvas.setToolTipText(L.tooltip("At least one condition is false"));
      }
    }

    @NotNull
    public FilterNode createFilterNodeTree() {
      return new NeitherFilterNode(collectFilters(getChildConstraints()));
    }

    public void onInsertToModel() {

    }

    public void onRemoveFromModel() {

    }

    public void onChildrenChanged() {
      if (getTreeNode().getChildCount() <= 2)
        getTreeNode().fireChanged();
    }
  }
}
