package com.almworks.util.ui.actions.dnd;

import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TreeInsertDropHint {
  public static final TreeInsertDropHint INSTANCE = new TreeInsertDropHint();

  private static JTree ourRenderer = new JTree(new DefaultTreeModel(null));
  private static CellRendererPane ourRendererPane = new CellRendererPane();

  private final TreeModelBridge myHintItem = new TreeModelBridge("hint");
  private final MyHintRenderer myHintRenderer = new MyHintRenderer();

  private Image myImage;
  private Object myLastParentNode;
  private int myLastInsertIndex;
  private Dimension myLastDragSourceSize;
  private int myHintWidth;

  public TreeInsertDropHint() {
  }

  public int getHeight(JTree tree) {
    return tree.getRowHeight();
  }

  public boolean paintSetup(JTree tree, Object parentNode, int insertIndex, int hintRow, Dimension dragSourceSize) {
    Threads.assertAWTThread();
    if (myLastInsertIndex != insertIndex || myLastParentNode != parentNode ||
      !Util.equals(dragSourceSize, myLastDragSourceSize) || myImage == null)
    {
      myLastInsertIndex = insertIndex;
      myLastParentNode = parentNode;
      myLastDragSourceSize = dragSourceSize;
      myImage = createImage(tree, (ATreeNode) parentNode, insertIndex, hintRow, dragSourceSize);
      return myImage != null;
    } else {
      return true;
    }
  }

  private Image createImage(JTree tree, ATreeNode parentNode, int insertIndex, int hintRow, Dimension dragSourceSize) {
    DefaultTreeModel mockModel = (DefaultTreeModel) ourRenderer.getModel();
    TreeModel model = tree.getModel();

    TreeModelBridge mockParent = new TreeModelBridge(parentNode.getUserObject());
    TreeModelBridge mockRoot = mockParent;

    TreePath path = tree.getPathForRow(hintRow - 1);
    while (path != null && path.getLastPathComponent() != parentNode) {
      path = path.getParentPath();
    }
    if (path == null)
      return null;
    boolean parentExpanded = tree.isExpanded(path);
    int parents = 1;
    ATreeNode lastAncestor = parentNode;
    for (path = path.getParentPath(); path != null && path.getPathCount() > 0; path = path.getParentPath()) {
      ATreeNode ancestor = (ATreeNode) path.getLastPathComponent();
      TreeModelBridge newRoot = new TreeModelBridge(ancestor.getUserObject());
      newRoot.add(mockRoot);
      if (model.getIndexOfChild(ancestor, lastAncestor) < model.getChildCount(ancestor) - 1) {
        // not last - add this to get vertical line
        newRoot.add(new TreeModelBridge(null));
      }
      lastAncestor = ancestor;
      mockRoot = newRoot;
      parents++;
    }

    if (dragSourceSize == null) {
      Rectangle boundsAbove = tree.getRowBounds(hintRow - 1);
      if (boundsAbove == null)
        return null;
      myHintWidth = boundsAbove.width;
    } else {
      myHintWidth = dragSourceSize.width;
    }

    mockModel.setRoot(mockRoot);
    try {
      if (myHintItem.getParent() != null)
        myHintItem.removeFromParent();
      mockParent.add(myHintItem);
      if (insertIndex < model.getChildCount(parentNode) && parentExpanded) {
        mockParent.add(new TreeModelBridge(null));
      }

      boolean rootVisible = tree.isRootVisible();
      ourRenderer.setRootVisible(rootVisible);
      ourRenderer.setShowsRootHandles(tree.getShowsRootHandles());
      ourRenderer.setBorder(new EmptyBorder(tree.getInsets()));
      ourRenderer.setForeground(tree.getForeground());
      ourRenderer.setBackground(tree.getBackground());
      ourRenderer.setFont(tree.getFont());
      ourRenderer.setCellRenderer(myHintRenderer);

      if (!rootVisible) {
        parents--;
      }

      tree.add(ourRendererPane);
      try {
        if (ourRenderer.getParent() == null) {
          ourRendererPane.add(ourRenderer);
        }

        for (int i = 0; i <= parents; i++)
          ourRenderer.expandRow(i);
        Rectangle bounds = ourRenderer.getRowBounds(parents);
        if (bounds == null)
          return null;

        int width = bounds.x + bounds.width;
        BufferedImage image = new BufferedImage(width, bounds.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D) image.getGraphics();
        try {
          g.translate(0, -bounds.y);
          ourRendererPane.paintComponent(g, ourRenderer, tree, 0, 0, width, bounds.y + bounds.height, false);
        } finally {
          g.dispose();
        }

//        int keep = ourRenderer.getBackground().getRGB();
//        Image altered = ImageUtil.createSingleHueImage(image, GlobalColors.DRAG_AND_DROP_COLOR, keep);
//        return altered;
        return image;
      } finally {
        tree.remove(ourRendererPane);
      }
    } finally {
      mockModel.setRoot(null);
    }
  }

  public void paint(Graphics2D g, JComponent c, Rectangle r) {
    assert myImage != null;
    g.drawImage(myImage, r.x, r.y, c);
  }

  private class MyHintRenderer extends BaseRendererComponent implements TreeCellRenderer {
    private final BaseRendererComponent myEmpty = new BaseRendererComponent();
    private final int myHeight = 4;

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
      boolean leaf, int row, boolean hasFocus)
    {
      if (value != myHintItem) {
        return myEmpty;
      } else {
        return this;
      }
    }

    public void paint(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(GlobalColors.DRAG_AND_DROP_COLOR);
      Dimension size = getSize();
      int height = Math.min(myHeight, size.height);
      int width = size.width;
      int x = 0;
      int y = (size.height - height) / 2;
      g2.fillRoundRect(x, y, width, height, height, height);
    }

    private int getHintWidth() {
      return Math.max(myHintWidth, 20);
    }

    public Dimension getPreferredSize() {
      return new Dimension(getHintWidth(), myHeight);
    }
  }
}
