package com.almworks.util.ui.actions.dnd;

import com.almworks.util.commons.Factory;
import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.TreeDropPoint;
import com.almworks.util.io.IOUtils;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.ui.TreeUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import com.almworks.util.ui.actions.ComponentContext;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.external.BitSet2;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.almworks.util.ui.actions.dnd.DndAction.MOVE;

/**
 * @author dyoma
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public class TreeStringTransfer implements ContextTransfer {
  private static final TypedKey<List<ATreeNode>> SOURCE_NODES = TypedKey.create("SOURCE_NODES");

  private final TreeStringTransferService myTransferService;

  public TreeStringTransfer(TreeStringTransferService transferService) {
    assert transferService != null;
    myTransferService = transferService;
  }

  // ==================== EXPORTING ==============================================================

  @NotNull
  public Transferable transfer(DragContext context) throws CantPerformException {
    List<ATreeNode> nodes = context.getSourceCollection(ATreeNode.ATREE_NODE);
    assert myTransferService.canExport(nodes);
    context.putValue(SOURCE_NODES, nodes);

    String[] exported = new String[nodes.size()];
    for (int i = 0; i < nodes.size(); i++) {
      ATreeNode node = nodes.get(i);
      exported[i] = myTransferService.exportString(node);
    }
    return new StringListTransferable(exported);
  }

  public boolean canCopy(ActionContext context) throws CantPerformException {
    List<ATreeNode> nodes = context.getSourceCollection(ATreeNode.ATREE_NODE);
    return myTransferService.canExport(nodes);
  }

  public boolean canLink(ActionContext context) throws CantPerformException {
    return false;
  }

  public boolean canRemove(ActionContext context) throws CantPerformException {
    ComponentContext<ATree> cc = context.getComponentContext(ATree.class, ATreeNode.ATREE_NODE);
    List<ATreeNode> selection = cc.getSourceCollection(ATreeNode.ATREE_NODE);
    return selection != null && selection.size() != 0 && canRemoveAll(selection);
  }

  public boolean canMove(ActionContext context) throws CantPerformException {
    return canRemove(context);
  }

  private boolean canRemoveAll(List<? extends ATreeNode> nodes) {
    for (ATreeNode node : nodes) {
      if (!myTransferService.canRemove(node))
        return false;
    }
    return true;
  }

  public void cleanup(DragContext context) throws CantPerformException {
    DndAction action = context.getAction();
    if (action != MOVE)
      return;
    ATree<?> tree = getTree(context);
    if (tree == null)
      return;
    List<? extends ATreeNode> nodes = context.getValue(SOURCE_NODES);
    if (nodes == null && context.isKeyboardTransfer()) {
      nodes = tree.getSelectionAccessor().getSelectedItems();
    }
    if (nodes == null || nodes.size() == 0)
      return;
    assert canRemoveAll(nodes);
    new RemoveMethod(nodes, tree).perform();
  }

  public void remove(ActionContext context) throws CantPerformException {
    assert canRemove(context);
    ComponentContext<ATree> cc = context.getComponentContext(ATree.class, ATreeNode.ATREE_NODE);
    ATree<?> tree = cc.getComponent();
    List<ATreeNode> toRemove = cc.getSourceCollection(ATreeNode.ATREE_NODE);
    new RemoveMethod(toRemove, tree).perform();
  }

  public void startDrag(DragContext context, InputEvent event) throws CantPerformException {
    ATree<?> tree = getTree(context);
    if (tree == null) {
      return;
    }
    JTree jtree = tree.getScrollable();
    if (event instanceof MouseEvent) {
      Point p = ((MouseEvent) event).getPoint();
      Point treePoint = SwingUtilities.convertPoint(event.getComponent(), p, jtree);
      TreePath path = jtree.getClosestPathForLocation(treePoint.x, treePoint.y);
      if (path != null) {
        Rectangle bounds = jtree.getPathBounds(path);
        if (bounds != null && bounds.contains(treePoint)) {
          context.putValue(DndUtil.DRAG_SOURCE_OFFSET, new Point(treePoint.x - bounds.x, bounds.height));
          context.putValue(DndUtil.DRAG_SOURCE_SIZE, new Dimension(bounds.getSize()));
        }
      }
    }
  }

  @Nullable
  public Factory<Image> getTransferImageFactory(DragContext context) throws CantPerformException {
    List<ATreeNode> sourceNodes = context.getValue(SOURCE_NODES);
    if (sourceNodes == null || sourceNodes.size() == 0)
      return null;
    ATree<?> tree = getTree(context);
    if (tree == null)
      return null;
    return new TreeNodesDragImageFactory(tree.getScrollable(), sourceNodes);
  }

  // ==================== IMPORTING ==============================================================

  public boolean canImportData(DragContext context) throws CantPerformException {
    List<String> strings = getStrings(context);
    return canImportStrings(strings);
  }

  public boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException {
    List<String> strings = getStrings(context);
    return canImportStringsNow(strings, context);
  }

  public boolean canImportFlavor(DataFlavor flavor) {
    //noinspection Deprecation
    return DataFlavor.stringFlavor.equals(flavor) || DataFlavor.plainTextFlavor.equals(flavor);
  }

  private boolean canImportStringsNow(List<String> strings, DragContext context) {
    boolean correct = false;
    if (strings != null && strings.size() != 0) {
      TreeDropPoint dropPoint = ATree.getDropPoint(context);
      if (dropPoint != null && dropPoint.isInsertNode()) {
        ATreeNode node = (ATreeNode) dropPoint.getNode();
        boolean nodeAllows;
        if (context.getAction() == MOVE) {
          nodeAllows = canMoveUnderNode(node, context);
          if (nodeAllows) {
            nodeAllows = moveMakesSense(node, dropPoint.getInsertionIndex(), context);
          }
        } else {
          nodeAllows = true;
        }
        if (nodeAllows) {
          for (String string : strings) {
            correct = myTransferService.canImportUnder(node, dropPoint.getInsertionIndex(), string, context);
            if (!correct)
              break;
          }
        }
      }
    }
    return correct;
  }

  private boolean canImportStrings(List<String> strings) {
    boolean correct = false;
    if (strings != null && strings.size() != 0) {
      for (String string : strings) {
        correct = myTransferService.isParseable(string);
        if (!correct)
          break;
      }
    }
    return correct;
  }

  private boolean canMoveUnderNode(ATreeNode<?> node, DragContext context) {
    List<ATreeNode> sourceNodes = context.getValue(SOURCE_NODES);
    if (sourceNodes == null || sourceNodes.size() == 0)
      return true;
    for (ATreeNode sourceNode : sourceNodes) {
      if (sourceNode == node || TreeUtil.isAncestor(sourceNode, node))
        return false;
    }
    return true;
  }

  private boolean moveMakesSense(ATreeNode node, int insertIndex, DragContext context) {
    List<ATreeNode> sourceNodes = context.getValue(SOURCE_NODES);
    if (sourceNodes == null)
      return true;
    int count = sourceNodes.size();
    if (count == 0)
      return true;
    ATreeNode adjacentParent = null;
    int adjacentMin = -1;
    int adjacentMax = -1;
    if (count == 1) {
      ATreeNode sourceNode = sourceNodes.get(0);
      adjacentParent = sourceNode.getParent();
      adjacentMin = adjacentMax = adjacentParent == null ? -1 : adjacentParent.getIndex(sourceNode);
    } else {
      BitSet2 indices = new BitSet2();
      for (ATreeNode sourceNode : sourceNodes) {
        ATreeNode parent = sourceNode.getParent();
        if (parent == null)
          break;
        int index = parent.getIndex(sourceNode);
        if (index < 0)
          break;
        if (adjacentParent == null) {
          adjacentParent = parent;
        } else {
          if (!adjacentParent.equals(parent)) {
            // different parents
            adjacentParent = null;
            break;
          }
        }
        indices.set(index);
      }
      if (adjacentParent != null) {
        // check continuosity
        assert indices.cardinality() == sourceNodes.size();
        int i = indices.nextSetBit(0);
        adjacentMin = i;
        while (i >= 0) {
          adjacentMax = i;
          int j = indices.nextSetBit(i + 1);
          if (j >= 0 && j != i + 1) {
            adjacentParent = null;
            break;
          }
          i = j;
        }
      }
    }
    if (adjacentParent == null)
      return true;
    if (!adjacentParent.equals(node))
      return true;
    // moving under same node: from adjacentMin to adjacentMax is the same place
    return !(insertIndex >= adjacentMin && insertIndex <= adjacentMax + 1);
  }


  public void acceptTransfer(DragContext context, Transferable transfer)
    throws CantPerformException, UnsupportedFlavorException, IOException
  {
    List<String> strings = getStrings(context);
    assert strings != null && canImportStringsNow(strings, context) : strings;
    TreeDropPoint dropPoint = ATree.getDropPoint(context);
    if (dropPoint == null || !dropPoint.isInsertNode())
      throw new CantPerformException();
    ATree<?> tree = getTree(context);
    if (tree == null)
      throw new CantPerformException();
    ATreeNode node = (ATreeNode) dropPoint.getNode();
    int index = dropPoint.getInsertionIndex();
    for (String string : strings) {
      TreeStringTransfer.ImportTarget importTarget = new ImportTarget(tree, node, index);
      try {
        ATreeNode newNode = myTransferService.parseAndCreateNode(string, importTarget.getParent());
        importTarget.insertNode(newNode);
        index++;
      } catch (ParseException e) {
        throw new CantPerformExceptionExplained("Syntax error: " + string, e, null);
      }
    }
  }

  @Nullable
  protected List<String> getStrings(DragContext context) {
    List<String> strings = context.getTransferData(DndUtil.LIST_OF_STRINGS);
    if (strings != null)
      return strings;
    String string = context.getTransferData(DataFlavor.stringFlavor, String.class);
    if (string == null) {
      InputStream in = null;
      try {
        //noinspection Deprecation
        in = context.getTransferData(DataFlavor.plainTextFlavor, InputStream.class);
        if (in != null)
          string = IOUtils.transferToString(in);
      } catch (IOException e) {
        // ignore
      } finally {
        IOUtils.closeStreamIgnoreExceptions(in);
      }
    }
    return string != null ? Collections.singletonList(string) : null;
  }

  // ======================= SERVICE =========================================================

  @Nullable
  private ATree<?> getTree(ActionContext context) {
    Component c = context.getComponent();
    if (c instanceof JTree)
      c = c.getParent();
    return c instanceof ATree ? ((ATree) c) : null;
  }


  private class ImportTarget {
    private final Set<ATreeNode> mySelection = Collections15.hashSet();
    private final ATree<ATreeNode> myTree;
    private ATreeNode myParent;
    private int myInsertIndex;

    public ImportTarget(ATree tree, ATreeNode node, int insertIndex) throws CantPerformException {
      myTree = tree;
      myParent = node;
      myInsertIndex = insertIndex;
    }

    public void insertNode(ATreeNode newTreeNode) {
      if (myInsertIndex < myParent.getChildCount()) {
        ATreeNode node = myParent.getChildAt(myInsertIndex);
        if (myTransferService.shouldReplaceOnPaste(node, newTreeNode)) {
          myTransferService.removeNode(node);
        }
      } else if (myInsertIndex == 0) {
        assert myParent.getChildCount() == 0;
        ATreeNode grandParent = myParent.getParent();
        if (grandParent != null) {
          if (myTransferService.shouldReplaceOnPaste(myParent, newTreeNode)) {
            int index = myTransferService.removeNode(myParent);
            myParent = grandParent;
            myInsertIndex = index;
          }
        }
      }
      insertFlattening(myInsertIndex, newTreeNode);
      myTree.getSelectionAccessor().setSelected(mySelection);
    }

    private int insertFlattening(int index, ATreeNode node) {
      if (!myTransferService.shouldFlattenUnder(myParent, node)) {
        if (node.getParent() != null)
          myTransferService.moveNode(node, myParent, index);
        else
          myParent.insert(node, index);
        mySelection.add(node);
        return index + 1;
      }
      ATreeNode[] children = node.childrenToArray();
      for (ATreeNode child : children) {
        index = insertFlattening(index, child);
      }
      return index;
    }

    public ATreeNode getParent() {
      return myParent;
    }
  }


  private class RemoveMethod {
    private final Set<ATreeNode> mySelection = Collections15.hashSet();
    private final Set<ATreeNode> myExpanded = Collections15.hashSet();
    private final Set<ATreeNode> myToRemove;
    private final ATree myTree;

    public RemoveMethod(List<? extends ATreeNode> toRemove, ATree tree) {
      myToRemove = TreeUtil.excludeDescendants(toRemove);
      myTree = tree;
    }

    public void perform() {
      for (ATreeNode node : myToRemove) {
        if (!node.isAttachedToModel())
          continue;
        ATreeNode parent = node.getParent();
        if (!removeRoot(node)) {
          removeNode(node);
          flattenAncestors(parent);
        }
      }
      restoreSelection();
    }

    private void restoreSelection() {
      SelectionAccessor selection = myTree.getSelectionAccessor();
      for (ATreeNode node : myExpanded) {
        if (!node.isAttachedToModel())
          continue;
        if (node.isLeaf())
          continue;
        myTree.expand(node);
      }
      for (ATreeNode node : mySelection) {
        if (node.getParent() == null) {
          if (node != myTree.getRoot() || !myTree.isRootVisible()) {
            continue;
          }
        }
//        if (!node.isAttachedToModel() || (!myTree.isRootVisible() && node == myTree.getRoot()))
//          continue;
        selection.addSelection(node);
      }
    }

    private void flattenAncestors(ATreeNode node) {
      if (node == null)
        return;
      while (true) {
        ATreeNode parent = node.getParent();
        if (!myTransferService.shouldFlattenUnder(parent, node))
          break;
        ATreeNode[] children = node.childrenToArray();
        saveNodesState(children);
        if (parent == null) {
          assert children.length < 2 : node;
          if (children.length == 0)
            replaceRootWithDefault();
          else {
            assert children.length == 1; // No one node should be lost except old root
            ATreeNode newRoot = children[0];
            newRoot.removeFromParent();
            replaceTreeRoot(newRoot);
          }
          break;
        }
        if (children.length == 0)
          if (removeRoot(node))
            break;
        int index = removeNode(node);
        for (ATreeNode child : children) {
          myTransferService.moveNode(child, parent, index);
          index++;
        }
        node = parent;
      }
    }

    private void saveNodesState(ATreeNode[] children) {
      for (ATreeNode child : children) {
        if (isExpanded(child))
          myExpanded.add(child);
        if (isSelected(child))
          mySelection.add(child);
      }
    }

    private boolean removeRoot(ATreeNode node) {
      ATreeNode parent = node.getParent();
      boolean lastVisibleRoot = myTree.isVisibleRoot(node) && (parent == null || parent.getChildCount() == 1);
      if (lastVisibleRoot) {
        if (myTree.isRootVisible()) {
          replaceRootWithDefault();
        } else {
          assert parent != null : node;
          ATreeNode defaultRoot = myTransferService.createDefaultRoot();
          if (defaultRoot != null) {
            boolean expanded = isExpanded(node);
            int index = removeNode(node);
            parent.insert(defaultRoot, index);
            mySelection.add(defaultRoot);
            if (expanded)
              myExpanded.add(defaultRoot);
          }
        }
        ATreeNode treeRoot = myTree.getRoot();
        if (treeRoot.getChildCount() > 0)
          myTree.expand(treeRoot);
      }
      return lastVisibleRoot;
    }

    private boolean isExpanded(ATreeNode node) {
      return myExpanded.contains(node) || myTree.isExpanded(node);
    }

    private void replaceRootWithDefault() {
      ATreeNode defaultRoot = myTransferService.createDefaultRoot();
      assert defaultRoot != null : "New model root shouldn't be null";
      mySelection.clear();
      myExpanded.clear();
      replaceTreeRoot(defaultRoot);
    }

    private void replaceTreeRoot(ATreeNode newRoot) {
      myTree.setRoot(newRoot);
      mySelection.add(newRoot);
      myExpanded.add(newRoot);
    }

    private int removeNode(ATreeNode node) {
      boolean selected = isSelected(node);
      if (selected) {
        ATreeNode parent = node.getParent();
        assert parent != null : node;
        ATreeNode newSelection = node.getNextSibling();
        if (newSelection == null) {
          newSelection = node.getPrevSibling();
          if (newSelection == null) {
            newSelection = parent;
          }
        }
        mySelection.add(newSelection);
      }
      return myTransferService.removeNode(node);
    }

    private boolean isSelected(ATreeNode node) {
      return mySelection.contains(node) || myTree.getSelectionAccessor().isSelected(node);
    }
  }
}
