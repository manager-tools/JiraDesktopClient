package com.almworks.api.application.tree;

import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBReader;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.IdentifiableNode;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * @author dyoma
 */
public interface GenericNode extends IdentifiableNode, Comparable<GenericNode> {
  Convertor<GenericNode, ATreeNode<GenericNode>> GET_TREE_NODE = new Convertor<GenericNode, ATreeNode<GenericNode>>() {
    public ATreeNode<GenericNode> convert(GenericNode value) {
      return value.getTreeNode();
    }
  };

  Convertor<GenericNode, String> GET_NAME = new Convertor<GenericNode, String>() {
    public String convert(GenericNode value) {
      return value.getName();
    }
  };

  DataRole<GenericNode> NAVIGATION_NODE = DataRole.createRole(GenericNode.class);

  Convertor<GenericNode, GenericNode> GET_PARENT_NODE = new Convertor<GenericNode, GenericNode>() {
    public GenericNode convert(GenericNode value) {
      return value.getParent();
    }
  };

  void addChildNode(GenericNode child);

  boolean allowsAnyChildren();

  boolean allowsChildren(TreeNodeFactory.NodeType childType);

  ReadonlyConfiguration createCopy(Configuration parentConfig);

  /**
   * Starts search from <code>this</code>
   */
  @Nullable
  <T extends GenericNode> T getAncestorOfType(Class<? extends T> ancestorClass);

  @Nullable
  <T extends GenericNode> T getStrictAncestorOfType(Class<? extends T> ancestorClass);

  int getChildrenCount();

  GenericNode getChildAt(int index);

  List<? extends GenericNode> getChildren();

  Configuration getConfiguration();

  @Nullable
  Connection getConnection();

  @Nullable("when there's no valid contstraint or when precise hypercube is requested and is not available")
  ItemHypercube getHypercube(boolean precise);

  @Nullable
  GenericNode getParent();

  /**
   * Returns null in nodes that do not participate in "ordering"
   */
  String getPositionId();

  CanvasRenderable getPresentation();

  @NotNull
  @ThreadSafe
  QueryResult getQueryResult();

  @NotNull
  TreeModelBridge<GenericNode> getTreeNode();

  boolean isCopiable();

  boolean isOrdered();

  boolean isRemovable();

  int removeFromTree();

  @Nullable
  RootNode getRoot();

  boolean isSynchronized();

  @ThreadAWT
  void fireTreeNodeChanged();

  @ThreadAWT
  void fireSubtreeChanged();

  boolean canHideEmptyChildren();

  void setHideEmptyChildren(boolean newValue);

  boolean getHideEmptyChildren();

  /**
   * @return true if the node is connected to a tree (model)
   */
  boolean isNode();

  void sortChildren();

  /**
   * Returns true if the node narrows the parent's node result set.
   */
  boolean isNarrowing();

  String getName();

  int compareChildren(GenericNode node1, GenericNode node2);

  ChildrenOrderPolicy getChildrenOrderPolicy();

  boolean isSortedChildren();

  boolean isCollapsed();

  @ThreadAWT
  void invalidatePreview();

  @ThreadSafe
  void invalidatePreviewSafe();

  /**
   * Requests that the node, if it needs to, recalculates its ItemsPreview.
   * @param lifespan
   */
  @CanBlock
  ItemsPreview getOrCalculatePreview(Lifespan lifespan, DBReader reader);

  @ThreadAWT
  void maybeSchedulePreview();

  @Nullable
  ItemsPreview getPreview(boolean scheduleIfNotAvailable);

  /**
   * @return current number of items in preview, or -1 if preview is not yet available or recounting.
   * @param scheduleIfNotAvailable
   */
  int getPreviewCount(boolean scheduleIfNotAvailable);

  /**
   * Cushioned preview count is needed for navigation tree, so the tree won't jump when
   * a node is first invalidated, then validated. System may be not supporting cushioned count,
   * for example to debug counting.
   *
   * @return number of items in last valid preview, or -1 if there's been valid preview.
   */
  int getCusionedPreviewCount();

  /**
   * Returns true if this node is shown on the screen - i.e. its parent is expanded
   * @return
   */
  boolean isShowable();

  List<GenericNode> getPathFromRoot();

  int compareToSame(GenericNode that);

  Procedure<GenericNode> createNodeInsert();

  Iterator<? extends GenericNode> getChildrenIterator();
}
