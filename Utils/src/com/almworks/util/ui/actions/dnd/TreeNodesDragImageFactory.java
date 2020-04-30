package com.almworks.util.ui.actions.dnd;

import com.almworks.util.commons.Factory;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.ui.TreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;

public class TreeNodesDragImageFactory implements Factory<Image> {
  private static final int MAX_ROWS = 8;

  private static CellRendererPane ourRendererPane = new CellRendererPane();

  private final JTree myTree;
  private final Set<ATreeNode> myNodes;

  private int myTotalRows;
  private JTree myRenderer;

  public TreeNodesDragImageFactory(JTree tree, Collection<? extends ATreeNode> nodes) {
    myTree = tree;
    myNodes = TreeUtil.excludeDescendants(nodes);
  }

  public Image create() {
    DefaultTreeModel model = getModel();
    if (model == null)
      return null;

    myTotalRows = 0;
    prepareTree();

    if (ourRendererPane.getComponentCount() != 1 || ourRendererPane.getComponent(0) != myRenderer) {
      ourRendererPane.removeAll();
      ourRendererPane.add(myRenderer);
    }
    Container parent = ourRendererPane.getParent();
    if (parent != myTree) {
      if (parent != null) {
        parent.remove(ourRendererPane);
      }
      myTree.add(ourRendererPane);
    }

    Dimension size = myRenderer.getPreferredSize();
    BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);
    Graphics2D g = (Graphics2D) image.getGraphics();
    try {
      ourRendererPane.paintComponent(g, myRenderer, myTree, 0, 0, size.width, size.height, true);
      g.setComposite(AlphaComposite.DstIn);
      int bgt = (0xFFFFFF) | (((int) (255 * DndUtil.DRAG_IMAGE_OPACITY)) << 24);
      Color color1 = new Color(bgt, true);
      bgt = (0xFFFFFF) | (((int) (255 * DndUtil.DRAG_IMAGE_OPACITY_MIN)) << 24);
      Color color2 = new Color(bgt, true);
      g.setColor(color1);
      int rowHeight = myRenderer.getRowHeight();
      if (rowHeight <= 0) {
        // See #567
        rowHeight = 16;
      }
      g.fillRect(0, 0, size.width, rowHeight);
      g.setPaint(new GradientPaint(0, rowHeight, color1, 0, size.height, color2));
      g.fillRect(0, rowHeight, size.width, size.height - rowHeight);
    } finally {
      g.dispose();
      cleanUp();
    }
    return image;
  }

  private void cleanUp() {
    Container parent;
    parent = ourRendererPane.getParent();
    if (parent != null)
      parent.remove(ourRendererPane);
    parent = myRenderer.getParent();
    if (parent != null)
      parent.remove(myRenderer);
    myRenderer = null;
  }

  private void prepareTree() {
    TreeModelBridge<ATreeNode> root = new TreeModelBridge<ATreeNode>(null);
    DefaultTreeModel model = root.becomeRoot();
    myRenderer = new JTree() {
      public boolean isFocusOwner() {
        return true;
      }
    };
    myRenderer.setOpaque(false);
    myRenderer.setRootVisible(false);
    myRenderer.setShowsRootHandles(false);
    myRenderer.setModel(model);

    // create copy of model
    root.removeAll();
    myTotalRows = 0;
    TreePath rootPath = new TreePath(root);

    // sort nodes by their row
    SortedMap<Integer, ATreeNode> nodes = Collections15.treeMap();
    for (ATreeNode node : myNodes) {
      nodes.put(getNodeRow(node), node);
    }

    for (ATreeNode node : nodes.values()) {
      copyRealNode(rootPath, node, model);
    }
    myRenderer.expandPath(rootPath);

    // set renderer
    myRenderer.setCellRenderer(new MyTreeCellRenderer());
  }

  private void copyRealNode(TreePath imagePath, ATreeNode realNode, DefaultTreeModel realModel) {
    if (myTotalRows >= MAX_ROWS)
      return;
    TreeModelBridge<ATreeNode> parent = (TreeModelBridge<ATreeNode>) imagePath.getLastPathComponent();
    TreeModelBridge<ATreeNode> image = new TreeModelBridge<ATreeNode>(realNode);
    parent.add(image);
    myTotalRows++;
    int childCount = realNode.getChildCount();
    if (childCount > 0) {
      if (realModel != null) {
        TreeNode[] realPath = realModel.getPathToRoot(realNode);
        TreePath imageSubPath = imagePath.pathByAddingChild(image);
        boolean expanded = myTree.isExpanded(new TreePath(realPath));
        if (!expanded || myTotalRows >= MAX_ROWS) {
          // add any child, don't expand
          ATreeNode child = realNode.getChildAt(0);
          image.add(new TreeModelBridge<ATreeNode>(child));
          myRenderer.collapsePath(imageSubPath);
        } else {
          for (int i = 0; i < childCount; i++) {
            if (myTotalRows >= MAX_ROWS) {
              break;
            }
            copyRealNode(imageSubPath, realNode.getChildAt(i), realModel);
          }
          myRenderer.expandPath(imageSubPath);
        }
      }
    }
  }

  private int getNodeRow(ATreeNode node) {
    DefaultTreeModel model = getModel();
    if (model == null) {
      assert false : this;
      return -1;
    }
    TreeNode[] path = model.getPathToRoot(node);
    if (path == null) {
      assert false : this;
      return -1;
    }
    TreePath treePath = new TreePath(path);
    int row = myTree.getRowForPath(treePath);
    if (row < 0) {
      assert false : this + " " + treePath;
      return -1;
    }
    return row;
  }

  private DefaultTreeModel getModel() {
    return Util.castNullable(DefaultTreeModel.class, myTree.getModel());
  }

  private class MyTreeCellRenderer implements TreeCellRenderer {
    private final JLabel myStub = new JLabel();

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
      boolean leaf, int row, boolean hasFocus)
    {
      TreeModelBridge<ATreeNode> node = (TreeModelBridge<ATreeNode>) value;
      if (node == null) {
        assert false : this;
        return null;
      }
      ATreeNode realNode = node.getUserObject();
      if (realNode == null) {
        // root -- how?
        return myStub;
      }
      TreeCellRenderer reallRenderer = myTree.getCellRenderer();
      int realRow = getNodeRow(realNode);
      if (realRow < 0)
        realRow = row;
      return reallRenderer.getTreeCellRendererComponent(myTree, realNode, true, expanded, leaf, realRow, false);
    }
  }
}
