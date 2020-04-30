package com.almworks.api.application.qb;

import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.ListDataAdapter;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIPresentable;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;

/**
 * EditorNode is the node in QB tree of constraints. It is UIPresentable to be shown as a form (to edit
 * a constraint); it is CanvasRenderable to be shown as a tree node.
 * Todo: replace inheritance with delegation
 *
 * @author : Dyoma
 */
public abstract class EditorNode implements UIPresentable, CanvasRenderable {
  public static final DataRole<EditorNode> EDITOR_NODE = DataRole.createRole(EditorNode.class);

  @NotNull
  private final EditorContext myContext;

  private final EditorNodeType myType;
  private final TreeModelBridge<EditorNode> myTreeNode;

  protected EditorNode(@NotNull EditorContext context, @Nullable TreeModelBridge<EditorNode> treeNode,
    EditorNodeType type)
  {
    myContext = context;
    myType = type;
    if (treeNode == null) {
      myTreeNode = new TreeModelBridge<EditorNode>(this);
    } else {
      myTreeNode = treeNode;
      myTreeNode.setUserObject(this);
    }
  }

  protected EditorNode(EditorContext context, EditorNodeType type) {
    this(context, null, type);
  }

  public final EditorNodeType getType() {
    return myType;
  }

  /**
   * @return node for QB constraint tree with values taken from edited forms
   */
  @NotNull
  public abstract FilterNode createFilterNodeTree();

  /**
   * Recursively frees all subscriptions for EditorNode subtree
   */
  public abstract void dispose();

  /**
   * Creates editor panel for parameters of this node
   */
  @Nullable("when there's nothing to edit")
  public abstract UIComponentWrapper createEditor(Configuration configuration);

  public abstract boolean isModified();

  /**
   * @return true if the editor node is "No Constraint" node, which means it does not affect the result set in any way.
   *         (could be viewed as TRUE under AND, or FALSE under OR)
   */
  public abstract boolean isNoConstraint();

  @Nullable
  public UIComponentWrapper createUIWrapper(Configuration configuration) {
    return createEditor(configuration);
  }

  @NotNull
  public final EditorContext getContext() {
    return myContext;
  }

  @NotNull
  public TreeModelBridge<EditorNode> getTreeNode() {
    return myTreeNode;
  }

  public Detach watchChanges(ComboBoxModel comboBoxModel) {
    return UIUtil.addListDataListener(comboBoxModel, new ListDataAdapter() {
      public void contentsChanged(ListDataEvent e) {
        if (e.getIndex1() != -1)
          return;
        fireChanged();
      }
    });
  }

  public void watchSelection(final Lifespan lifespan, SelectionAccessor selectionAccessor) {
    lifespan.add(selectionAccessor.addListener(new SelectionAccessor.Listener() {
      public void onSelectionChanged(Object newSelection) {
        if (!lifespan.isEnded()) {
          fireChanged();
        }
      }
    }));
  }

  /**
   * asks to repaint (?)
   */
  protected final void fireChanged() {
    getTreeNode().fireChanged();
  }
}
