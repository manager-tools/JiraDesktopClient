package com.almworks.util.components.tables;

import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.ObjectWrapper;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.OverridenCellState;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * @author dyoma
 */
public class JTreeRenderer extends JTree {
  private final JComponent myHostComponent;
  private TreePath myCurrentCell;
  private ObjectWrapper<?> myCurrentNode;
  private CellState myCurrentState;
  private final TreeCellRenderer myRenderer;
  @Nullable
  private CollectionRenderer<Object> myCurrentRenderer;
  private boolean myExpanded;
  private final JLabel myMockRenderer = new JLabel("W");
  private boolean myMinimumWidth;
  private boolean myResetSizeCache = false;

  public JTreeRenderer(JComponent hostComponent) {
    super(new DefaultTreeModel(new DefaultMutableTreeNode(null)));
    setOpaque(false);
    myHostComponent = hostComponent;
    myRenderer = new TreeCellRenderer() {
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
        boolean leaf, int row, boolean hasFocus)
      {
        CollectionRenderer<Object> currentRenderer = myCurrentRenderer;
        if (currentRenderer == null) {
          assert false;
          return myMockRenderer;
        }
        return getRendererComponent(currentRenderer);
      }
    };
  }

  public DefaultTreeModel getModel() {
    return (DefaultTreeModel) super.getModel();
  }

  public void paintComponent(Graphics g) {
    if (myCurrentState == null)
      return;
    AwtUtil.applyRenderingHints(g);
    Rectangle cellBounds = getCurrentCellBounds();
    g.translate(0, -cellBounds.y);
    super.paintComponent(g);
  }

  public Rectangle getCurrentCellBounds() {
    return getPathBounds(myCurrentCell);
  }

  public void setBounds(int x, int y, int width, int height) {
    super.setBounds(x, 0, width, myHostComponent.getHeight());
  }

  public void preparePath(TreeNodeState nodeState, CellState state, CollectionRenderer<?> renderer,
    boolean minimumWidth)
  {
    myMinimumWidth = minimumWidth;
    myCurrentNode = nodeState.getNode();
    myCurrentCell = null;
    myCurrentRenderer = (CollectionRenderer<Object>) renderer;
    myCurrentState = state;
    myExpanded = nodeState.isExpanded();
    myCurrentCell = nodeState.setupPath(getModel(), this);
    setCellRenderer(myRenderer);
    if (myMinimumWidth || myResetSizeCache) {
      // kludge: make ui forget cached size
      firePropertyChange(CELL_RENDERER_PROPERTY, null, myRenderer);
    }
  }

  public void setResetSizeCache(boolean reset) {
    myResetSizeCache = reset;
  }

  private Component getRendererComponent(CollectionRenderer<Object> currentRenderer) {
    assert myCurrentNode != null;
    CellState state = new OverridenCellState(myCurrentState).setNullBorder(UIUtil.BORDER_1).setExpanded(myExpanded);
    JComponent rendererComponent = currentRenderer.getRendererComponent(state, myCurrentNode.getUserObject());
    Border border = rendererComponent.getBorder();
    if (border != null && !(border.getClass() != EmptyBorder.class))
      rendererComponent.setBorder(new EmptyBorder(border.getBorderInsets(rendererComponent)));
    if (myMinimumWidth) {
      rendererComponent.setPreferredSize(null);
      // kludge: make ui forget cached size
      firePropertyChange(CELL_RENDERER_PROPERTY, null, myRenderer);
    } else {
      int height = myCurrentState.getComponentCellHeight();
      Dimension prefSize = null;
      if (height <= 0) {
        prefSize = rendererComponent.getPreferredSize();
        height = prefSize.height;
      }
      int width = myCurrentState.getComponentCellWidth();
      if (width <= 0) {
        if (prefSize == null)
          prefSize = rendererComponent.getPreferredSize();
        width = prefSize.width;
      }
      if (prefSize == null)
        prefSize = new Dimension();
      prefSize.height = height;
      prefSize.width = width;
      rendererComponent.setPreferredSize(prefSize);
    }
    return rendererComponent;
  }

  public void validate() {
  }


  public void revalidate() {
  }

  public void repaint(long tm, int x, int y, int width, int height) {
  }

  public void repaint(Rectangle r) {
  }
}
